package ni.edu.uam.assistia.persistence;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Properties;
import ni.edu.uam.assistia.model.AsignacionCuidador;
import ni.edu.uam.assistia.model.Cuidador;
import ni.edu.uam.assistia.model.Paciente;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateConfig implements AutoCloseable {
    private SessionFactory factory;

    public synchronized SessionFactory sessionFactory() {
        if (factory == null) {
            Properties properties = cargarConfiguracion();
            factory = new Configuration()
                    .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                    .setProperty("hibernate.connection.url", valor("DB_URL", properties))
                    .setProperty("hibernate.connection.username", valor("DB_USER", properties))
                    .setProperty("hibernate.connection.password", valor("DB_PASSWORD", properties))
                    .setProperty("hibernate.hbm2ddl.auto", "update")
                    .setProperty("hibernate.hbm2ddl.halt_on_error", "true")
                    .addAnnotatedClass(Paciente.class)
                    .addAnnotatedClass(Cuidador.class)
                    .addAnnotatedClass(AsignacionCuidador.class)
                    .buildSessionFactory();
        }
        return factory;
    }

    private static Properties cargarConfiguracion() {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("db.properties"))) {
            properties.load(reader);
        } catch (NoSuchFileException ignored) {
            // Las variables de entorno también pueden proporcionar toda la configuración.
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("No se pudo leer db.properties. Revisa el archivo local.", error);
        }
        return properties;
    }

    private static String valor(String name, Properties properties) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = properties.getProperty(name);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar " + name + " en db.properties o en el entorno.");
        }
        return value;
    }

    @Override
    public synchronized void close() {
        if (factory != null) {
            factory.close();
            factory = null;
        }
    }
}
