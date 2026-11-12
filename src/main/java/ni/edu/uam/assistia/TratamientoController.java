
package ni.edu.uam.assistia;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import ni.edu.uam.assistia.models.Tratamiento;
import ni.edu.uam.assistia.models.DetalleTratamiento;
import ni.edu.uam.assistia.service.TratamientoService;

import java.math.BigDecimal;
import java.util.List;

public class TratamientoController {

    @FXML private TextField txtPacienteId;
    @FXML private TextField txtMedicoId;
    @FXML private TextField txtMedicamentoId;
    @FXML private TextField txtDosis;
    @FXML private TextField txtInstrucciones;
    @FXML private TextField txtBuscarPaciente;

    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;

    @FXML private ComboBox<String> cbUnidad;

    @FXML private TableView<DetalleTratamiento> tablaDetalles;
    @FXML private TableColumn<DetalleTratamiento, Long> colMedicamento;
    @FXML private TableColumn<DetalleTratamiento, BigDecimal> colDosis;
    @FXML private TableColumn<DetalleTratamiento, String> colUnidad;
    @FXML private TableColumn<DetalleTratamiento, String> colInstrucciones;

    @FXML private ListView<Tratamiento> listaTratamientos;
    @FXML private Label lblEstado;

    @FXML private Button btnGuardar;
    @FXML private Button btnActualizar;
    @FXML private Button btnDesactivar;
    @FXML private Button btnAgregar;

    private final TratamientoService service =
            new TratamientoService();

    private final ObservableList<DetalleTratamiento> detallesNuevos =
            FXCollections.observableArrayList();

    private Long tratamientoSeleccionadoId = null;

    @FXML
    public void initialize() {

        cbUnidad.setItems(FXCollections.observableArrayList(
                "mg", "g", "ml", "mcg",
                "tableta", "cápsula", "gotas", "unidad"
        ));

        colMedicamento.setCellValueFactory(
                new PropertyValueFactory<>("medicamentoId")
        );

        colDosis.setCellValueFactory(
                new PropertyValueFactory<>("dosisIndicada")
        );

        colUnidad.setCellValueFactory(
                new PropertyValueFactory<>("unidad")
        );

        colInstrucciones.setCellValueFactory(
                new PropertyValueFactory<>("instrucciones")
        );

        tablaDetalles.setItems(
                FXCollections.observableArrayList()
        );

        listaTratamientos.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, nuevo) -> {
                    if (nuevo != null) {
                        seleccionarTratamiento(nuevo);
                    }
                });

        cargarTratamientos();
    }

    private Long leerId(String texto, String campo) {

        try {
            long id = Long.parseLong(texto.trim());

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Ingrese un ID válido para " + campo
            );
        }
    }

    private DetalleTratamiento construirDetalle() {

        DetalleTratamiento detalle =
                new DetalleTratamiento();

        detalle.setMedicamentoId(
                leerId(
                        txtMedicamentoId.getText(),
                        "medicamento"
                )
        );

        try {
            detalle.setDosisIndicada(
                    new BigDecimal(
                            txtDosis.getText()
                                    .trim()
                                    .replace(",", ".")
                    )
            );

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "La dosis debe ser un número válido."
            );
        }

        detalle.setUnidad(cbUnidad.getValue());

        detalle.setInstrucciones(
                txtInstrucciones.getText().trim()
        );

        service.validarDetalle(detalle);

        return detalle;
    }

    @FXML
    private void agregarMedicamento() {

        try {
            DetalleTratamiento detalle =
                    construirDetalle();

            detallesNuevos.add(detalle);
            tablaDetalles.getItems().add(detalle);

            txtMedicamentoId.clear();
            txtDosis.clear();
            txtInstrucciones.clear();
            cbUnidad.setValue(null);

            mostrarEstado(
                    "Medicamento agregado correctamente."
            );

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void quitarMedicamento() {

        DetalleTratamiento seleccionado =
                tablaDetalles.getSelectionModel()
                        .getSelectedItem();

        if (seleccionado == null) {
            mostrarError(
                    "Seleccione un medicamento de la tabla."
            );
            return;
        }

        if (!detallesNuevos.contains(seleccionado)) {
            mostrarError(
                    "No se pueden quitar detalles ya guardados."
            );
            return;
        }

        detallesNuevos.remove(seleccionado);
        tablaDetalles.getItems().remove(seleccionado);

        mostrarEstado("Medicamento eliminado del formulario.");
    }

    private Tratamiento construirTratamiento() {

        Tratamiento tratamiento = new Tratamiento();

        tratamiento.setPacienteId(
                leerId(
                        txtPacienteId.getText(),
                        "paciente"
                )
        );

        tratamiento.setMedicoId(
                leerId(
                        txtMedicoId.getText(),
                        "médico"
                )
        );

        tratamiento.setFechaInicio(
                dpInicio.getValue()
        );

        tratamiento.setFechaFin(
                dpFin.getValue()
        );

        tratamiento.setActivo(true);

        for (DetalleTratamiento detalle : detallesNuevos) {

            DetalleTratamiento copia =
                    new DetalleTratamiento();

            copia.setMedicamentoId(
                    detalle.getMedicamentoId()
            );

            copia.setDosisIndicada(
                    detalle.getDosisIndicada()
            );

            copia.setUnidad(
                    detalle.getUnidad()
            );

            copia.setInstrucciones(
                    detalle.getInstrucciones()
            );

            tratamiento.agregarDetalle(copia);
        }

        return tratamiento;
    }

    @FXML
    private void guardarTratamiento() {

        try {

            if (tratamientoSeleccionadoId != null) {
                throw new IllegalArgumentException(
                        "Pulse Limpiar para registrar uno nuevo."
                );
            }

            Tratamiento tratamiento =
                    construirTratamiento();

            service.validarTratamiento(
                    tratamiento,
                    true
            );

            ejecutarOperacion(
                    () -> service.guardar(tratamiento),
                    "Tratamiento guardado correctamente."
            );

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void actualizarTratamiento() {

        try {

            if (tratamientoSeleccionadoId == null) {
                throw new IllegalArgumentException(
                        "Seleccione un tratamiento."
                );
            }

            Long id = tratamientoSeleccionadoId;

            Tratamiento datos =
                    construirTratamiento();

            service.validarTratamiento(
                    datos,
                    false
            );

            ejecutarOperacion(
                    () -> service.actualizar(id, datos),
                    "Tratamiento actualizado correctamente."
            );

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void desactivarTratamiento() {

        if (tratamientoSeleccionadoId == null) {
            mostrarError(
                    "Seleccione un tratamiento."
            );
            return;
        }

        Long id = tratamientoSeleccionadoId;

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Desea desactivar este tratamiento?",
                ButtonType.YES,
                ButtonType.NO
        );

        confirmacion.setHeaderText(
                "Confirmar desactivación"
        );

        confirmacion.showAndWait().ifPresent(respuesta -> {

            if (respuesta == ButtonType.YES) {
                ejecutarOperacion(
                        () -> service.desactivar(id),
                        "Tratamiento desactivado."
                );
            }
        });
    }

    @FXML
    private void buscarMedicamentosActivos() {

        try {

            Long pacienteId = leerId(
                    txtBuscarPaciente.getText(),
                    "paciente"
            );

            Task<List<DetalleTratamiento>> tarea =
                    new Task<>() {

                        @Override
                        protected List<DetalleTratamiento> call() {
                            return service.medicamentosActivos(
                                    pacienteId
                            );
                        }
                    };

            tarea.setOnSucceeded(event -> {

                tablaDetalles.getItems().setAll(
                        tarea.getValue()
                );

                mostrarEstado(
                        "Medicamentos activos encontrados: "
                                + tarea.getValue().size()
                );
            });

            tarea.setOnFailed(event ->
                    mostrarError(
                            tarea.getException().getMessage()
                    )
            );

            iniciarTarea(tarea);

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void seleccionarTratamiento(
            Tratamiento tratamiento) {

        tratamientoSeleccionadoId =
                tratamiento.getId();

        txtPacienteId.setText(
                String.valueOf(
                        tratamiento.getPacienteId()
                )
        );

        txtMedicoId.setText(
                String.valueOf(
                        tratamiento.getMedicoId()
                )
        );

        dpInicio.setValue(
                tratamiento.getFechaInicio()
        );

        dpFin.setValue(
                tratamiento.getFechaFin()
        );

        detallesNuevos.clear();

        tablaDetalles.getItems().setAll(
                tratamiento.getDetalles()
        );

        mostrarEstado(
                "Tratamiento #" +
                        tratamiento.getId() +
                        " seleccionado."
        );
    }

    @FXML
    private void cargarTratamientos() {

        Task<List<Tratamiento>> tarea =
                new Task<>() {

                    @Override
                    protected List<Tratamiento> call() {
                        return service.listar();
                    }
                };

        tarea.setOnSucceeded(event -> {

            listaTratamientos.getItems().setAll(
                    tarea.getValue()
            );

            mostrarEstado(
                    "Tratamientos cargados correctamente."
            );
        });

        tarea.setOnFailed(event ->
                mostrarError(
                        tarea.getException().getMessage()
                )
        );

        iniciarTarea(tarea);
    }

    @FXML
    private void limpiar() {

        tratamientoSeleccionadoId = null;

        listaTratamientos.getSelectionModel()
                .clearSelection();

        txtPacienteId.clear();
        txtMedicoId.clear();
        txtMedicamentoId.clear();
        txtDosis.clear();
        txtInstrucciones.clear();
        txtBuscarPaciente.clear();

        dpInicio.setValue(null);
        dpFin.setValue(null);

        cbUnidad.setValue(null);

        detallesNuevos.clear();
        tablaDetalles.getItems().clear();

        mostrarEstado("Formulario limpio.");
    }

    private void ejecutarOperacion(
            Runnable operacion,
            String mensajeExito) {

        habilitarBotones(false);

        Task<Void> tarea = new Task<>() {

            @Override
            protected Void call() {
                operacion.run();
                return null;
            }
        };

        tarea.setOnSucceeded(event -> {

            habilitarBotones(true);

            limpiar();
            cargarTratamientos();

            mostrarEstado(mensajeExito);
        });

        tarea.setOnFailed(event -> {

            habilitarBotones(true);

            mostrarError(
                    tarea.getException().getMessage()
            );
        });

        iniciarTarea(tarea);
    }

    private void iniciarTarea(Task<?> tarea) {

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void habilitarBotones(boolean habilitar) {

        btnGuardar.setDisable(!habilitar);
        btnActualizar.setDisable(!habilitar);
        btnDesactivar.setDisable(!habilitar);
        btnAgregar.setDisable(!habilitar);
    }

    private void mostrarEstado(String mensaje) {
        lblEstado.setText(mensaje);
    }

    private void mostrarError(String mensaje) {

        lblEstado.setText(
                "Error: " + mensaje
        );

        Alert alerta = new Alert(
                Alert.AlertType.ERROR
        );

        alerta.setTitle("Error");
        alerta.setHeaderText(
                "No se pudo completar la operación"
        );

        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
