package ni.edu.uam.assistia.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
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
import java.util.List;

public class TratamientoController {
    @FXML private VBox pagina;
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
    @FXML private Label lblEstado, lblConsulta;

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
                        "#" + tratamiento.getId() + " · " + tratamiento.getPaciente().getNombre()
                                + " · " + tratamiento.getFechaInicio() + " · "
                                + (tratamiento.isActivo() ? "Activo" : "Inactivo"));
            }
        });
        listaTratamientos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null) mostrarTratamiento(seleccionado);
        });
        cbPacienteConsulta.valueProperty().addListener((obs, anterior, paciente) -> {
            tablaActivos.getItems().clear();
            lblConsulta.setText("Seleccione un paciente y pulse «Consultar».");
        });
        recargar();
    }

    @FXML
    private void agregarMedicamento() {
        try {
            DetalleTratamiento detalle = new DetalleTratamiento();
            detalle.setMedicamento(cbMedicamento.getValue());
            detalle.setDosisIndicada(new BigDecimal(txtDosis.getText().strip().replace(',', '.')));
            detalle.setUnidad(cbUnidad.getValue());
            String instrucciones = txtInstrucciones.getText().strip();
            detalle.setInstrucciones(instrucciones.isEmpty() ? null : instrucciones);
            service.validarDetalle(detalle);
            tablaDetalles.getItems().add(detalle);
            limpiarDetalle();
            lblEstado.setText("Medicamento agregado al formulario.");
        } catch (NumberFormatException error) {
            Alertas.advertencia("Ingrese una dosis numérica.");
        } catch (ValidacionException error) {
            Alertas.mostrar(error);
        }
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
        if (tratamientoSeleccionadoId != null) {
            Alertas.advertencia("Pulse «Actualizar» para guardar cambios o «Limpiar» para crear un tratamiento.");
            return;
        }
        Tratamiento datos = datosFormulario();
        guardarCambios(datos, true);
    }

    @FXML
    private void actualizar() {
        if (tratamientoSeleccionadoId == null) {
            Alertas.advertencia("Seleccione un tratamiento del historial.");
            return;
        }
        guardarCambios(datosFormulario(), false);
    }

    @FXML
    private void desactivar() {
        Tratamiento seleccionado = listaTratamientos.getSelectionModel().getSelectedItem();
        if (seleccionado == null || !seleccionado.isActivo()) {
            Alertas.advertencia("Seleccione un tratamiento activo del historial.");
            return;
        }
        if (Alertas.confirmar("¿Desactivar el tratamiento #" + seleccionado.getId() + "?")) {
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
    private void limpiar() {
        limpiarFormulario();
        lblEstado.setText("Formulario limpio.");
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
        cbMedico.getSelectionModel().select(tratamiento.getMedico());
        dpInicio.setValue(tratamiento.getFechaInicio());
        dpFin.setValue(tratamiento.getFechaFin());
        limpiarDetalle();
        tablaDetalles.getItems().setAll(tratamiento.getDetalles());
        lblEstado.setText("Tratamiento #" + tratamiento.getId() + " seleccionado.");
    }

    private void mostrarActivos(List<DetalleTratamiento> activos) {
        tablaActivos.getItems().setAll(activos);
        lblConsulta.setText(activos.isEmpty() ? "El paciente no tiene medicamentos en tratamientos activos."
                : "Medicamentos encontrados: " + activos.size());
    }

    private void ejecutar(Runnable operacion, String mensaje) {
        async.accion(operacion, () -> {
            limpiarFormulario();
            lblEstado.setText(mensaje);
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
            lblEstado.setText(mensaje);
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
}
