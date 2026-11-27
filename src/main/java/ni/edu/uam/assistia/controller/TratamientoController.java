package ni.edu.uam.assistia.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ni.edu.uam.assistia.exception.ValidacionException;
import ni.edu.uam.assistia.model.DetalleTratamiento;
import ni.edu.uam.assistia.model.Medicamento;
import ni.edu.uam.assistia.model.Medico;
import ni.edu.uam.assistia.model.Paciente;
import ni.edu.uam.assistia.model.Tratamiento;
import ni.edu.uam.assistia.persistence.PacienteDao;
import ni.edu.uam.assistia.service.MedicamentoService;
import ni.edu.uam.assistia.service.MedicoService;
import ni.edu.uam.assistia.service.TratamientoService;
import ni.edu.uam.assistia.util.Alertas;
import ni.edu.uam.assistia.util.Async;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TratamientoController {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    @FXML private VBox pagina;
    @FXML private VBox historial, editor;
    @FXML private ComboBox<Paciente> cbPaciente, cbPacienteConsulta;
    @FXML private ComboBox<Medico> cbMedico;
    @FXML private ComboBox<Medicamento> cbMedicamento;
    @FXML private ComboBox<String> cbUnidad;
    @FXML private TextField txtDosis, txtInstrucciones;
    @FXML private DatePicker dpInicio, dpFin;
    @FXML private TableView<DetalleTratamiento> tablaDetalles, tablaActivos;
    @FXML private TableColumn<DetalleTratamiento, String> colMedicamento, colDosis, colInstrucciones;
    @FXML private TableColumn<DetalleTratamiento, String> colActivoTratamiento, colActivoMedicamento, colActivoDosis;
    @FXML private ListView<Tratamiento> listaTratamientos;
    @FXML private Label lblEstado, lblConsulta, lblEditor, lblHistorial;
    @FXML private Button btnAbrir, btnGuardar, btnDesactivar, btnQuitar;

    private final PacienteDao pacienteDao;
    private final Async async;
    private final MedicoService medicoService = new MedicoService();
    private final MedicamentoService medicamentoService = new MedicamentoService();
    private final TratamientoService service = new TratamientoService();
    private Long tratamientoSeleccionadoId;

    public TratamientoController(PacienteDao pacienteDao, Async async) {
        this.pacienteDao = pacienteDao;
        this.async = async;
    }

    @FXML
    private void initialize() {
        StringConverter<Paciente> formatoPaciente = new StringConverter<>() {
            @Override
            public String toString(Paciente paciente) {
                return paciente == null ? "" : paciente.getNombre() + " (#" + paciente.getId() + ")";
            }

            @Override
            public Paciente fromString(String texto) {
                return null;
            }
        };
        cbPaciente.setConverter(formatoPaciente);
        cbPacienteConsulta.setConverter(formatoPaciente);
        cbUnidad.getItems().setAll(TratamientoService.UNIDADES);

        colMedicamento.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getMedicamento().toString()));
        colDosis.setCellValueFactory(d -> new ReadOnlyStringWrapper(describirDosis(d.getValue())));
        colInstrucciones.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getInstrucciones()));
        colActivoTratamiento.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                "#" + d.getValue().getTratamiento().getId()));
        colActivoMedicamento.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getMedicamento().toString()));
        colActivoDosis.setCellValueFactory(d -> new ReadOnlyStringWrapper(describirDosis(d.getValue())));

        listaTratamientos.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Tratamiento tratamiento, boolean vacia) {
                super.updateItem(tratamiento, vacia);
                setText(vacia || tratamiento == null ? null :
                        tratamiento.getPaciente().getNombre() + " · Tratamiento #" + tratamiento.getId()
                                + " · Inicio " + FECHA.format(tratamiento.getFechaInicio()) + " · "
                                + (tratamiento.isActivo() ? "Activo" : "Inactivo"));
            }
        });
        btnAbrir.disableProperty().bind(listaTratamientos.getSelectionModel().selectedItemProperty().isNull());
        tablaDetalles.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) ->
                btnQuitar.setDisable(seleccionado == null || seleccionado.getId() != null));
        historial.managedProperty().bind(historial.visibleProperty());
        editor.managedProperty().bind(editor.visibleProperty());
        tablaDetalles.visibleProperty().bind(Bindings.isNotEmpty(tablaDetalles.getItems()));
        tablaDetalles.managedProperty().bind(tablaDetalles.visibleProperty());
        btnQuitar.visibleProperty().bind(tablaDetalles.visibleProperty());
        btnQuitar.managedProperty().bind(btnQuitar.visibleProperty());
        btnDesactivar.managedProperty().bind(btnDesactivar.visibleProperty());
        lblEstado.visibleProperty().bind(lblEstado.textProperty().isNotEmpty());
        lblEstado.managedProperty().bind(lblEstado.visibleProperty());
        cbPacienteConsulta.valueProperty().addListener((obs, anterior, paciente) -> {
            tablaActivos.getItems().clear();
            lblConsulta.setText("Seleccione un paciente y pulse «Consultar».");
        });
        recargar();
    }

    @FXML
    private void agregarMedicamento() {
        try {
            tablaDetalles.getItems().add(leerMedicamento());
            limpiarDetalle();
            lblEstado.setText("Medicamento añadido. Guarda el tratamiento para confirmar.");
            cbMedicamento.requestFocus();
        } catch (ValidacionException error) {
            Alertas.mostrar(error);
        }
    }

    private DetalleTratamiento leerMedicamento() {
        DetalleTratamiento detalle = new DetalleTratamiento();
        detalle.setMedicamento(cbMedicamento.getValue());
        try {
            detalle.setDosisIndicada(new BigDecimal(txtDosis.getText().strip().replace(',', '.')));
        } catch (NumberFormatException error) {
            throw new ValidacionException("Escribe una dosis numérica para el medicamento.");
        }
        detalle.setUnidad(cbUnidad.getValue());
        String instrucciones = txtInstrucciones.getText().strip();
        detalle.setInstrucciones(instrucciones.isEmpty() ? null : instrucciones);
        service.validarDetalle(detalle);
        return detalle;
    }

    @FXML
    private void quitarMedicamento() {
        DetalleTratamiento detalle = tablaDetalles.getSelectionModel().getSelectedItem();
        if (detalle == null) {
            Alertas.advertencia("Seleccione un medicamento de la tabla.");
            return;
        }
        if (detalle.getId() != null) {
            Alertas.advertencia("El medicamento ya está guardado en el historial.");
            return;
        }
        tablaDetalles.getItems().remove(detalle);
        lblEstado.setText("Medicamento quitado del formulario.");
    }

    @FXML
    private void guardar() {
        try {
            Tratamiento datos = datosFormulario();
            if (cbMedicamento.getValue() != null || cbUnidad.getValue() != null
                    || !txtDosis.getText().isBlank() || !txtInstrucciones.getText().isBlank()) {
                datos.agregarDetalle(leerMedicamento());
            }
            guardarCambios(datos, tratamientoSeleccionadoId == null);
        } catch (ValidacionException error) {
            Alertas.mostrar(error);
        }
    }

    @FXML
    private void desactivar() {
        Tratamiento seleccionado = listaTratamientos.getSelectionModel().getSelectedItem();
        if (seleccionado == null || !seleccionado.isActivo()) {
            Alertas.advertencia("Seleccione un tratamiento activo del historial.");
            return;
        }
        if (Alertas.confirmar("¿Desactivar el tratamiento #" + seleccionado.getId()
                + "? El tratamiento quedará en el historial y se descartarán los cambios sin guardar.")) {
            ejecutar(() -> service.desactivar(seleccionado.getId()), "Tratamiento desactivado.");
        }
    }

    @FXML
    private void consultarActivos() {
        Paciente paciente = cbPacienteConsulta.getValue();
        if (paciente == null) {
            Alertas.advertencia("Seleccione un paciente para consultar sus medicamentos activos.");
            return;
        }
        async.consultar(() -> service.medicamentosActivos(paciente.getId()), this::mostrarActivos,
                Alertas::mostrar, pagina);
    }

    @FXML
    private void recargar() {
        recargar("Datos actualizados.");
    }

    @FXML
    private void nuevo() {
        limpiarFormulario();
        lblEditor.setText("Nuevo tratamiento");
        btnGuardar.setText("Guardar tratamiento");
        btnDesactivar.setVisible(false);
        abrirEditor();
        cbPaciente.requestFocus();
    }

    @FXML
    private void abrirSeleccionado() {
        Tratamiento tratamiento = listaTratamientos.getSelectionModel().getSelectedItem();
        if (tratamiento != null) {
            mostrarTratamiento(tratamiento);
            abrirEditor();
            cbMedico.requestFocus();
        }
    }

    private void abrirEditor() {
        historial.setVisible(false);
        editor.setVisible(true);
        lblEstado.setText("");
    }

    @FXML
    private void cancelar() {
        if (Alertas.confirmar("¿Volver al historial? Los cambios sin guardar se descartarán.")) {
            cerrarEditor();
            listaTratamientos.requestFocus();
        }
    }

    private void cerrarEditor() {
        limpiarFormulario();
        editor.setVisible(false);
        historial.setVisible(true);
    }

    private void guardarCambios(Tratamiento datos, boolean nuevo) {
        try {
            service.validarTratamiento(datos, nuevo);
            if (nuevo) {
                ejecutar(() -> service.guardar(datos), "Tratamiento guardado.");
            } else {
                Long id = tratamientoSeleccionadoId;
                ejecutar(() -> service.actualizar(id, datos), "Tratamiento actualizado.");
            }
        } catch (ValidacionException error) {
            Alertas.mostrar(error);
        }
    }

    private Tratamiento datosFormulario() {
        Tratamiento tratamiento = new Tratamiento();
        tratamiento.setPaciente(cbPaciente.getValue());
        tratamiento.setMedico(cbMedico.getValue());
        tratamiento.setFechaInicio(dpInicio.getValue());
        tratamiento.setFechaFin(dpFin.getValue());
        for (DetalleTratamiento detalle : tablaDetalles.getItems()) {
            if (detalle.getId() == null) {
                DetalleTratamiento copia = new DetalleTratamiento();
                copia.setMedicamento(detalle.getMedicamento());
                copia.setDosisIndicada(detalle.getDosisIndicada());
                copia.setUnidad(detalle.getUnidad());
                copia.setInstrucciones(detalle.getInstrucciones());
                tratamiento.agregarDetalle(copia);
            }
        }
        return tratamiento;
    }

    private void mostrarTratamiento(Tratamiento tratamiento) {
        tratamientoSeleccionadoId = tratamiento.getId();
        cbPaciente.setValue(buscarPaciente(tratamiento.getPaciente().getId()));
        cbMedico.setValue(cbMedico.getItems().stream()
                .filter(medico -> medico.getId().equals(tratamiento.getMedico().getId()))
                .findFirst().orElse(null));
        dpInicio.setValue(tratamiento.getFechaInicio());
        dpFin.setValue(tratamiento.getFechaFin());
        limpiarDetalle();
        tablaDetalles.getItems().setAll(tratamiento.getDetalles());
        lblEditor.setText("Editar tratamiento #" + tratamiento.getId() + " de " + tratamiento.getPaciente().getNombre());
        btnGuardar.setText("Guardar cambios");
        btnDesactivar.setVisible(tratamiento.isActivo());
    }

    private void mostrarActivos(List<DetalleTratamiento> activos) {
        tablaActivos.getItems().setAll(activos);
        lblConsulta.setText(activos.isEmpty() ? "El paciente no tiene medicamentos en tratamientos activos."
                : "Medicamentos encontrados: " + activos.size());
    }

    private void ejecutar(Runnable operacion, String mensaje) {
        async.accion(operacion, () -> {
            cerrarEditor();
            lblHistorial.setText(mensaje);
            recargar(mensaje);
        }, Alertas::mostrar, pagina);
    }

    private void recargar(String mensaje) {
        Paciente filtro = cbPacienteConsulta.getValue();
        async.consultar(() -> new DatosPantalla(pacienteDao.listar(), medicoService.listar(), medicamentoService.listar(),
                service.listar(), filtro == null ? List.of() : service.medicamentosActivos(filtro.getId())), datos -> {
            limpiarFormulario();
            cbPaciente.getItems().setAll(datos.pacientes());
            cbPacienteConsulta.getItems().setAll(datos.pacientes());
            cbMedico.getItems().setAll(datos.medicos());
            cbMedicamento.getItems().setAll(datos.medicamentos());
            listaTratamientos.getItems().setAll(datos.tratamientos());
            if (filtro != null) {
                cbPacienteConsulta.setValue(buscarPaciente(filtro.getId()));
                if (cbPacienteConsulta.getValue() != null) mostrarActivos(datos.activos());
            }
            lblHistorial.setText(mensaje);
        }, Alertas::mostrar, pagina);
    }

    private Paciente buscarPaciente(Long id) {
        return cbPaciente.getItems().stream().filter(paciente -> paciente.getId().equals(id)).findFirst().orElse(null);
    }

    private void limpiarFormulario() {
        tratamientoSeleccionadoId = null;
        listaTratamientos.getSelectionModel().clearSelection();
        cbPaciente.setValue(null);
        cbMedico.setValue(null);
        dpInicio.setValue(null);
        dpFin.setValue(null);
        tablaDetalles.getItems().clear();
        limpiarDetalle();
    }

    private void limpiarDetalle() {
        cbMedicamento.setValue(null);
        cbUnidad.setValue(null);
        txtDosis.clear();
        txtInstrucciones.clear();
    }

    private String describirDosis(DetalleTratamiento detalle) {
        return detalle.getDosisIndicada().stripTrailingZeros().toPlainString() + " " + detalle.getUnidad();
    }

    private record DatosPantalla(List<Paciente> pacientes, List<Medico> medicos, List<Medicamento> medicamentos,
                                 List<Tratamiento> tratamientos, List<DetalleTratamiento> activos) { }


    public void actualizarCatalogos() {
        async.consultar(
                () -> new DatosCatalogos(
                        pacienteDao.listar(),
                        medicoService.listar(),
                        medicamentoService.listar()
                ),
                datos -> {
                    cbPaciente.getItems().setAll(datos.pacientes());
                    cbPacienteConsulta.getItems().setAll(datos.pacientes());
                    cbMedico.getItems().setAll(datos.medicos());
                    cbMedicamento.getItems().setAll(datos.medicamentos());
                },
                Alertas::mostrar,
                pagina
        );
    }

    private record DatosCatalogos(
            List<Paciente> pacientes,
            List<Medico> medicos,
            List<Medicamento> medicamentos
    ) {}
}
