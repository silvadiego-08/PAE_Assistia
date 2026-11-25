package ni.edu.uam.assistia.reporte;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import net.sf.jasperreports.engine.JasperPrint;

import ni.edu.uam.assistia.controller.VistaPreviaReporteController;

import java.io.IOException;
import java.net.URL;

public class VistaPreviaReporte {

    public void mostrar(
            JasperPrint reporte,
            Window propietario,
            ReportePacienteService reporteService) {

        try {

            URL recurso = getClass().getResource(
                    "/ni/edu/uam/assistia/fxml/VistaPreviaReporte.fxml"
            );

            if (recurso == null) {
                throw new IOException(
                        "No se encontró el FXML de vista previa."
                );
            }

            FXMLLoader loader = new FXMLLoader(recurso);

            Parent raiz = loader.load();

            Stage ventana = new Stage();

            ventana.setTitle(
                    "Assistia - Vista previa del expediente"
            );

            ventana.initOwner(propietario);
            ventana.initModality(Modality.WINDOW_MODAL);

            ventana.setScene(new Scene(raiz));

            ventana.setMinWidth(700);
            ventana.setMinHeight(500);

            VistaPreviaReporteController controller =
                    loader.getController();

            controller.configurar(
                    reporte,
                    reporteService,
                    ventana
            );

            ventana.show();

        } catch (IOException error) {

            System.getLogger(
                    VistaPreviaReporte.class.getName()
            ).log(
                    System.Logger.Level.ERROR,
                    "No se pudo cargar la vista previa",
                    error
            );

            Alert alerta = new Alert(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir la vista previa del expediente."
            );

            alerta.initOwner(propietario);
            alerta.showAndWait();
        }
    }
}