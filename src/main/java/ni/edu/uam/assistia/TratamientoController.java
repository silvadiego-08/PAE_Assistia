package ni.edu.uam.assistia;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import ni.edu.uam.assistia.model.*;
import ni.edu.uam.assistia.persistence.HibernateConfig;
import ni.edu.uam.assistia.persistence.PacienteDao;
import ni.edu.uam.assistia.service.MedicoService;
import ni.edu.uam.assistia.service.MedicamentoService;
import ni.edu.uam.assistia.service.TratamientoService;

import java.math.BigDecimal;
import java.util.List;

public class TratamientoController {
    @FXML private ComboBox<Paciente> cbPaciente;
    @FXML private ComboBox<Medico> cbMedico;
    @FXML private ComboBox<Medicamento> cbMedicamento;
    @FXML private ComboBox<String> cbUnidad;
    @FXML private TextField txtDosis, txtInstrucciones, txtBuscarPaciente;
    @FXML private DatePicker dpInicio, dpFin;
    @FXML private TableView<DetalleTratamiento> tablaDetalles;
    @FXML private TableColumn<DetalleTratamiento, String> colMedicamento;
    @FXML private TableColumn<DetalleTratamiento, BigDecimal> colDosis;
    @FXML private TableColumn<DetalleTratamiento, String> colUnidad, colInstrucciones;
    @FXML private ListView<Tratamiento> listaTratamientos;
    @FXML private Label lblEstado;
    @FXML private Button btnGuardar, btnActualizar, btnDesactivar, btnAgregar;

    private final PacienteDao pacienteDao = new PacienteDao(HibernateConfig.compartida());
    private final MedicoService medicoService = new MedicoService();
    private final MedicamentoService medicamentoService = new MedicamentoService();
    private final TratamientoService service = new TratamientoService();
    private final ObservableList<DetalleTratamiento> detallesNuevos = FXCollections.observableArrayList();
    private Long tratamientoSeleccionadoId;

    @FXML
    public void initialize() {
        cbUnidad.setItems(FXCollections.observableArrayList(
                "mg", "g", "ml", "mcg", "tableta", "cápsula", "gotas", "unidad"));

        cbPaciente.setConverter(new StringConverter<>() {
            @Override public String toString(Paciente p) {
                return p == null ? "" : p.getNombre() + " (#" + p.getId() + ")";
            }
            @Override public Paciente fromString(String text) { return null; }
        });

        colMedicamento.setCellValueFactory(c -> {
            Medicamento m = c.getValue().getMedicamento();
            return new ReadOnlyStringWrapper(m == null ? "" : m.toString());
        });
        colDosis.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDosisIndicada()));
        colUnidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUnidad()));
        colInstrucciones.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getInstrucciones()));

        tablaDetalles.setItems(FXCollections.observableArrayList());
        listaTratamientos.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(Tratamiento t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null :
                        "Tratamiento #" + t.getId() + " | " + t.getFechaInicio() +
                        (t.isActivo() ? " | Activo" : " | Inactivo"));
            }
        });
        listaTratamientos.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, nuevo) -> {
                    if (nuevo != null) seleccionarTratamiento(nuevo);
                });

        cargarCatalogos();
        cargarTratamientos();
    }

    private void cargarCatalogos() {
        Task<Void> tarea = new Task<>() {
            private List<Paciente> pacientes;
            private List<Medico> medicos;
            private List<Medicamento> medicamentos;
            @Override protected Void call() {
                pacientes = pacienteDao.listar();
                medicos = medicoService.listar();
                medicamentos = medicamentoService.listar();
                return null;
            }
            @Override protected void succeeded() {
                cbPaciente.getItems().setAll(pacientes);
                cbMedico.getItems().setAll(medicos);
                cbMedicamento.getItems().setAll(medicamentos);
                mostrarEstado("Pacientes, médicos y medicamentos cargados.");
            }
            @Override protected void failed() { mostrarError(getException().getMessage()); }
        };
        iniciarTarea(tarea);
    }

    private DetalleTratamiento construirDetalle() {
        Medicamento medicamento = cbMedicamento.getValue();
        if (medicamento == null) throw new IllegalArgumentException("Seleccione un medicamento.");
        DetalleTratamiento detalle = new DetalleTratamiento();
        detalle.setMedicamento(medicamento);
        try {
            detalle.setDosisIndicada(new BigDecimal(txtDosis.getText().trim().replace(',', '.')));
        } catch (Exception e) {
            throw new IllegalArgumentException("La dosis debe ser un número válido.");
        }
        detalle.setUnidad(cbUnidad.getValue());
        detalle.setInstrucciones(txtInstrucciones.getText().trim());
        service.validarDetalle(detalle);
        return detalle;
    }

    @FXML private void agregarMedicamento() {
        try {
            DetalleTratamiento detalle = construirDetalle();
            detallesNuevos.add(detalle);
            tablaDetalles.getItems().add(detalle);
            cbMedicamento.setValue(null);
            txtDosis.clear();
            txtInstrucciones.clear();
            cbUnidad.setValue(null);
            mostrarEstado("Medicamento agregado al formulario.");
        } catch (Exception e) { mostrarError(e.getMessage()); }
    }

    @FXML private void quitarMedicamento() {
        DetalleTratamiento detalle = tablaDetalles.getSelectionModel().getSelectedItem();
        if (detalle == null) { mostrarError("Seleccione un medicamento de la tabla."); return; }
        if (!detallesNuevos.contains(detalle)) {
            mostrarError("No se pueden quitar detalles que ya están guardados.");
            return;
        }
        detallesNuevos.remove(detalle);
        tablaDetalles.getItems().remove(detalle);
        mostrarEstado("Medicamento quitado del formulario.");
    }

    private Tratamiento construirTratamiento() {
        Tratamiento tratamiento = new Tratamiento();
        tratamiento.setPaciente(cbPaciente.getValue());
        tratamiento.setMedico(cbMedico.getValue());
        tratamiento.setFechaInicio(dpInicio.getValue());
        tratamiento.setFechaFin(dpFin.getValue());
        tratamiento.setActivo(true);
        for (DetalleTratamiento detalle : detallesNuevos) {
            DetalleTratamiento copia = new DetalleTratamiento();
            copia.setMedicamento(detalle.getMedicamento());
            copia.setDosisIndicada(detalle.getDosisIndicada());
            copia.setUnidad(detalle.getUnidad());
            copia.setInstrucciones(detalle.getInstrucciones());
            tratamiento.agregarDetalle(copia);
        }
        return tratamiento;
    }

    @FXML private void guardarTratamiento() {
        try {
            if (tratamientoSeleccionadoId != null)
                throw new IllegalArgumentException("Pulse Limpiar para registrar un tratamiento nuevo.");
            Tratamiento tratamiento = construirTratamiento();
            service.validarTratamiento(tratamiento, true);
            ejecutarOperacion(() -> service.guardar(tratamiento), "Tratamiento guardado correctamente.");
        } catch (Exception e) { mostrarError(e.getMessage()); }
    }

    @FXML private void actualizarTratamiento() {
        try {
            if (tratamientoSeleccionadoId == null)
                throw new IllegalArgumentException("Seleccione un tratamiento.");
            Long id = tratamientoSeleccionadoId;
            Tratamiento datos = construirTratamiento();
            service.validarTratamiento(datos, false);
            ejecutarOperacion(() -> {
                service.actualizar(id, datos);
                for (DetalleTratamiento detalle : datos.getDetalles()) {
                    service.agregarDetalle(id, detalle);
                }
            }, "Tratamiento actualizado correctamente.");
        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }
        @FXML private void desactivarTratamiento() {
            if (tratamientoSeleccionadoId == null) {
                mostrarError("Seleccione un tratamiento."); return;
            }
            Long id = tratamientoSeleccionadoId;
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Desea desactivar este tratamiento?", ButtonType.YES, ButtonType.NO);
            confirmacion.setHeaderText("Confirmar desactivación");
            confirmacion.showAndWait().ifPresent(respuesta -> {
                if (respuesta == ButtonType.YES)
                    ejecutarOperacion(() -> service.desactivar(id), "Tratamiento desactivado.");
            });
        }

        @FXML private void buscarMedicamentosActivos() {
            try {
                long pacienteId = Long.parseLong(txtBuscarPaciente.getText().trim());
                if (pacienteId <= 0) throw new NumberFormatException();
                Task<List<DetalleTratamiento>> tarea = new Task<>() {
                    @Override protected List<DetalleTratamiento> call() {
                        return service.medicamentosActivos(pacienteId);
                    }
                };
                tarea.setOnSucceeded(e -> {
                    tablaDetalles.getItems().setAll(tarea.getValue());
                    mostrarEstado("Medicamentos activos encontrados: " + tarea.getValue().size());
                });
                tarea.setOnFailed(e -> mostrarError(tarea.getException().getMessage()));
                iniciarTarea(tarea);
            } catch (NumberFormatException e) { mostrarError("Ingrese un ID de paciente válido."); }
        }

        private void seleccionarTratamiento(Tratamiento tratamiento) {
            tratamientoSeleccionadoId = tratamiento.getId();
            cbPaciente.getSelectionModel().select(
                    buscarPacienteEnLista(tratamiento.getPaciente().getId()));
            cbMedico.getSelectionModel().select(
                    buscarMedicoEnLista(tratamiento.getMedico().getId()));
            dpInicio.setValue(tratamiento.getFechaInicio());
            dpFin.setValue(tratamiento.getFechaFin());
            detallesNuevos.clear();
            tablaDetalles.getItems().setAll(tratamiento.getDetalles());
            mostrarEstado("Tratamiento #" + tratamiento.getId() + " seleccionado.");
        }

        private Paciente buscarPacienteEnLista(Long id) {
            return cbPaciente.getItems().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        }
        private Medico buscarMedicoEnLista(Long id) {
            return cbMedico.getItems().stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
        }

        @FXML private void cargarTratamientos() {
            Task<List<Tratamiento>> tarea = new Task<>() {
                @Override protected List<Tratamiento> call() { return service.listar(); }
            };
            tarea.setOnSucceeded(e -> {
                listaTratamientos.getItems().setAll(tarea.getValue());
                mostrarEstado("Tratamientos cargados correctamente.");
            });
            tarea.setOnFailed(e -> mostrarError(tarea.getException().getMessage()));
            iniciarTarea(tarea);
        }

    @FXML private void limpiar() {
        tratamientoSeleccionadoId = null;
        listaTratamientos.getSelectionModel().clearSelection();
        cbPaciente.setValue(null);
        cbMedico.setValue(null);
        cbMedicamento.setValue(null);
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

    private void ejecutarOperacion(Runnable operacion, String mensaje) {
        habilitarBotones(false);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() { operacion.run(); return null; }
        };
        tarea.setOnSucceeded(e -> {
            habilitarBotones(true);
            limpiar();
            cargarTratamientos();
            mostrarEstado(mensaje);
        });
        tarea.setOnFailed(e -> {
            habilitarBotones(true);
            mostrarError(tarea.getException().getMessage());
        });
        iniciarTarea(tarea);
    }

    private void iniciarTarea(Task<?> tarea) {
        Thread hilo = new Thread(tarea, "tratamientos-db");
        hilo.setDaemon(true);
        hilo.start();
    }
    private void habilitarBotones(boolean habilitar) {
        btnGuardar.setDisable(!habilitar);
        btnActualizar.setDisable(!habilitar);
        btnDesactivar.setDisable(!habilitar);
        btnAgregar.setDisable(!habilitar);
    }
    private void mostrarEstado(String mensaje) { lblEstado.setText(mensaje); }
    private void mostrarError(String mensaje) {
        lblEstado.setText("Error: " + mensaje);
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error");
        alerta.setHeaderText("No se pudo completar la operación");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}