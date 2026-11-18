package ni.edu.uam.assistia.rxnorm;

import ni.edu.uam.assistia.exception.AppException;

/** Error controlado al consultar el servicio externo RxNorm. */
public class RxNormException extends AppException {

    public RxNormException(String mensaje) {
        super(mensaje);
    }

    public RxNormException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
