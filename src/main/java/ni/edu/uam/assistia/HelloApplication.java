package ni.edu.uam.assistia;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.assistia.persistencia.HibernateUtil;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("registro-toma-view.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 620);
        stage.setTitle("Assistia - Registro de tomas");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        HibernateUtil.shutdown();
    }
}
