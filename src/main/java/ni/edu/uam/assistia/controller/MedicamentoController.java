package ni.edu.uam.assistia.controller;

import ni.edu.uam.assistia.model.Medicamento;
import ni.edu.uam.assistia.rxnorm.RxNormClient;
import ni.edu.uam.assistia.rxnorm.RxNormResultado;
import ni.edu.uam.assistia.service.MedicamentoService;
import ni.edu.uam.assistia.util.Alertas;
import ni.edu.uam.assistia.util.Async;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;

public class MedicamentoController {

    @FXML private TextField txtNombre, txtPrincipioActivo, txtPresentacion, txtConcentracion;
    @FXML private TextArea txtDescripcion;
    @FXML private TableView<Medicamento> tabla;
    @FXML private TableColumn<Medicamento, String> colNombre, colPrincipio, colPresentacion, colConcentracion;
    @FXML private HBox barraAcciones;

    @FXML private TextField txtBuscar;
    @FXML private Button btnBuscar, btnUsarResultado;
    @FXML private Label lblEstadoBusqueda;
    @FXML private TableView<RxNormResultado> tablaResultados;
    @FXML private TableColumn<RxNormResultado, String> colResultadoNombre, colResultadoDosis;
    @FXML private HBox barraBusqueda;
    @FXML private TitledPane panelBusqueda;

    private final MedicamentoService service = new MedicamentoService();
    private final RxNormClient rxNorm = new RxNormClient();
    private final Async async;
    private final Async asyncRed;

    public MedicamentoController(Async async) {
        this(async, async);
    }

    public MedicamentoController(Async async, Async asyncRed) {
        this.async = async;
        this.asyncRed = asyncRed;
    }

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNombre()));
        colPrincipio.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPrincipioActivo()));
        colPresentacion.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPresentacion()));
        colConcentracion.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getConcentracion()));

        colResultadoNombre.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().nombre()));
        colResultadoDosis.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().dosis()));
        tablaResultados.setPlaceholder(new Label("Los resultados de RxNorm aparecerán aquí."));
        btnUsarResultado.disableProperty().bind(
                tablaResultados.getSelectionModel().selectedItemProperty().isNull());
        lblEstadoBusqueda.setText("Escribe el nombre y presiona «Buscar en línea».");

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, sel) -> {
            if (sel != null) {
                txtNombre.setText(sel.getNombre());
                txtPrincipioActivo.setText(sel.getPrincipioActivo());
                txtPresentacion.setText(sel.getPresentacion());
                txtConcentracion.setText(sel.getConcentracion());
                txtDescripcion.setText(sel.getDescripcion());
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
               presentacion = txtPresentacion.getText(), concentracion = txtConcentracion.getText(),
               descripcion = txtDescripcion.getText();
        async.accion(
                () -> service.registrar(nombre, principio, presentacion, concentracion, descripcion),
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
               presentacion = txtPresentacion.getText(), concentracion = txtConcentracion.getText(),
               descripcion = txtDescripcion.getText();
        async.accion(
                () -> service.actualizar(id, nombre, principio, presentacion, concentracion, descripcion),
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
        async.accion(
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

    @FXML
    private void onBuscar() {
        String termino = txtBuscar.getText();
        if (termino == null || termino.isBlank()) {
            Alertas.advertencia("Escribe el nombre de un medicamento antes de buscar.");
            txtBuscar.requestFocus();
            return;
        }

        tablaResultados.getItems().clear();
        tablaResultados.getSelectionModel().clearSelection();
        lblEstadoBusqueda.setText("Buscando…");

        asyncRed.consultar(
                () -> rxNorm.buscar(termino),
                resultados -> {
                    tablaResultados.setItems(FXCollections.observableArrayList(resultados));
                    if (resultados.isEmpty()) {
                        lblEstadoBusqueda.setText("No se encontraron resultados para «"
                                + termino.trim() + "».");
                    } else {
                        lblEstadoBusqueda.setText(resultados.size()
                                + " resultado(s). Selecciona uno y presiona «Usar resultado».");
                    }
                },
                error -> {
                    tablaResultados.getItems().clear();
                    lblEstadoBusqueda.setText("No se pudo consultar RxNorm. "
                            + "Revisa tu conexión e inténtalo de nuevo.");
                    error.printStackTrace();
                },
                barraBusqueda);
    }

    @FXML
    private void onUsarResultado() {
        RxNormResultado seleccionado = tablaResultados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alertas.advertencia("Selecciona un resultado de RxNorm.");
            return;
        }

        // Es un registro nuevo: se quita cualquier selección del catálogo.
        tabla.getSelectionModel().clearSelection();
        txtNombre.setText(seleccionado.nombre());
        txtPrincipioActivo.setText(seleccionado.nombre());
        txtConcentracion.setText(seleccionado.dosis());
        txtDescripcion.setText(seleccionado.descripcion());
        txtPresentacion.clear();
        lblEstadoBusqueda.setText("Datos copiados. Revisa la presentación y presiona «Guardar».");
        panelBusqueda.setExpanded(false);
        txtPresentacion.requestFocus();
    }

    private void limpiar() {
        tabla.getSelectionModel().clearSelection();
        txtNombre.clear();
        txtPrincipioActivo.clear();
        txtPresentacion.clear();
        txtConcentracion.clear();
        txtDescripcion.clear();
        txtNombre.requestFocus();
    }

    private void recargar() {
        async.consultar(
                service::listar,
                lista -> tabla.setItems(FXCollections.observableArrayList(lista)),
                Alertas::mostrar, barraAcciones);
    }
}
