
package ni.edu.uam.assistia;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                Launcher.class.getResource("tratamiento-view.fxml")
        );

        Scene scene = new Scene(loader.load());

        stage.setTitle("PAE Assistia - Tratamientos");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
