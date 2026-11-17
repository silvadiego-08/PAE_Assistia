package ni.edu.uam.assistia.util;

import ni.edu.uam.assistia.exception.AppException;
import ni.edu.uam.assistia.exception.RegistroDuplicadoException;
import ni.edu.uam.assistia.exception.RegistroEnUsoException;
import ni.edu.uam.assistia.exception.ValidacionException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Diálogos simples. Nunca muestran stack traces al usuario. */
public final class Alertas {

    private Alertas() { }

    public static void info(String mensaje) { emitir(Alert.AlertType.INFORMATION, "Información", mensaje); }

    public static void advertencia(String mensaje) { emitir(Alert.AlertType.WARNING, "Atención", mensaje); }

    public static void error(String mensaje) { emitir(Alert.AlertType.ERROR, "Error", mensaje); }

    public static boolean confirmar(String mensaje) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        a.setTitle("Confirmar");
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    /** Decide el tipo de diálogo según la excepción. */
    public static void mostrar(Throwable t) {
        if (t instanceof ValidacionException
                || t instanceof RegistroEnUsoException
                || t instanceof RegistroDuplicadoException) {
            advertencia(t.getMessage());
        } else if (t instanceof AppException) {
            if (t.getCause() != null) t.printStackTrace();
            error(t.getMessage());
        } else {
            t.printStackTrace();
            error("Ocurrió un error inesperado. Intente de nuevo y, si persiste, avise al equipo.");
        }
    }

    private static void emitir(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert a = new Alert(tipo, mensaje);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
