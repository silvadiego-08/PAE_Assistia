package ni.edu.uam.assistia.exception;

/** Se violó una restricción de unicidad. */
public class RegistroDuplicadoException extends AppException {
    public RegistroDuplicadoException(String mensaje) { super(mensaje); }
    public RegistroDuplicadoException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
