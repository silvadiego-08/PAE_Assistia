package ni.edu.uam.assistia.exception;

/** El registro está referenciado por otras tablas (violación de llave foránea). */
public class RegistroEnUsoException extends AppException {
    public RegistroEnUsoException(String mensaje) { super(mensaje); }
    public RegistroEnUsoException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
