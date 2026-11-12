package ni.edu.uam.assistia.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ni.edu.uam.assistia.model.AsignacionCuidador;
import ni.edu.uam.assistia.model.Cuidador;
import ni.edu.uam.assistia.model.Paciente;
import ni.edu.uam.assistia.persistence.CuidadorDao;
import ni.edu.uam.assistia.persistence.PacienteDao;

public final class PacienteController {
    private final PacienteDao pacientes;
    private final CuidadorDao cuidadores;
    private final Executor executor;

    @FXML private VBox contenido;
    @FXML private TextField nombre;
    @FXML private DatePicker nacimiento;
    @FXML private TextField telefono;
    @FXML private TableView<Paciente> tabla;
    @FXML private TableColumn<Paciente, String> columnaNombre;
    @FXML private TableColumn<Paciente, String> columnaNacimiento;
    @FXML private TableColumn<Paciente, String> columnaTelefono;
    @FXML private ComboBox<Cuidador> cuidador;
    @FXML private TextField parentesco;
    @FXML private CheckBox principal;
    @FXML private TableView<AsignacionCuidador> asignaciones;
    @FXML private TableColumn<AsignacionCuidador, String> columnaCuidador;
    @FXML private TableColumn<AsignacionCuidador, String> columnaParentesco;
    @FXML private TableColumn<AsignacionCuidador, String> columnaPrincipal;
    @FXML private Label estado;

    public PacienteController(PacienteDao pacientes, CuidadorDao cuidadores, Executor executor) {
        this.pacientes = pacientes;
        this.cuidadores = cuidadores;
        this.executor = executor;
    }

    @FXML
    private void initialize() {
        columnaNombre.setCellValueFactory(fila -> new ReadOnlyStringWrapper(fila.getValue().getNombre()));
        columnaNacimiento.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().getFechaNacimiento().toString()));
        columnaTelefono.setCellValueFactory(fila -> new ReadOnlyStringWrapper(fila.getValue().getTelefono()));
        columnaCuidador.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().getCuidador().getNombre()));
        columnaParentesco.setCellValueFactory(fila -> new ReadOnlyStringWrapper(fila.getValue().getParentesco()));
        columnaPrincipal.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().isResponsablePrincipal() ? "Sí" : "No"));
        cuidador.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cuidador valor) {
                return valor == null ? "" : valor.getNombre() + " (ID " + valor.getId() + ")";
            }

            @Override
            public Cuidador fromString(String texto) {
                throw new UnsupportedOperationException();
            }
        });
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            asignaciones.getItems().clear();
            cuidador.getSelectionModel().clearSelection();
            parentesco.clear();
            principal.setSelected(false);
            if (seleccionado != null) {
                nombre.setText(seleccionado.getNombre());
                nacimiento.setValue(seleccionado.getFechaNacimiento());
                telefono.setText(seleccionado.getTelefono() == null ? "" : seleccionado.getTelefono());
                cargarAsignaciones(seleccionado.getId(), "Paciente seleccionado.");
            }
        });
        recargar();
    }

    @FXML private void guardar() {
        guardarPaciente(null);
    }

    @FXML private void actualizar() {
        Paciente paciente = seleccionarPaciente();
        if (paciente != null) {
            guardarPaciente(paciente.getId());
        }
    }

    private void guardarPaciente(Long id) {
        try {
            nacimiento.commitValue();
        } catch (DateTimeParseException error) {
            estado.setText("Escribe una fecha válida o selecciónala en el calendario.");
            return;
        }
        String nombreActual = nombre.getText();
        LocalDate fecha = nacimiento.getValue();
        String telefonoActual = telefono.getText();
        ejecutar(() -> {
            pacientes.guardar(id, nombreActual, fecha, telefonoActual);
            return null;
        }, resultado -> cargarPacientes(id == null ? "Paciente guardado." : "Paciente actualizado."));
    }

    @FXML private void eliminar() {
        Paciente paciente = seleccionarPaciente();
        if (paciente != null && confirmar("¿Eliminar a " + paciente.getNombre() + "?")) {
            ejecutar(() -> {
                pacientes.eliminar(paciente.getId());
                return null;
            }, resultado -> cargarPacientes("Paciente eliminado."));
        }
    }

    @FXML private void limpiar() {
        tabla.getSelectionModel().clearSelection();
        nombre.clear();
        nacimiento.setValue(null);
        telefono.clear();
    }

    @FXML private void recargar() {
        cargarPacientes("Lista de pacientes actualizada.");
    }

    private void cargarPacientes(String mensaje) {
        ejecutar(pacientes::listar, filas -> {
            limpiar();
            tabla.getItems().setAll(filas);
            estado.setText(mensaje);
        });
    }

    @FXML private void asignar() {
        Paciente paciente = seleccionarPaciente();
        Cuidador seleccionado = cuidador.getValue();
        if (paciente == null) {
            return;
        }
        if (seleccionado == null) {
            estado.setText("Selecciona un cuidador.");
            return;
        }
        long id = paciente.getId();
        String parentescoActual = parentesco.getText();
        boolean esPrincipal = principal.isSelected();
        ejecutar(() -> {
            pacientes.asignar(id, seleccionado.getId(), parentescoActual, esPrincipal);
            return null;
        }, resultado -> cargarAsignaciones(id, "Cuidador asignado."));
    }

    @FXML private void quitar() {
        Paciente paciente = seleccionarPaciente();
        AsignacionCuidador asignacion = asignaciones.getSelectionModel().getSelectedItem();
        if (paciente == null) {
            return;
        }
        if (asignacion == null) {
            estado.setText("Selecciona una asignación de la tabla.");
            return;
        }
        if (confirmar("¿Quitar la asignación de " + asignacion.getCuidador().getNombre() + "?")) {
            long id = paciente.getId();
            ejecutar(() -> {
                pacientes.quitarAsignacion(id, asignacion.getId());
                return null;
            }, resultado -> cargarAsignaciones(id, "Asignación eliminada."));
        }
    }

    @FXML private void recargarAsignaciones() {
        Paciente paciente = seleccionarPaciente();
        if (paciente != null) {
            cargarAsignaciones(paciente.getId(), "Lista de asignaciones actualizada.");
        }
    }

    private void cargarAsignaciones(long id, String mensaje) {
        ejecutar(() -> new DatosAsignaciones(pacientes.listarAsignaciones(id), cuidadores.listar()), datos -> {
            asignaciones.getItems().setAll(datos.asignaciones());
            cuidador.getItems().setAll(datos.cuidadores());
            cuidador.getSelectionModel().clearSelection();
            parentesco.clear();
            principal.setSelected(false);
            estado.setText(mensaje);
        });
    }

    private Paciente seleccionarPaciente() {
        Paciente paciente = tabla.getSelectionModel().getSelectedItem();
        if (paciente == null) {
            estado.setText("Selecciona un paciente de la tabla.");
        }
        return paciente;
    }

    private boolean confirmar(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        alerta.initOwner(contenido.getScene().getWindow());
        return alerta.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private <T> void ejecutar(Supplier<T> consulta, Consumer<T> alTerminar) {
        contenido.setDisable(true);
        estado.setText("Consultando PostgreSQL…");
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() {
                return consulta.get();
            }
        };
        tarea.setOnSucceeded(evento -> {
            contenido.setDisable(false);
            alTerminar.accept(tarea.getValue());
        });
        tarea.setOnFailed(evento -> {
            contenido.setDisable(false);
            Throwable error = tarea.getException();
            String mensaje = error.getMessage();
            if (error instanceof IllegalArgumentException
                    || error instanceof IllegalStateException && mensaje != null
                    && (mensaje.startsWith("Falta configurar ") || mensaje.startsWith("No se pudo leer db.properties"))) {
                estado.setText(mensaje);
            } else {
                estado.setText("No se pudo completar la operación. Revisa la conexión a PostgreSQL.");
            }
        });
        executor.execute(tarea);
    }

    private record DatosAsignaciones(List<AsignacionCuidador> asignaciones, List<Cuidador> cuidadores) {}
}
