package ni.edu.uam.assistia;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import ni.edu.uam.assistia.modelo.DetalleTratamiento;
import ni.edu.uam.assistia.modelo.Paciente;
import ni.edu.uam.assistia.persistencia.RegistroTomaDAO;
import ni.edu.uam.assistia.persistencia.RegistroTomaException;
import ni.edu.uam.assistia.persistencia.TomaPendienteVista;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class RegistroTomaController {

    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RegistroTomaDAO dao = new RegistroTomaDAO();

    @FXML
    private ComboBox<Paciente> pacienteCombo;
    @FXML
    private ComboBox<DetalleTratamiento> detalleCombo;
    @FXML
    private DatePicker fechaPicker;
    @FXML
    private TextField horaField;
    @FXML
    private TextField observacionField;
    @FXML
    private TableView<TomaPendienteVista> tablaPendientes;
    @FXML
    private TableColumn<TomaPendienteVista, String> colMedicamento;
    @FXML
    private TableColumn<TomaPendienteVista, String> colHorario;
    @FXML
    private TableColumn<TomaPendienteVista, String> colEstado;
    @FXML
    private TableColumn<TomaPendienteVista, String> colObservacion;
    @FXML
    private Label mensajeLabel;

    @FXML
    public void initialize() {
        colMedicamento.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().medicamento()));
        colHorario.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().fechaHoraProgramada().format(FORMATO_FECHA_HORA)));
        colEstado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().estado().name()));
        colObservacion.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().observacion() == null
                        ? "" : c.getValue().observacion()));

        tablaPendientes.setPlaceholder(
                new Label("No hay tomas pendientes para esta fecha."));
        fechaPicker.setValue(LocalDate.now());
        horaField.setText("08:00");

        pacienteCombo.valueProperty().addListener((obs, anterior, actual) -> {
            cargarDetalles(actual);
            recargarTabla();
        });
        fechaPicker.valueProperty().addListener((obs, anterior, actual) -> recargarTabla());

        cargarPacientes();
    }

    private void cargarPacientes() {
        try {
            pacienteCombo.setItems(
                    FXCollections.observableArrayList(dao.listarPacientes()));
            if (!pacienteCombo.getItems().isEmpty()) {
                pacienteCombo.getSelectionModel().selectFirst();
            } else {
                mostrar("No hay pacientes registrados. Ejecuta seed-demo.sql.", false);
            }
        } catch (RuntimeException e) {
            mostrar("No se pudo conectar a PostgreSQL: " + raiz(e), false);
        }
    }

    private void cargarDetalles(Paciente paciente) {
        detalleCombo.getItems().clear();
        if (paciente == null) {
            return;
        }
        try {
            detalleCombo.setItems(FXCollections.observableArrayList(
                    dao.listarDetalles(paciente.getId())));
            if (!detalleCombo.getItems().isEmpty()) {
                detalleCombo.getSelectionModel().selectFirst();
            }
        } catch (RuntimeException e) {
            mostrar("No se pudieron cargar los detalles: " + raiz(e), false);
        }
    }

    @FXML
    private void onProgramar() {
        limpiarMensaje();
        Paciente paciente = pacienteCombo.getValue();
        DetalleTratamiento detalle = detalleCombo.getValue();
        LocalDate fecha = fechaPicker.getValue();

        if (paciente == null || detalle == null || fecha == null) {
            mostrar("Seleccione paciente, detalle de tratamiento y fecha.", false);
            return;
        }

        LocalTime hora;
        try {
            hora = LocalTime.parse(horaField.getText().trim(), FORMATO_HORA);
        } catch (DateTimeParseException e) {
            mostrar("Hora invalida. Usa el formato HH:mm, por ejemplo 08:30.", false);
            return;
        }

        try {
            dao.programar(paciente.getId(), detalle.getId(),
                    LocalDateTime.of(fecha, hora), textoOpcional(observacionField.getText()));
            mostrar("Toma programada correctamente.", true);
            recargarTabla();
        } catch (RegistroTomaException e) {
            mostrar(e.getMessage(), false);
        } catch (RuntimeException e) {
            mostrar("No se pudo programar la toma: " + raiz(e), false);
        }
    }

    @FXML
    private void onMarcarRealizada() {
        limpiarMensaje();
        TomaPendienteVista seleccionada =
                tablaPendientes.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrar("Seleccione una toma pendiente de la tabla.", false);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar toma");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("Marcar como realizada la toma de "
                + seleccionada.medicamento() + " programada para el "
                + seleccionada.fechaHoraProgramada().format(FORMATO_FECHA_HORA) + "?");
        confirmacion.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            boolean cambio = dao.marcarRealizada(seleccionada.id(), LocalDateTime.now());
            mostrar(cambio
                    ? "Toma marcada como realizada."
                    : "La toma ya estaba realizada; se conservo la hora real.", true);
            recargarTabla();
        } catch (RegistroTomaException e) {
            mostrar(e.getMessage(), false);
        } catch (RuntimeException e) {
            mostrar("No se pudo marcar la toma: " + raiz(e), false);
        }
    }

    private void recargarTabla() {
        Paciente paciente = pacienteCombo.getValue();
        LocalDate fecha = fechaPicker.getValue();
        if (paciente == null || fecha == null) {
            tablaPendientes.getItems().clear();
            return;
        }
        try {
            tablaPendientes.setItems(FXCollections.observableArrayList(
                    dao.consultarPendientes(paciente.getId(), fecha)));
        } catch (RuntimeException e) {
            tablaPendientes.getItems().clear();
            mostrar("No se pudieron consultar las tomas: " + raiz(e), false);
        }
    }

    private void mostrar(String texto, boolean exito) {
        mensajeLabel.setText(texto);
        mensajeLabel.setTextFill(exito ? Color.DARKGREEN : Color.FIREBRICK);
    }

    private void limpiarMensaje() {
        mensajeLabel.setText("");
    }

    private String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private String raiz(Throwable error) {
        Throwable actual = error;
        while (actual.getCause() != null && actual.getCause() != actual) {
            actual = actual.getCause();
        }
        return actual.getMessage() == null
                ? actual.getClass().getSimpleName()
                : actual.getMessage();
    }
}
