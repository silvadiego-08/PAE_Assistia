package ni.edu.uam.assistia.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ni.edu.uam.assistia.dao.RegistroTomaDAO.Toma;
import ni.edu.uam.assistia.exception.ValidacionException;
import ni.edu.uam.assistia.model.DetalleTratamiento;
import ni.edu.uam.assistia.model.EstadoToma;
import ni.edu.uam.assistia.model.Paciente;
import ni.edu.uam.assistia.persistence.PacienteDao;
import ni.edu.uam.assistia.service.RegistroTomaService;
import ni.edu.uam.assistia.util.Alertas;
import ni.edu.uam.assistia.util.Async;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.function.Consumer;
import java.util.function.Function;

public class RegistroTomaController {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PacienteDao pacientes;
    private final RegistroTomaService servicio = new RegistroTomaService();
    private final Async async;
    private boolean cargandoPacientes;

    @FXML private VBox pagina;
    @FXML private ComboBox<Paciente> paciente;
    @FXML private ComboBox<DetalleTratamiento> medicamento;
    @FXML private DatePicker diaConsulta;
    @FXML private DatePicker fechaProgramada;
    @FXML private TextField horaProgramada;
    @FXML private TextField observacion;
    @FXML private TableView<Toma> tabla;
    @FXML private TableColumn<Toma, String> colMedicamento;
    @FXML private TableColumn<Toma, String> colProgramada;
    @FXML private TableColumn<Toma, String> colRealizada;
    @FXML private TableColumn<Toma, String> colEstado;
    @FXML private TableColumn<Toma, String> colObservacion;
    @FXML private Button btnProgramar;
    @FXML private Button btnReprogramar;
    @FXML private Button btnRealizada;
    @FXML private Label mensaje;

    public RegistroTomaController(PacienteDao pacientes, Async async) {
        this.pacientes = pacientes;
        this.async = async;
    }

    @FXML
    private void initialize() {
        LocalDate hoy = LocalDate.now();
        diaConsulta.setValue(hoy);
        fechaProgramada.setValue(hoy);
        paciente.setConverter(soloLectura(Paciente::getNombre));
        medicamento.setConverter(soloLectura(detalle -> {
            var tratamiento = detalle.getTratamiento();
            return "%s · %s %s · Tratamiento #%d".formatted(detalle.getMedicamento().getNombre(),
                    detalle.getDosisIndicada().stripTrailingZeros().toPlainString(),
                    detalle.getUnidad(), tratamiento.getId());
        }));

        columna(colMedicamento, Toma::medicamento);
        columna(colProgramada, toma -> formatear(toma.programada()));
        columna(colRealizada, toma -> formatear(toma.realizada()));
        columna(colEstado, toma -> toma.estado() == EstadoToma.PENDIENTE ? "Pendiente" : "Realizada");
        columna(colObservacion, Toma::observacion);

        paciente.valueProperty().addListener((obs, anterior, actual) -> {
            if (!cargandoPacientes) {
                medicamento.getItems().clear();
                medicamento.setValue(null);
                observacion.clear();
                mensaje.setText("");
                cargarPaciente();
            }
        });
        diaConsulta.valueProperty().addListener((obs, anterior, actual) -> cargarTabla());
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) ->
                actualizarBotones());
        medicamento.valueProperty().addListener((obs, anterior, actual) -> actualizarBotones());
        actualizarBotones();
        recargar();
    }

    @FXML
    private void recargar() {
        Paciente anterior = paciente.getValue();
        async.consultar(pacientes::listar, lista -> {
            cargandoPacientes = true;
            paciente.getItems().setAll(lista);
            paciente.setValue(anterior == null ? null : lista.stream()
                    .filter(item -> item.getId().equals(anterior.getId()))
                    .findFirst().orElse(null));
            cargandoPacientes = false;
            cargarPaciente();
        }, Alertas::mostrar, pagina);
    }

    private void cargarPaciente() {
        DetalleTratamiento anterior = medicamento.getValue();
        medicamento.setValue(null);
        medicamento.getItems().clear();
        Paciente seleccionado = paciente.getValue();
        if (seleccionado == null) {
            cargarTabla();
            return;
        }
        tabla.getItems().clear();
        async.consultar(() -> servicio.listarDetalles(seleccionado.getId()), detalles -> {
            medicamento.getItems().setAll(detalles);
            if (anterior != null) {
                detalles.stream().filter(item -> item.getId().equals(anterior.getId()))
                        .findFirst().ifPresent(medicamento::setValue);
            }
            cargarTabla();
        }, error -> {
            Alertas.mostrar(error);
            cargarTabla();
        }, pagina);
    }

    private void cargarTabla() {
        tabla.getItems().clear();
        Paciente seleccionado = paciente.getValue();
        LocalDate dia = diaConsulta.getValue();
        if (seleccionado != null && dia != null) {
            async.consultar(() -> servicio.listar(seleccionado.getId(), dia),
                    tomas -> tabla.getItems().setAll(tomas), Alertas::mostrar, pagina);
        }
    }

    @FXML
    private void programar() {
        ejecutarConHorario(hora -> {
            Long pacienteId = paciente.getValue().getId();
            Long detalleId = medicamento.getValue().getId();
            String nota = observacion.getText();
            guardar(() -> servicio.programar(pacienteId, detalleId, hora, nota),
                    "Toma programada para " + formatear(hora) + ".");
        });
    }

    @FXML
    private void reprogramar() {
        Toma toma = tabla.getSelectionModel().getSelectedItem();
        ejecutarConHorario(hora -> {
            Long pacienteId = paciente.getValue().getId();
            guardar(() -> servicio.reprogramar(toma.id(), pacienteId, hora),
                    "Toma reprogramada para " + formatear(hora) + ".");
        });
    }

    @FXML
    private void marcarRealizada() {
        Toma toma = tabla.getSelectionModel().getSelectedItem();
        Long pacienteId = paciente.getValue().getId();
        guardar(() -> servicio.marcarRealizada(toma.id(), pacienteId), "Toma marcada como realizada.");
    }

    private void ejecutarConHorario(Consumer<LocalDateTime> operacion) {
        try {
            LocalDate fecha = fechaProgramada.getValue();
            if (fecha == null) {
                throw new ValidacionException("Selecciona una fecha para la toma.");
            }
            LocalTime hora = LocalTime.parse(horaProgramada.getText().strip(), HORA);
            operacion.accept(fecha.atTime(hora));
        } catch (DateTimeParseException error) {
            Alertas.advertencia("Escribe la hora en formato de 24 horas, por ejemplo 08:30.");
        } catch (ValidacionException error) {
            Alertas.mostrar(error);
        }
    }

    private void guardar(Runnable operacion, String confirmacion) {
        mensaje.setText("");
        async.accion(operacion, () -> {
            mensaje.setText(confirmacion);
            cargarTabla();
        }, Alertas::mostrar, pagina);
    }

    private void actualizarBotones() {
        Toma toma = tabla.getSelectionModel().getSelectedItem();
        boolean pendiente = toma != null && toma.estado() == EstadoToma.PENDIENTE;
        btnProgramar.setDisable(medicamento.getValue() == null);
        btnReprogramar.setDisable(!pendiente);
        btnRealizada.setDisable(!pendiente);
    }

    private static <T> StringConverter<T> soloLectura(Function<T, String> texto) {
        return new StringConverter<>() {
            @Override public String toString(T valor) { return valor == null ? "" : texto.apply(valor); }
            @Override public T fromString(String valor) { throw new UnsupportedOperationException(); }
        };
    }

    private static void columna(TableColumn<Toma, String> columna, Function<Toma, String> texto) {
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(texto.apply(celda.getValue())));
    }

    private static String formatear(LocalDateTime fecha) {
        return fecha == null ? "" : FECHA_HORA.format(fecha);
    }
}
