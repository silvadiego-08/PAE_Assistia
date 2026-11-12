
package ni.edu.uam.assistia.dao;

import ni.edu.uam.assistia.models.Tratamiento;
import ni.edu.uam.assistia.models.DetalleTratamiento;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

public class TratamientoDAO {

    private static final SessionFactory sessionFactory =
            crearSessionFactory();

    // Configuración de Hibernate
    private static SessionFactory crearSessionFactory() {

        try {
            Properties propiedades = new Properties();

            try (InputStream archivo = TratamientoDAO.class
                    .getClassLoader()
                    .getResourceAsStream("hibernate.properties")) {

                if (archivo == null) {
                    throw new IllegalStateException(
                            "No se encontró Hibernate.properties"
                    );
                }

                propiedades.load(archivo);
            }

            return new Configuration()
                    .addProperties(propiedades)
                    .addAnnotatedClass(Tratamiento.class)
                    .addAnnotatedClass(DetalleTratamiento.class)
                    .buildSessionFactory();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al configurar Hibernate", e
            );
        }
    }

    // Ejecutar operaciones dentro de una transacción
    private void ejecutarTransaccion(
            Consumer<Session> operacion) {

        Transaction transaccion = null;

        try (Session session = sessionFactory.openSession()) {

            transaccion = session.beginTransaction();

            operacion.accept(session);

            transaccion.commit();

        } catch (Exception e) {

            if (transaccion != null &&
                    transaccion.isActive()) {
                transaccion.rollback();
            }

            throw new RuntimeException(
                    "Error en la base de datos: " +
                            e.getMessage(), e
            );
        }
    }

    // CREATE: guardar tratamiento y todos sus detalles
    public void guardar(Tratamiento tratamiento) {

        ejecutarTransaccion(session -> {
            session.persist(tratamiento);
            session.flush();
        });
    }

    // READ: listar tratamientos con sus detalles
    public List<Tratamiento> listar() {

        try (Session session = sessionFactory.openSession()) {

            return session.createQuery(
                    """
                    SELECT DISTINCT t
                    FROM Tratamiento t
                    LEFT JOIN FETCH t.detalles
                    ORDER BY t.id DESC
                    """,
                    Tratamiento.class
            ).getResultList();
        }
    }

    // READ: buscar tratamiento por ID
    public Tratamiento buscarPorId(Long id) {

        try (Session session = sessionFactory.openSession()) {

            return session.createQuery(
                            """
                            SELECT DISTINCT t
                            FROM Tratamiento t
                            LEFT JOIN FETCH t.detalles
                            WHERE t.id = :id
                            """,
                            Tratamiento.class
                    )
                    .setParameter("id", id)
                    .uniqueResult();
        }
    }

    // UPDATE: actualizar los datos generales
    public void actualizar(Long id, Tratamiento datos) {

        ejecutarTransaccion(session -> {

            Tratamiento existente =
                    session.get(Tratamiento.class, id);

            if (existente == null) {
                throw new IllegalArgumentException(
                        "Tratamiento no encontrado"
                );
            }

            existente.setPacienteId(datos.getPacienteId());
            existente.setMedicoId(datos.getMedicoId());
            existente.setFechaInicio(datos.getFechaInicio());
            existente.setFechaFin(datos.getFechaFin());
            existente.setActivo(datos.isActivo());
        });
    }

    // Agregar un detalle a un tratamiento existente
    public void agregarDetalle(
            Long tratamientoId,
            DetalleTratamiento detalle) {

        ejecutarTransaccion(session -> {

            Tratamiento tratamiento =
                    session.get(Tratamiento.class, tratamientoId);

            if (tratamiento == null) {
                throw new IllegalArgumentException(
                        "Tratamiento no encontrado"
                );
            }

            detalle.setTratamiento(tratamiento);
            session.persist(detalle);
        });
    }

    // Desactivar sin eliminar el historial
    public void desactivar(Long id) {

        ejecutarTransaccion(session -> {

            Tratamiento tratamiento =
                    session.get(Tratamiento.class, id);

            if (tratamiento == null) {
                throw new IllegalArgumentException(
                        "Tratamiento no encontrado"
                );
            }

            tratamiento.setActivo(false);
        });
    }

    // CONSULTA HQL 1:
    // Medicamentos de tratamientos activos de un paciente
    public List<DetalleTratamiento> medicamentosActivos(
            Long pacienteId) {

        try (Session session = sessionFactory.openSession()) {

            return session.createQuery(
                            """
                            SELECT d
                            FROM DetalleTratamiento d
                            JOIN FETCH d.tratamiento t
                            WHERE t.pacienteId = :pacienteId
                            AND t.activo = true
                            ORDER BY d.medicamentoId
                            """,
                            DetalleTratamiento.class
                    )
                    .setParameter("pacienteId", pacienteId)
                    .getResultList();
        }
    }

    // CONSULTA HQL 2:
    // Listar tratamientos de un paciente
    public List<Tratamiento> listarPorPaciente(
            Long pacienteId) {

        try (Session session = sessionFactory.openSession()) {

            return session.createQuery(
                            """
                            SELECT DISTINCT t
                            FROM Tratamiento t
                            LEFT JOIN FETCH t.detalles
                            WHERE t.pacienteId = :pacienteId
                            ORDER BY t.fechaInicio DESC
                            """,
                            Tratamiento.class
                    )
                    .setParameter("pacienteId", pacienteId)
                    .getResultList();
        }
    }

    // Cerrar Hibernate al finalizar la aplicación
    public static void cerrar() {

        if (!sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}
