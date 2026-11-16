package ni.edu.uam.assistia;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ni.edu.uam.assistia.controller.CuidadorController;
import ni.edu.uam.assistia.controller.PacienteController;
import ni.edu.uam.assistia.persistence.CuidadorDao;
import ni.edu.uam.assistia.persistence.HibernateConfig;
import ni.edu.uam.assistia.persistence.PacienteDao;

public class HelloApplication extends Application {
    private final HibernateConfig hibernate = new HibernateConfig();
    private final ExecutorService baseDeDatos = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage stage) throws IOException {
        CuidadorDao cuidadorDao = new CuidadorDao(hibernate);
        FXMLLoader cuidadores = new FXMLLoader(getClass().getResource("cuidadores.fxml"));
        cuidadores.setController(new CuidadorController(cuidadorDao, baseDeDatos));
        FXMLLoader pacientes = new FXMLLoader(getClass().getResource("pacientes.fxml"));
        pacientes.setController(new PacienteController(new PacienteDao(hibernate), cuidadorDao, baseDeDatos));

        TabPane pestañas = new TabPane(new Tab("Pacientes", pacientes.load()), new Tab("Cuidadores", cuidadores.load()));
        pestañas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        stage.setScene(new Scene(pestañas, 740, 660));
        stage.setMinWidth(580);
        stage.setMinHeight(550);
        stage.setTitle("Assistia");
        stage.show();
    }

    @Override
    public void stop() {
        baseDeDatos.execute(hibernate::close);
        baseDeDatos.shutdown();
    }
}
