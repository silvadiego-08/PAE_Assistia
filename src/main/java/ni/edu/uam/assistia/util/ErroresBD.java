package ni.edu.uam.assistia.util;

import ni.edu.uam.assistia.exception.AppException;
import ni.edu.uam.assistia.exception.PersistenciaException;
import ni.edu.uam.assistia.exception.RegistroDuplicadoException;
import ni.edu.uam.assistia.exception.RegistroEnUsoException;
import ni.edu.uam.assistia.exception.ValidacionException;
import org.hibernate.exception.JDBCConnectionException;

import java.sql.SQLException;

/** Convierte excepciones de Hibernate/JDBC en excepciones de la aplicación usando el SQLSTATE de PostgreSQL. */
public final class ErroresBD {

    private static final String MSG_CONEXION = "No se pudo conectar con la base de datos. "
            + "Verifique que PostgreSQL esté en ejecución y que DB_URL, DB_USER y DB_PASSWORD sean correctos.";

    private ErroresBD() { }

    public static AppException traducir(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof AppException app) {
                return app;
            }
            if (t instanceof JDBCConnectionException) {
                return new PersistenciaException(MSG_CONEXION, error);
            }
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                String estado = sql.getSQLState();
                switch (estado) {
                    case "23503":
                        return new RegistroEnUsoException("El registro está referenciado por otros datos.", error);
                    case "23505":
                        return new RegistroDuplicadoException("Ya existe un registro con esos datos.", error);
                    case "23502":
                        return new ValidacionException("Falta un dato obligatorio.");
                    default:
                        if (estado.startsWith("08")) {
                            return new PersistenciaException(MSG_CONEXION, error);
                        }
                }
            }
        }
        return new PersistenciaException("Ocurrió un error al acceder a la base de datos.", error);
    }
}
