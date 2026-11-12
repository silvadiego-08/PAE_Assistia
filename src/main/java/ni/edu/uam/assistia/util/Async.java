package ni.edu.uam.assistia.util;

import javafx.concurrent.Task;
import javafx.scene.Node;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ejecuta acceso a datos fuera del hilo de JavaFX.
 * Los callbacks alExito y alError se ejecutan en el hilo de JavaFX, por lo que pueden tocar la interfaz.
 */
public final class Async {

    private Async() { }

    public static <T> void consultar(Supplier<T> trabajo, Consumer<T> alExito,
                                     Consumer<Throwable> alError, Node... bloquear) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() {
                return trabajo.get();
            }
        };
        cambiar(bloquear, true);
        tarea.setOnSucceeded(e -> {
            cambiar(bloquear, false);
            alExito.accept(tarea.getValue());
        });
        tarea.setOnFailed(e -> {
            cambiar(bloquear, false);
            alError.accept(tarea.getException());
        });
        Thread hilo = new Thread(tarea, "acceso-datos");
        hilo.setDaemon(true);
        hilo.start();
    }

    public static void accion(Runnable trabajo, Runnable alExito,
                              Consumer<Throwable> alError, Node... bloquear) {
        consultar(() -> {
            trabajo.run();
            return null;
        }, resultado -> alExito.run(), alError, bloquear);
    }

    private static void cambiar(Node[] nodos, boolean deshabilitar) {
        for (Node n : nodos) {
            n.setDisable(deshabilitar);
        }
    }
}
