package ni.edu.uam.assistia.util;

import ni.edu.uam.assistia.exception.AppException;
import ni.edu.uam.assistia.exception.PersistenciaException;
import ni.edu.uam.assistia.model.Medicamento;
import ni.edu.uam.assistia.model.Medico;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.List;

/**
 * Única fuente de SessionFactory de la aplicación.
 * Lee DB_URL, DB_USER y DB_PASSWORD desde variables de entorno.
 */
public final class HibernateUtil {

    /**
     * REGISTRO DE ENTIDADES: cada módulo agrega aquí su clase, una por línea.
     * Si hay conflicto de merge en esta lista, conserven las líneas de ambos lados.
     */
    private static final List<Class<?>> ENTIDADES = List.of(
            Medicamento.class,
            Medico.class
            // , Paciente.class
            // , Prescripcion.class
    );

    private static SessionFactory sessionFactory;

    private HibernateUtil() { }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = construir();
        }
        return sessionFactory;
    }

    /** Se llama una sola vez al salir (App.stop). */
    public static synchronized void cerrar() {
        if (sessionFactory != null && sessionFactory.isOpen()) {
            sessionFactory.close();
        }
        sessionFactory = null;
    }

    /** Consulta simple para comprobar la conexión; devuelve la versión de PostgreSQL. */
    public static String versionBD() {
        return Tx.leer(s -> s.createNativeQuery("select version()", String.class).getSingleResult());
    }

    private static SessionFactory construir() {
        try {
            Configuration cfg = new Configuration();
            cfg.setProperty("jakarta.persistence.jdbc.url", requerida("DB_URL"));
            cfg.setProperty("jakarta.persistence.jdbc.user", requerida("DB_USER"));
            cfg.setProperty("jakarta.persistence.jdbc.password", requerida("DB_PASSWORD"));
            cfg.setProperty("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
            cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            // update: crea/ajusta tablas en el esquema local. Se puede cambiar con DB_DDL_AUTO.
            cfg.setProperty("hibernate.hbm2ddl.auto", System.getenv().getOrDefault("DB_DDL_AUTO", "update"));
            cfg.setProperty("hibernate.show_sql", "true");
            cfg.setProperty("hibernate.connection.pool_size", "5");

            for (Class<?> entidad : ENTIDADES) {
                cfg.addAnnotatedClass(entidad);
            }
            return cfg.buildSessionFactory();
        } catch (AppException e) {
            throw e;
        } catch (RuntimeException e) {
            e.printStackTrace(); // detalle técnico solo en consola
            throw ErroresBD.traducir(e);
        }
    }

    private static String requerida(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            throw new PersistenciaException("Falta la variable de entorno " + nombre
                    + ". Configúrela en IntelliJ (Run > Edit Configurations > Environment variables).");
        }
        return valor;
    }
}
