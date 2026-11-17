
package ni.edu.uam.assistia.dao;

import ni.edu.uam.assistia.model.DetalleTratamiento;
import ni.edu.uam.assistia.model.Medicamento;
import ni.edu.uam.assistia.model.Medico;
import ni.edu.uam.assistia.model.Paciente;
import ni.edu.uam.assistia.model.Tratamiento;
import ni.edu.uam.assistia.persistence.HibernateConfig;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.function.Consumer;

public class TratamientoDAO {

    private SessionFactory sessionFactory() {
        return HibernateConfig.compartida().sessionFactory();
    }

    private void ejecutarTransaccion(Consumer<Session> operacion) {
        try (Session session = sessionFactory().openSession()) {
            Transaction transaccion = session.beginTransaction();

            try {
                operacion.accept(session);
                transaccion.commit();
            } catch (Exception e) {
                if (transaccion.isActive()) {
                    transaccion.rollback();
                }
                throw e;
            }
        }
    }

    // Comprobar que la entidad existe y obtenerla en la sesión actual
    private <T> T obtener(Session session, Class<T> tipo,
                          Long id, String nombre) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un " + nombre + " válido.");
        }

        T entidad = session.find(tipo, id);

        if (entidad == null) {
            throw new IllegalArgumentException(
                    "El " + nombre + " seleccionado ya no existe.");
        }

        return entidad;
    }

    // CREATE: guardar tratamiento y detalles en una transacción
    public void guardar(Tratamiento tratamiento) {
        ejecutarTransaccion(session -> {

            tratamiento.setPaciente(obtener(
                    session, Paciente.class,
                    tratamiento.getPaciente().getId(), "paciente"));

            tratamiento.setMedico(obtener(
                    session, Medico.class,
                    tratamiento.getMedico().getId(), "médico"));

            for (DetalleTratamiento detalle : tratamiento.getDetalles()) {
                detalle.setMedicamento(obtener(
                        session, Medicamento.class,
                        detalle.getMedicamento().getId(), "medicamento"));

                detalle.setTratamiento(tratamiento);
            }

            session.persist(tratamiento);
            session.flush();
        });
    }

    // READ: listar tratamientos con sus relaciones
    public List<Tratamiento> listar() {
        try (Session session = sessionFactory().openSession()) {
            return session.createQuery("""
                    SELECT DISTINCT t
                    FROM Tratamiento t
                    JOIN FETCH t.paciente
                    JOIN FETCH t.medico
                    LEFT JOIN FETCH t.detalles d
                    LEFT JOIN FETCH d.medicamento
                    ORDER BY t.id DESC
                    """, Tratamiento.class).getResultList();
        }
    }

    // READ: buscar tratamiento por ID
    public Tratamiento buscarPorId(Long id) {
        try (Session session = sessionFactory().openSession()) {
            return session.createQuery("""
                    SELECT DISTINCT t
                    FROM Tratamiento t
                    JOIN FETCH t.paciente
                    JOIN FETCH t.medico
                    LEFT JOIN FETCH t.detalles d
                    LEFT JOIN FETCH d.medicamento
                    WHERE t.id = :id
                    """, Tratamiento.class)
                    .setParameter("id", id)
                    .uniqueResult();
        }
    }

    // UPDATE: actualizar datos y agregar detalles nuevos
    public void actualizar(Long id, Tratamiento datos) {
        ejecutarTransaccion(session -> {

            Tratamiento existente = obtener(
                    session, Tratamiento.class, id, "tratamiento");

            existente.setPaciente(obtener(
                    session, Paciente.class,
                    datos.getPaciente().getId(), "paciente"));

            existente.setMedico(obtener(
                    session, Medico.class,
                    datos.getMedico().getId(), "médico"));

            existente.setFechaInicio(datos.getFechaInicio());
            existente.setFechaFin(datos.getFechaFin());

            // Conservamos el estado actual del tratamiento.
            // La desactivación se realiza con su método específico.

            if (datos.getDetalles() != null) {
                for (DetalleTratamiento detalle : datos.getDetalles()) {

                    // Solo insertar detalles nuevos
                    if (detalle.getId() != null) {
                        continue;
                    }

                    DetalleTratamiento nuevo = new DetalleTratamiento();

                    nuevo.setTratamiento(existente);
                    nuevo.setMedicamento(obtener(
                            session, Medicamento.class,
                            detalle.getMedicamento().getId(),
                            "medicamento"));

                    nuevo.setDosisIndicada(detalle.getDosisIndicada());
                    nuevo.setUnidad(detalle.getUnidad());
                    nuevo.setInstrucciones(detalle.getInstrucciones());

                    session.persist(nuevo);
                }
            }

            session.flush();
        });
    }

    // Agregar un medicamento a un tratamiento existente
    public void agregarDetalle(Long tratamientoId,
                               DetalleTratamiento detalle) {
        ejecutarTransaccion(session -> {

            Tratamiento tratamiento = obtener(
                    session, Tratamiento.class,
                    tratamientoId, "tratamiento");

            Medicamento medicamento = obtener(
                    session, Medicamento.class,
                    detalle.getMedicamento().getId(), "medicamento");

            detalle.setTratamiento(tratamiento);
            detalle.setMedicamento(medicamento);

            session.persist(detalle);
        });
    }

    // Desactivar sin eliminar el historial
    public void desactivar(Long id) {
        ejecutarTransaccion(session -> {

            Tratamiento tratamiento = obtener(
                    session, Tratamiento.class, id, "tratamiento");

            tratamiento.setActivo(false);
        });
    }

    // CONSULTA HQL 1:
    // Medicamentos de tratamientos activos de un paciente
    public List<DetalleTratamiento> medicamentosActivos(Long pacienteId) {
        try (Session session = sessionFactory().openSession()) {
            return session.createQuery("""
                    SELECT d
                    FROM DetalleTratamiento d
                    JOIN FETCH d.tratamiento t
                    JOIN FETCH d.medicamento
                    WHERE t.paciente.id = :pacienteId
                    AND t.activo = true
                    ORDER BY d.medicamento.id
                    """, DetalleTratamiento.class)
                    .setParameter("pacienteId", pacienteId)
                    .getResultList();
        }
    }

    // CONSULTA HQL 2:
    // Listar tratamientos de un paciente
    public List<Tratamiento> listarPorPaciente(Long pacienteId) {
        try (Session session = sessionFactory().openSession()) {
            return session.createQuery("""
                    SELECT DISTINCT t
                    FROM Tratamiento t
                    JOIN FETCH t.paciente
                    JOIN FETCH t.medico
                    LEFT JOIN FETCH t.detalles d
                    LEFT JOIN FETCH d.medicamento
                    WHERE t.paciente.id = :pacienteId
                    ORDER BY t.fechaInicio DESC
                    """, Tratamiento.class)
                    .setParameter("pacienteId", pacienteId)
                    .getResultList();
        }
    }
}
