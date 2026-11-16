package ni.edu.uam.assistia.controller;

import ni.edu.uam.assistia.model.Medico;
import ni.edu.uam.assistia.service.MedicoService;
import ni.edu.uam.assistia.util.Alertas;
import ni.edu.uam.assistia.util.Async;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.Objects;

public class MedicoController {

    @FXML private TextField txtNombre, txtEspecialidad, txtTelefono;
    @FXML private TableView<Medico> tabla;
    @FXML private TableColumn<Medico, String> colNombre, colEspecialidad, colTelefono;
    @FXML private HBox barraAcciones;

    private final MedicoService service = new MedicoService();

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNombre()));
        colEspecialidad.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEspecialidad()));
        colTelefono.setCellValueFactory(d -> new ReadOnlyStringWrapper(Objects.toString(d.getValue().getTelefono(), "")));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, sel) -> {
            if (sel != null) {
                txtNombre.setText(sel.getNombre());
                txtEspecialidad.setText(sel.getEspecialidad());
                txtTelefono.setText(Objects.toString(sel.getTelefono(), ""));
            }
        });
        recargar();
    }

    @FXML
    private void onGuardar() {
        if (tabla.getSelectionModel().getSelectedItem() != null) {
            Alertas.advertencia("Hay un registro seleccionado. Use «Actualizar» o presione «Limpiar» para crear uno nuevo.");
            return;
        }
        String nombre = txtNombre.getText(), especialidad = txtEspecialidad.getText(), telefono = txtTelefono.getText();
        Async.accion(
                () -> service.registrar(nombre, especialidad, telefono),
                () -> {
                    limpiar();
                    recargar();
                    Alertas.info("Médico guardado correctamente.");
                },
                Alertas::mostrar, barraAcciones);
    }

    @FXML
    private void onActualizar() {
        Medico sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alertas.advertencia("Seleccione un médico de la tabla antes de actualizar.");
            return;
        }
        Long id = sel.getId();
        String nombre = txtNombre.getText(), especialidad = txtEspecialidad.getText(), telefono = txtTelefono.getText();
        Async.accion(
                () -> service.actualizar(id, nombre, especialidad, telefono),
                () -> {
                    limpiar();
                    recargar();
                    Alertas.info("Médico actualizado correctamente.");
                },
                Alertas::mostrar, barraAcciones);
    }

    @FXML
    private void onEliminar() {
        Medico sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alertas.advertencia("Seleccione un médico de la tabla antes de eliminar.");
            return;
        }
        if (!Alertas.confirmar("¿Eliminar al médico «" + sel.getNombre() + "»?")) {
            return;
        }
        Async.accion(
                () -> service.eliminar(sel),
                () -> {
                    limpiar();
                    recargar();
                    Alertas.info("Médico eliminado.");
                },
                Alertas::mostrar, barraAcciones);
    }

    @FXML
    private void onLimpiar() {
        limpiar();
    }

    private void limpiar() {
        tabla.getSelectionModel().clearSelection();
        txtNombre.clear();
        txtEspecialidad.clear();
        txtTelefono.clear();
        txtNombre.requestFocus();
    }

    private void recargar() {
        Async.consultar(
                service::listar,
                lista -> tabla.setItems(FXCollections.observableArrayList(lista)),
                Alertas::mostrar, barraAcciones);
    }
}
