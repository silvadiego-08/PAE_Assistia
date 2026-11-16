package ni.edu.uam.assistia.controller;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ni.edu.uam.assistia.model.Cuidador;
import ni.edu.uam.assistia.persistence.CuidadorDao;

public final class CuidadorController {
    private static final Pattern CORREO = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private final CuidadorDao dao;
    private final Executor executor;

    @FXML private VBox contenido;
    @FXML private TextField nombre;
    @FXML private TextField telefono;
    @FXML private TextField correo;
    @FXML private TableView<Cuidador> tabla;
    @FXML private TableColumn<Cuidador, String> columnaNombre;
    @FXML private TableColumn<Cuidador, String> columnaTelefono;
    @FXML private TableColumn<Cuidador, String> columnaCorreo;
    @FXML private Label estado;

    public CuidadorController(CuidadorDao dao, Executor executor) {
        this.dao = dao;
        this.executor = executor;
    }

    @FXML
    private void initialize() {
        columnaNombre.setCellValueFactory(fila -> new ReadOnlyStringWrapper(fila.getValue().getNombre()));
        columnaTelefono.setCellValueFactory(fila -> new ReadOnlyStringWrapper(fila.getValue().getTelefono()));
        columnaCorreo.setCellValueFactory(fila -> new ReadOnlyStringWrapper(fila.getValue().getCorreo()));
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null) {
                nombre.setText(seleccionado.getNombre());
                telefono.setText(seleccionado.getTelefono());
                correo.setText(seleccionado.getCorreo());
            }
        });
        recargar();
    }

    @FXML
    private void guardar() {
        Datos datos = leerFormulario();
        if (datos != null) {
            cambiar(() -> dao.crear(datos.nombre(), datos.telefono(), datos.correo()), "Cuidador guardado.");
        }
    }

    @FXML
    private void actualizar() {
        Cuidador seleccionado = seleccion();
        if (seleccionado == null) {
            return;
        }
        Datos datos = leerFormulario();
        if (datos != null) {
            long id = seleccionado.getId();
            cambiar(() -> dao.actualizar(id, datos.nombre(), datos.telefono(), datos.correo()),
                    "Cuidador actualizado.");
        }
    }

    @FXML
    private void eliminar() {
        Cuidador seleccionado = seleccion();
        if (seleccionado == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar a " + seleccionado.getNombre() + "?", ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setHeaderText("Eliminar cuidador");
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            long id = seleccionado.getId();
            cambiar(() -> dao.eliminar(id), "Cuidador eliminado.");
        }
    }

    @FXML
    private void limpiar() {
        tabla.getSelectionModel().clearSelection();
        nombre.clear();
        telefono.clear();
        correo.clear();
    }

    @FXML
    private void recargar() {
        cargarTabla(null);
    }

    private void cargarTabla(String mensaje) {
        ejecutar(dao::listar, filas -> {
            tabla.getItems().setAll(filas);
            if (mensaje == null) {
                estado.setText("Cuidadores: " + filas.size());
            } else {
                limpiar();
                estado.setText(mensaje);
            }
        }, mensaje == null ? "" : mensaje + " No se pudo recargar la tabla. ");
    }

    private void cambiar(Runnable cambio, String mensaje) {
        ejecutar(() -> {
            cambio.run();
            return null;
        }, resultado -> cargarTabla(mensaje), "");
    }

    private <T> void ejecutar(Supplier<T> operacion, Consumer<T> alTerminar, String prefijoError) {
        contenido.setDisable(true);
        estado.setText("Consultando PostgreSQL…");
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() {
                return operacion.get();
            }
        };
        tarea.setOnSucceeded(evento -> {
            contenido.setDisable(false);
            alTerminar.accept(tarea.getValue());
        });
        tarea.setOnFailed(evento -> {
            contenido.setDisable(false);
            Throwable error = tarea.getException();
            String mensaje = error instanceof IllegalArgumentException
                    || error instanceof IllegalStateException && error.getMessage() != null
                    && (error.getMessage().startsWith("Falta configurar ")
                    || error.getMessage().startsWith("No se pudo leer db.properties"))
                    ? error.getMessage()
                    : "No se pudo completar la operación. Revisa la conexión a PostgreSQL.";
            estado.setText(prefijoError + mensaje);
        });
        executor.execute(tarea);
    }

    private Cuidador seleccion() {
        Cuidador seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            estado.setText("Selecciona un cuidador de la tabla.");
        }
        return seleccionado;
    }

    private Datos leerFormulario() {
        String nombreLimpio = valor(nombre);
        String telefonoLimpio = valor(telefono);
        String correoLimpio = valor(correo);
        if (nombreLimpio.isEmpty() || nombreLimpio.length() > 120) {
            estado.setText("Escribe un nombre de entre 1 y 120 caracteres.");
            return null;
        }
        if (telefonoLimpio.length() > 25) {
            estado.setText("El teléfono debe tener como máximo 25 caracteres.");
            return null;
        }
        if (!correoLimpio.isEmpty()
                && (correoLimpio.length() > 150 || !CORREO.matcher(correoLimpio).matches())) {
            estado.setText("Escribe un correo válido de hasta 150 caracteres.");
            return null;
        }
        return new Datos(nombreLimpio, telefonoLimpio.isEmpty() ? null : telefonoLimpio,
                correoLimpio.isEmpty() ? null : correoLimpio);
    }

    private String valor(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().strip();
    }

    private record Datos(String nombre, String telefono, String correo) {}
}
