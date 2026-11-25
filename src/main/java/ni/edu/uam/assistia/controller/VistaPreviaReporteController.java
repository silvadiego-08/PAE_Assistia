package ni.edu.uam.assistia.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JRException;

import ni.edu.uam.assistia.reporte.ReportePacienteService;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

public class VistaPreviaReporteController {

    @FXML private ImageView imagenReporte;
    @FXML private Label indicadorPagina;
    @FXML private Label indicadorZoom;

    @FXML private Button anterior;
    @FXML private Button siguiente;
    @FXML private Button descargarPdf;
    @FXML private Button descargarExcel;

    private JasperPrint reporte;
    private ReportePacienteService reporteService;
    private Stage ventana;

    private int paginaActual = 0;
    private double zoom = 1.0;

    public void configurar(
            JasperPrint reporte,
            ReportePacienteService reporteService,
            Stage ventana) {

        this.reporte = reporte;
        this.reporteService = reporteService;
        this.ventana = ventana;

        paginaActual = 0;
        zoom = 1.0;

        if (reporte == null || reporte.getPages().isEmpty()) {

            anterior.setDisable(true);
            siguiente.setDisable(true);
            descargarPdf.setDisable(true);
            descargarExcel.setDisable(true);

            indicadorPagina.setText("Sin páginas");

            return;
        }

        actualizarPagina();
    }

    private void actualizarPagina() {

        try {

            BufferedImage pagina = (BufferedImage)
                    JasperPrintManager.printPageToImage(
                            reporte,
                            paginaActual,
                            1.5f
                    );

            imagenReporte.setImage(
                    SwingFXUtils.toFXImage(pagina, null)
            );

            actualizarZoom();

            int totalPaginas = reporte.getPages().size();

            indicadorPagina.setText(
                    "Página " + (paginaActual + 1)
                            + " de " + totalPaginas
            );

            anterior.setDisable(paginaActual == 0);

            siguiente.setDisable(
                    paginaActual == totalPaginas - 1
            );

        } catch (JRException error) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo visualizar el expediente: "
                            + error.getMessage()
            );
        }
    }

    @FXML
    private void paginaAnterior() {

        if (paginaActual > 0) {
            paginaActual--;
            actualizarPagina();
        }
    }

    @FXML
    private void paginaSiguiente() {

        if (reporte != null &&
                paginaActual < reporte.getPages().size() - 1) {

            paginaActual++;
            actualizarPagina();
        }
    }

    @FXML
    private void reducirZoom() {

        zoom = Math.max(0.5, zoom - 0.1);

        actualizarZoom();
    }

    @FXML
    private void ampliarZoom() {

        zoom = Math.min(2.0, zoom + 0.1);

        actualizarZoom();
    }

    private void actualizarZoom() {

        if (reporte == null) {
            return;
        }

        imagenReporte.setFitWidth(
                reporte.getPageWidth() * zoom
        );

        indicadorZoom.setText(
                Math.round(zoom * 100) + "%"
        );
    }

    @FXML
    private void exportarPdf() {
        exportar(false);
    }

    @FXML
    private void exportarExcel() {
        exportar(true);
    }

    private void exportar(boolean excel) {

        if (reporte == null || reporte.getPages().isEmpty()) {
            return;
        }

        String extension = excel ? ".xlsx" : ".pdf";

        FileChooser selector = new FileChooser();

        selector.setTitle(
                excel
                        ? "Guardar expediente Excel"
                        : "Guardar expediente PDF"
        );

        selector.setInitialFileName(
                "expediente_medico" + extension
        );

        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        excel ? "Archivo Excel" : "Archivo PDF",
                        "*" + extension
                )
        );

        File archivo = selector.showSaveDialog(ventana);

        if (archivo == null) {
            return;
        }

        Path destino = archivo.toPath();

        if (!destino.toString()
                .toLowerCase(Locale.ROOT).endsWith(extension)) {

            destino = Path.of(
                    destino.toString() + extension
            );

            if (java.nio.file.Files.exists(destino)) {

                Alert confirmar = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "El archivo ya existe. ¿Desea reemplazarlo?",
                        javafx.scene.control.ButtonType.YES,
                        javafx.scene.control.ButtonType.NO
                );

                confirmar.initOwner(ventana);

                if (confirmar.showAndWait().orElse(
                        javafx.scene.control.ButtonType.NO
                ) != javafx.scene.control.ButtonType.YES) {
                    return;
                }
            }
        }

        try {

            Path resultado;

            if (excel) {

                resultado = reporteService.exportarExcel(
                        reporte,
                        destino
                );

            } else {

                resultado = reporteService.exportarPdf(
                        reporte,
                        destino
                );
            }

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Expediente guardado correctamente en:\n"
                            + resultado
            );

        } catch (Exception error) {

            System.getLogger(
                    VistaPreviaReporteController.class.getName()
            ).log(
                    System.Logger.Level.ERROR,
                    "Error al exportar expediente",
                    error
            );

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo exportar el expediente:\n"
                            + error.getMessage()
            );
        }
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String mensaje) {

        Alert alerta = new Alert(tipo, mensaje);

        alerta.initOwner(ventana);
        alerta.showAndWait();
    }

    @FXML
    private void cerrar() {
        ventana.close();
    }
}