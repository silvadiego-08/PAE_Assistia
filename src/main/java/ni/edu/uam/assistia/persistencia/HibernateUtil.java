package ni.edu.uam.assistia.persistencia;

import ni.edu.uam.assistia.modelo.DetalleTratamiento;
import ni.edu.uam.assistia.modelo.Medicamento;
import ni.edu.uam.assistia.modelo.Paciente;
import ni.edu.uam.assistia.modelo.RegistroToma;
import ni.edu.uam.assistia.modelo.Tratamiento;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Construye el SessionFactory una sola vez.
 * La conexión se configura con variables de entorno para no guardar
 * credenciales en el repositorio.
 */
public final class HibernateUtil {

    private static volatile SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static SessionFactory getSessionFactory() {
        SessionFactory actual = sessionFactory;
        if (actual == null) {
            synchronized (HibernateUtil.class) {
                actual = sessionFactory;
                if (actual == null) {
                    actual = construir();
                    sessionFactory = actual;
                }
            }
        }
        return actual;
    }

    private static SessionFactory construir() {
        Map<String, Object> ajustes = new HashMap<>();
        ajustes.put(Environment.DRIVER, "org.postgresql.Driver");
        ajustes.put(Environment.URL,
                valor("ASSISTIA_DB_URL", "jdbc:postgresql://localhost:5432/assistia"));
        ajustes.put(Environment.USER, valor("ASSISTIA_DB_USER", "postgres"));
        ajustes.put(Environment.PASS, obligatorio("ASSISTIA_DB_PASSWORD"));
        ajustes.put(Environment.SHOW_SQL, valor("ASSISTIA_DB_SHOW_SQL", "false"));
        ajustes.put(Environment.HBM2DDL_AUTO, valor("ASSISTIA_DB_DDL", "validate"));

        StandardServiceRegistry registro = new StandardServiceRegistryBuilder()
                .applySettings(ajustes)
                .build();
        try {
            Metadata metadata = new MetadataSources(registro)
                    .addAnnotatedClass(Paciente.class)
                    .addAnnotatedClass(Medicamento.class)
                    .addAnnotatedClass(Tratamiento.class)
                    .addAnnotatedClass(DetalleTratamiento.class)
                    .addAnnotatedClass(RegistroToma.class)
                    .buildMetadata();
            return metadata.buildSessionFactory();
        } catch (RuntimeException e) {
            StandardServiceRegistryBuilder.destroy(registro);
            throw new IllegalStateException(
                    "No se pudo iniciar Hibernate. Revisa ASSISTIA_DB_* y ejecuta schema.sql.", e);
        }
    }

    private static String valor(String clave, String porDefecto) {
        String v = System.getenv(clave);
        return (v == null || v.isBlank()) ? porDefecto : v;
    }

    private static String obligatorio(String clave) {
        String v = System.getenv(clave);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + clave + ".");
        }
        return v;
    }

    public static void shutdown() {
        SessionFactory actual = sessionFactory;
        if (actual != null) {
            actual.close();
            sessionFactory = null;
        }
    }
}
