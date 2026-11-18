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
import java.util.function.Function;

public class RegistroTomaController {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PacienteDao pacientes;
    private final RegistroTomaService servicio = new RegistroTomaService();
    private final Async async;
    private boolean cargandoPacientes;
    private Toma tomaEnEdicion;

    @FXML private VBox pagina;
    @FXML private VBox listado, editor, campoMedicamento, campoObservacion;
    @FXML private javafx.scene.layout.HBox filtros;
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
    @FXML private Button btnNueva, btnGuardar;
    @FXML private Button btnReprogramar;
    @FXML private Button btnRealizada;
    @FXML private Label mensaje, tituloEditor, contextoEditor, seleccion, vacio, estadoPaciente;

    public RegistroTomaController(PacienteDao pacientes, Async async) {
        this.pacientes = pacientes;
        this.async = async;
    }

    @FXML
    private void initialize() {
        LocalDate hoy = LocalDate.now();
        diaConsulta.setValue(hoy);
        fechaProgramada.setValue(hoy);
        paciente.setConverter(soloLectura(valor -> valor.getNombre() + " (#" + valor.getId() + ")"));
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

        listado.managedProperty().bind(listado.visibleProperty());
        editor.managedProperty().bind(editor.visibleProperty());
        filtros.disableProperty().bind(editor.visibleProperty());
        campoMedicamento.managedProperty().bind(campoMedicamento.visibleProperty());
        campoObservacion.managedProperty().bind(campoObservacion.visibleProperty());
        mensaje.visibleProperty().bind(mensaje.textProperty().isNotEmpty());
        mensaje.managedProperty().bind(mensaje.visibleProperty());
        estadoPaciente.visibleProperty().bind(estadoPaciente.textProperty().isNotEmpty());
        estadoPaciente.managedProperty().bind(estadoPaciente.visibleProperty());

        paciente.valueProperty().addListener((obs, anterior, actual) -> {
            if (!cargandoPacientes) {
                medicamento.getItems().clear();
                medicamento.setValue(null);
                observacion.clear();
                mensaje.setText("");
                cargarPaciente();
            }
        });
        diaConsulta.valueProperty().addListener((obs, anterior, actual) -> {
            if (!cargandoPacientes) {
                mensaje.setText("");
                cargarTabla();
            }
        });
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) ->
                actualizarBotones());
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
            mensaje.setText("");
            cargarPaciente();
        }, Alertas::mostrar, pagina);
    }

    private void cargarPaciente() {
        medicamento.setValue(null);
        medicamento.getItems().clear();
        estadoPaciente.setText("");
        Paciente seleccionado = paciente.getValue();
        if (seleccionado == null) {
            cargarTabla();
            return;
        }
        tabla.getItems().clear();
        async.consultar(() -> servicio.listarDetalles(seleccionado.getId()), detalles -> {
            medicamento.getItems().setAll(detalles);
            estadoPaciente.setText(detalles.isEmpty()
                    ? "Este paciente necesita un tratamiento activo con medicamentos para programar una toma." : "");
            cargarTabla();
        }, error -> {
            estadoPaciente.setText("No se pudieron cargar los medicamentos. Pulsa «Recargar» para intentar de nuevo.");
            Alertas.mostrar(error);
            cargarTabla();
        }, pagina);
    }

    private void cargarTabla() {
        tabla.getItems().clear();
        Paciente seleccionado = paciente.getValue();
        LocalDate dia = diaConsulta.getValue();
        actualizarBotones();
        vacio.setText(seleccionado == null ? "Selecciona un paciente para ver sus tomas."
                : dia == null ? "Selecciona el día que quieres consultar." : "Cargando tomas…");
        if (seleccionado == null) {
            estadoPaciente.setText("");
        }
        if (seleccionado != null && dia != null) {
            async.consultar(() -> servicio.listar(seleccionado.getId(), dia),
                    tomas -> {
                        tabla.getItems().setAll(tomas);
                        vacio.setText("No hay tomas para este día.");
                    }, error -> {
                        vacio.setText("No se pudieron cargar las tomas. Pulsa «Recargar» para intentar de nuevo.");
                        Alertas.mostrar(error);
                    }, pagina);
        }
    }

    @FXML
    private void nueva() {
        if (paciente.getValue() == null) return;
        tomaEnEdicion = null;
        tituloEditor.setText("Nueva toma");
        contextoEditor.setText(paciente.getValue().getNombre());
        campoMedicamento.setVisible(true);
        campoObservacion.setVisible(true);
        medicamento.setValue(null);
        observacion.clear();
        fechaProgramada.setValue(diaConsulta.getValue());
        horaProgramada.clear();
        btnGuardar.setText("Programar toma");
        abrirEditor();
        medicamento.requestFocus();
    }

    @FXML
    private void reprogramar() {
        Toma toma = tabla.getSelectionModel().getSelectedItem();
        if (toma == null || toma.estado() != EstadoToma.PENDIENTE) return;
        tomaEnEdicion = toma;
        tituloEditor.setText("Cambiar horario");
        contextoEditor.setText(paciente.getValue().getNombre() + " · " + toma.medicamento()
                + "\nHorario actual: " + formatear(toma.programada()));
        campoMedicamento.setVisible(false);
        campoObservacion.setVisible(false);
        fechaProgramada.setValue(toma.programada().toLocalDate());
        horaProgramada.setText(HORA.format(toma.programada()));
        btnGuardar.setText("Guardar nuevo horario");
        abrirEditor();
        horaProgramada.requestFocus();
    }

    private void abrirEditor() {
        mensaje.setText("");
        listado.setVisible(false);
        editor.setVisible(true);
    }

    @FXML
    private void cancelar() {
        cerrarEditor();
        mensaje.setText("");
        btnNueva.requestFocus();
    }

    private void cerrarEditor() {
        editor.setVisible(false);
        listado.setVisible(true);
        tomaEnEdicion = null;
    }

    @FXML
    private void guardarHorario() {
        try {
            LocalDateTime hora = leerHorario();
            Long pacienteId = paciente.getValue().getId();
            if (tomaEnEdicion != null) {
                Long tomaId = tomaEnEdicion.id();
                guardar(() -> servicio.reprogramar(tomaId, pacienteId, hora),
                        "Horario cambiado a " + formatear(hora) + ".", hora.toLocalDate());
            } else {
                if (medicamento.getValue() == null) {
                    throw new ValidacionException("Selecciona un medicamento del tratamiento.");
                }
                Long detalleId = medicamento.getValue().getId();
                String nota = observacion.getText();
                guardar(() -> servicio.programar(pacienteId, detalleId, hora, nota),
                        "Toma programada para " + formatear(hora) + ".", hora.toLocalDate());
            }
        } catch (DateTimeParseException error) {
            Alertas.advertencia("Escribe la hora en formato de 24 horas, por ejemplo 08:30.");
        } catch (ValidacionException error) {
            Alertas.mostrar(error);
        }
    }

    @FXML
    private void marcarRealizada() {
        Toma toma = tabla.getSelectionModel().getSelectedItem();
        if (toma == null || toma.estado() != EstadoToma.PENDIENTE) return;
        Long pacienteId = paciente.getValue().getId();
        if (Alertas.confirmar("¿Registrar como realizada la toma de " + toma.medicamento()
                + " programada para " + formatear(toma.programada()) + "? Se guardará la hora actual.")) {
            guardar(() -> servicio.marcarRealizada(toma.id(), pacienteId),
                    "Toma marcada como realizada.", diaConsulta.getValue());
        }
    }

    private LocalDateTime leerHorario() {
        LocalDate fecha = fechaProgramada.getValue();
        if (fecha == null) {
            throw new ValidacionException("Selecciona una fecha para la toma.");
        }
        return fecha.atTime(LocalTime.parse(horaProgramada.getText().strip(), HORA));
    }

    private void guardar(Runnable operacion, String confirmacion, LocalDate dia) {
        mensaje.setText("");
        async.accion(operacion, () -> {
            cerrarEditor();
            cargandoPacientes = true;
            diaConsulta.setValue(dia);
            cargandoPacientes = false;
            mensaje.setText(confirmacion);
            cargarTabla();
        }, Alertas::mostrar, pagina);
    }

    private void actualizarBotones() {
        Toma toma = tabla.getSelectionModel().getSelectedItem();
        boolean pendiente = toma != null && toma.estado() == EstadoToma.PENDIENTE;
        btnNueva.setDisable(paciente.getValue() == null || medicamento.getItems().isEmpty());
        btnReprogramar.setDisable(!pendiente);
        btnRealizada.setDisable(!pendiente);
        seleccion.setText(toma == null ? "Selecciona una toma para registrar su realización o cambiar el horario."
                : toma.medicamento() + " · " + formatear(toma.programada())
                + (pendiente ? " · Pendiente" : " · Realizada a las " + formatear(toma.realizada())));
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
