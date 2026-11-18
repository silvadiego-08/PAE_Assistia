package ni.edu.uam.assistia;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ni.edu.uam.assistia.controller.CuidadorController;
import ni.edu.uam.assistia.controller.PacienteController;
import ni.edu.uam.assistia.controller.MedicamentoController;
import ni.edu.uam.assistia.controller.MedicoController;
import ni.edu.uam.assistia.persistence.CuidadorDao;
import ni.edu.uam.assistia.persistence.HibernateConfig;
import ni.edu.uam.assistia.persistence.PacienteDao;
import ni.edu.uam.assistia.util.Async;
import ni.edu.uam.assistia.util.Alertas;

public class App extends Application {
    private final HibernateConfig hibernate = HibernateConfig.compartida();
    private final ExecutorService baseDeDatos = Executors.newSingleThreadExecutor();

    @FXML private StackPane areaPrincipal;
    @FXML private ScrollPane inicio;
    @FXML private ToggleButton navInicio;
    @FXML private ToggleButton navPacientes;
    @FXML private ToggleButton navCuidadores;
    @FXML private ToggleButton navMedicamentos;
    @FXML private ToggleButton navMedicos;
    @FXML private Button abrirPacientes;
    @FXML private Button abrirCuidadores;
    @FXML private Button abrirMedicamentos;
    @FXML private Button abrirMedicos;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader principal = cargar("principal.fxml");
        principal.setController(this);
        Scene escena = new Scene(principal.load(), 1180, 780);
        escena.getStylesheets().add(getClass().getResource("css/assistia.css").toExternalForm());

        CuidadorDao cuidadorDao = new CuidadorDao(hibernate);
        FXMLLoader cuidadores = cargar("cuidadores.fxml");
        cuidadores.setController(new CuidadorController(cuidadorDao, baseDeDatos));
        FXMLLoader pacientes = cargar("pacientes.fxml");
        pacientes.setController(new PacienteController(new PacienteDao(hibernate), cuidadorDao, baseDeDatos));

        Async async = new Async(baseDeDatos);
        FXMLLoader medicamentos = cargar("medicamentos.fxml");
        medicamentos.setControllerFactory(tipo -> new MedicamentoController(async));
        FXMLLoader medicos = cargar("medicos.fxml");
        medicos.setControllerFactory(tipo -> new MedicoController(async));

        navInicio.setOnAction(evento -> mostrar(navInicio, inicio));
        conectar(navPacientes, abrirPacientes, pacientes);
        conectar(navCuidadores, abrirCuidadores, cuidadores);
        conectar(navMedicamentos, abrirMedicamentos, medicamentos);
        conectar(navMedicos, abrirMedicos, medicos);

        stage.setScene(escena);
        stage.setMinWidth(920);
        stage.setMinHeight(580);
        stage.setTitle("Assistia");
        stage.show();
        navInicio.requestFocus();
    }

    private FXMLLoader cargar(String archivo) {
        return new FXMLLoader(getClass().getResource("fxml/" + archivo));
    }

    private void conectar(ToggleButton enlace, Button tarjeta, FXMLLoader vista) {
        ScrollPane pagina = new ScrollPane();
        pagina.setFitToWidth(true);
        pagina.setFitToHeight(true);
        pagina.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pagina.setAccessibleText(enlace.getText());
        enlace.setOnAction(evento -> {
            try {
                if (pagina.getContent() == null) {
                    pagina.setContent(vista.load());
                }
                mostrar(enlace, pagina);
            } catch (IOException error) {
                mostrar(navInicio, inicio);
                Alertas.advertencia("No se pudo abrir la pantalla de " + enlace.getText() + ".");
                System.getLogger(App.class.getName()).log(System.Logger.Level.ERROR,
                        "No se pudo cargar la pantalla de " + enlace.getText(), error);
            }
        });
        tarjeta.setOnAction(evento -> enlace.fire());
    }

    private void mostrar(ToggleButton enlace, Node pagina) {
        enlace.setSelected(true);
        areaPrincipal.getChildren().setAll(pagina);
        enlace.requestFocus();
    }

    @Override
    public void stop() {
        baseDeDatos.execute(hibernate::close);
        baseDeDatos.shutdown();
    }
}
