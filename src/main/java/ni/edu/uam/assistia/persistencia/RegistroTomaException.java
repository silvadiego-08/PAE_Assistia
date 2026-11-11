package ni.edu.uam.assistia.persistencia;

/**
 * Error de negocio que la pantalla muestra al usuario.
 */
public class RegistroTomaException extends RuntimeException {

    public RegistroTomaException(String mensaje) {
        super(mensaje);
    }

    public RegistroTomaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
