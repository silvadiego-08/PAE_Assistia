
package ni.edu.uam.assistia.reporte;

public class ReporteException extends RuntimeException {

    public ReporteException(String mensaje) {
        super(mensaje);
    }

    public ReporteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
