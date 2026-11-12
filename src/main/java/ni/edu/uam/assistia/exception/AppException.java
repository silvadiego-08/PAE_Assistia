package ni.edu.uam.assistia.exception;

/** Base de los errores "esperados" de la aplicación, con mensajes aptos para mostrar al usuario. */
public class AppException extends RuntimeException {
    public AppException(String mensaje) { super(mensaje); }
    public AppException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
