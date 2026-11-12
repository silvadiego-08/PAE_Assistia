package ni.edu.uam.assistia.exception;

public class PersistenciaException extends AppException {
    public PersistenciaException(String mensaje) { super(mensaje); }
    public PersistenciaException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
