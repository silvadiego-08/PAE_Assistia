package ni.edu.uam.assistia.controller;

import ni.edu.uam.assistia.model.Medicamento;
import ni.edu.uam.assistia.service.MedicamentoService;
import ni.edu.uam.assistia.util.Alertas;
import ni.edu.uam.assistia.util.Async;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class MedicamentoController {

    @FXML private TextField txtNombre, txtPrincipioActivo, txtPresentacion, txtConcentracion;
    @FXML private TableView<Medicamento> tabla;
    @FXML private TableColumn<Medicamento, String> colNombre, colPrincipio, colPresentacion, colConcentracion;
    @FXML private HBox barraAcciones;

    private final MedicamentoService service = new MedicamentoService();

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNombre()));
        colPrincipio.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPrincipioActivo()));
        colPresentacion.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPresentacion()));
        colConcentracion.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getConcentracion()));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, sel) -> {
            if (sel != null) {
                txtNombre.setText(sel.getNombre());
                txtPrincipioActivo.setText(sel.getPrincipioActivo());
                txtPresentacion.setText(sel.getPresentacion());
                txtConcentracion.setText(sel.getConcentracion());
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
        String nombre = txtNombre.getText(), principio = txtPrincipioActivo.getText(),
               presentacion = txtPresentacion.getText(), concentracion = txtConcentracion.getText();
        Async.accion(
                () -> service.registrar(nombre, principio, presentacion, concentracion),
                () -> {
                    limpiar();
                    recargar();
                    Alertas.info("Medicamento guardado correctamente.");
                },
                Alertas::mostrar, barraAcciones);
    }

    @FXML
    private void onActualizar() {
        Medicamento sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alertas.advertencia("Seleccione un medicamento de la tabla antes de actualizar.");
            return;
        }
        Long id = sel.getId();
        String nombre = txtNombre.getText(), principio = txtPrincipioActivo.getText(),
               presentacion = txtPresentacion.getText(), concentracion = txtConcentracion.getText();
        Async.accion(
                () -> service.actualizar(id, nombre, principio, presentacion, concentracion),
                () -> {
                    limpiar();
                    recargar();
                    Alertas.info("Medicamento actualizado correctamente.");
                },
                Alertas::mostrar, barraAcciones);
    }

    @FXML
    private void onEliminar() {
        Medicamento sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alertas.advertencia("Seleccione un medicamento de la tabla antes de eliminar.");
            return;
        }
        if (!Alertas.confirmar("¿Eliminar el medicamento «" + sel.getNombre() + "»?")) {
            return;
        }
        Async.accion(
                () -> service.eliminar(sel),
                () -> {
                    limpiar();
                    recargar();
                    Alertas.info("Medicamento eliminado.");
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
        txtPrincipioActivo.clear();
        txtPresentacion.clear();
        txtConcentracion.clear();
        txtNombre.requestFocus();
    }

    private void recargar() {
        Async.consultar(
                service::listar,
                lista -> tabla.setItems(FXCollections.observableArrayList(lista)),
                Alertas::mostrar, barraAcciones);
    }
}
