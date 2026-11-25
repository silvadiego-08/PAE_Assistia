package ni.edu.uam.assistia.reporte;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JRException;

import java.awt.image.BufferedImage;

public class VistaPreviaReporte {

    private int paginaActual = 0;
    private double zoom = 1.0;

    public void mostrar(JasperPrint reporte, Window propietario) {

        paginaActual = 0;
        zoom = 1.0;

        Stage ventana = new Stage();

        ventana.setTitle("Assistia - Vista previa del expediente");
        ventana.initOwner(propietario);
        ventana.initModality(Modality.WINDOW_MODAL);

        if (reporte == null || reporte.getPages().isEmpty()) {
            Alert alerta = new Alert(
                    Alert.AlertType.WARNING,
                    "El expediente no contiene páginas."
            );
            alerta.initOwner(propietario);
            alerta.showAndWait();
            return;
        }

        ImageView imagen = new ImageView();
        imagen.setPreserveRatio(true);
        imagen.setSmooth(true);

        Label indicador = new Label();

        Button anterior = new Button("Anterior");
        Button siguiente = new Button("Siguiente");

        Button reducir = new Button("-");
        Button ampliar = new Button("+");

        Label indicadorZoom = new Label("100%");

        Button cerrar = new Button("Cerrar");

        int totalPaginas = reporte.getPages().size();

        Runnable actualizarPagina = () -> {

            try {
                BufferedImage pagina = (BufferedImage)
                        JasperPrintManager.printPageToImage(
                                reporte,
                                paginaActual,
                                1.5f
                        );

                imagen.setImage(
                        SwingFXUtils.toFXImage(pagina, null)
                );

                imagen.setFitWidth(
                        reporte.getPageWidth() * zoom
                );

                indicador.setText(
                        "Página " + (paginaActual + 1)
                                + " de " + totalPaginas
                );

                indicadorZoom.setText(
                        Math.round(zoom * 100) + "%"
                );

                anterior.setDisable(paginaActual == 0);

                siguiente.setDisable(
                        paginaActual == totalPaginas - 1
                );

            } catch (JRException error) {

                Alert alerta = new Alert(
                        Alert.AlertType.ERROR,
                        "No se pudo visualizar el expediente: "
                                + error.getMessage()
                );

                alerta.initOwner(ventana);
                alerta.showAndWait();
            }
        };

        anterior.setOnAction(evento -> {
            if (paginaActual > 0) {
                paginaActual--;
                actualizarPagina.run();
            }
        });

        siguiente.setOnAction(evento -> {
            if (paginaActual < totalPaginas - 1) {
                paginaActual++;
                actualizarPagina.run();
            }
        });

        reducir.setOnAction(evento -> {
            zoom = Math.max(0.5, zoom - 0.1);

            imagen.setFitWidth(
                    reporte.getPageWidth() * zoom
            );

            indicadorZoom.setText(
                    Math.round(zoom * 100) + "%"
            );
        });

        ampliar.setOnAction(evento -> {
            zoom = Math.min(2.0, zoom + 0.1);

            imagen.setFitWidth(
                    reporte.getPageWidth() * zoom
            );

            indicadorZoom.setText(
                    Math.round(zoom * 100) + "%"
            );
        });

        cerrar.setOnAction(evento -> ventana.close());

        HBox herramientas = new HBox(
                12,
                anterior,
                indicador,
                siguiente,
                reducir,
                indicadorZoom,
                ampliar
        );

        herramientas.setAlignment(Pos.CENTER);
        herramientas.setPadding(new Insets(15));

        herramientas.setStyle("""
                -fx-background-color: #dcefe7;
                """);

        VBox hoja = new VBox(imagen);
        hoja.setAlignment(Pos.TOP_CENTER);
        hoja.setPadding(new Insets(25));

        hoja.setStyle("""
                -fx-background-color: #dfe5e2;
                """);

        ScrollPane desplazamiento = new ScrollPane(hoja);

        desplazamiento.setFitToWidth(true);
        desplazamiento.setPannable(true);

        HBox acciones = new HBox(cerrar);

        acciones.setAlignment(Pos.CENTER_RIGHT);
        acciones.setPadding(new Insets(15));

        acciones.setStyle("""
                -fx-background-color: #dcefe7;
                """);

        BorderPane contenedor = new BorderPane();

        contenedor.setTop(herramientas);
        contenedor.setCenter(desplazamiento);
        contenedor.setBottom(acciones);

        ventana.setScene(
                new Scene(contenedor, 1000, 750)
        );

        ventana.setMinWidth(700);
        ventana.setMinHeight(500);

        ventana.show();

        actualizarPagina.run();
    }
}