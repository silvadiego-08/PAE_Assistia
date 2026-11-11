package ni.edu.uam.assistia.persistencia;

import ni.edu.uam.assistia.modelo.DetalleTratamiento;
import ni.edu.uam.assistia.modelo.EstadoToma;
import ni.edu.uam.assistia.modelo.Paciente;
import ni.edu.uam.assistia.modelo.RegistroToma;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RegistroTomaDAO {

    public List<Paciente> listarPacientes() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "select p from Paciente p order by p.apellidos, p.nombres",
                            Paciente.class)
                    .getResultList();
        }
    }

    public List<DetalleTratamiento> listarDetalles(Long pacienteId) {
        if (pacienteId == null) {
            return List.of();
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("""
                            select d from DetalleTratamiento d
                            join fetch d.medicamento
                            join fetch d.tratamiento t
                            where t.paciente.id = :pacienteId
                            order by d.id
                            """, DetalleTratamiento.class)
                    .setParameter("pacienteId", pacienteId)
                    .getResultList();
        }
    }

    /**
     * Verifica dentro de la transacción que el detalle existe y pertenece
     * al paciente seleccionado antes de programar la toma.
     */
    public Long programar(Long pacienteId, Long detalleId,
                          LocalDateTime fechaHoraProgramada, String observacion) {
        if (pacienteId == null || detalleId == null || fechaHoraProgramada == null) {
            throw new RegistroTomaException(
                    "Seleccione paciente, detalle de tratamiento y fecha/hora.");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                DetalleTratamiento detalle = session.createQuery("""
                                select d from DetalleTratamiento d
                                join fetch d.tratamiento t
                                join fetch t.paciente p
                                where d.id = :detalleId
                                """, DetalleTratamiento.class)
                        .setParameter("detalleId", detalleId)
                        .uniqueResultOptional()
                        .orElseThrow(() -> new RegistroTomaException(
                                "El detalle de tratamiento no existe."));

                if (!detalle.getTratamiento().getPaciente().getId().equals(pacienteId)) {
                    throw new RegistroTomaException(
                            "El detalle no pertenece al paciente seleccionado.");
                }

                RegistroToma toma = new RegistroToma(detalle, fechaHoraProgramada, observacion);
                session.persist(toma);
                tx.commit();
                return toma.getId();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                if (esViolacionUnica(e)) {
                    throw new RegistroTomaException(
                            "Ya existe una toma para ese detalle a esa misma hora.", e);
                }
                throw e;
            }
        }
    }

    /**
     * Segunda consulta HQL: tomas pendientes de un paciente en el rango
     * [inicio del día, inicio del día siguiente).
     */
    public List<TomaPendienteVista> consultarPendientes(Long pacienteId, LocalDate dia) {
        if (pacienteId == null || dia == null) {
            throw new RegistroTomaException("Seleccione paciente y fecha.");
        }
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.plusDays(1).atStartOfDay();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<RegistroToma> tomas = session.createQuery("""
                            select r from RegistroToma r
                            join fetch r.detalleTratamiento d
                            join fetch d.medicamento
                            join fetch d.tratamiento t
                            where t.paciente.id = :pacienteId
                              and r.estado = :estado
                              and r.fechaHoraProgramada >= :desde
                              and r.fechaHoraProgramada < :hasta
                            order by r.fechaHoraProgramada
                            """, RegistroToma.class)
                    .setParameter("pacienteId", pacienteId)
                    .setParameter("estado", EstadoToma.PENDIENTE)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta)
                    .getResultList();

            return tomas.stream()
                    .map(t -> new TomaPendienteVista(
                            t.getId(),
                            t.getDetalleTratamiento().getMedicamento().getNombre(),
                            t.getFechaHoraProgramada(),
                            t.getEstado(),
                            t.getObservacion()))
                    .toList();
        }
    }

    /**
     * Devuelve true si la toma cambió a REALIZADA. Si ya estaba realizada
     * devuelve false sin modificar la hora real.
     */
    public boolean marcarRealizada(Long tomaId, LocalDateTime fechaHoraReal) {
        if (tomaId == null) {
            throw new RegistroTomaException("Seleccione una toma.");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                RegistroToma toma = session.get(RegistroToma.class, tomaId);
                if (toma == null) {
                    throw new RegistroTomaException("La toma no existe.");
                }
                if (toma.getEstado() == EstadoToma.REALIZADA) {
                    tx.commit();
                    return false;
                }
                toma.marcarRealizada(fechaHoraReal);
                tx.commit();
                return true;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    public void reprogramar(Long tomaId, LocalDateTime nuevaFechaHora) {
        if (tomaId == null || nuevaFechaHora == null) {
            throw new RegistroTomaException("Seleccione la toma y la nueva fecha/hora.");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                RegistroToma toma = session.get(RegistroToma.class, tomaId);
                if (toma == null) {
                    throw new RegistroTomaException("La toma no existe.");
                }
                toma.reprogramar(nuevaFechaHora);
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                if (esViolacionUnica(e)) {
                    throw new RegistroTomaException(
                            "Ya existe una toma para ese detalle a esa misma hora.", e);
                }
                throw e;
            }
        }
    }

    /**
     * Borra únicamente tomas pendientes; las realizadas se conservan.
     */
    public void eliminarPendiente(Long tomaId) {
        if (tomaId == null) {
            throw new RegistroTomaException("Seleccione una toma.");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                RegistroToma toma = session.get(RegistroToma.class, tomaId);
                if (toma == null) {
                    throw new RegistroTomaException("La toma no existe.");
                }
                if (toma.getEstado() != EstadoToma.PENDIENTE) {
                    throw new RegistroTomaException(
                            "No se pueden eliminar tomas ya realizadas.");
                }
                session.remove(toma);
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    private boolean esViolacionUnica(Throwable error) {
        Throwable actual = error;
        while (actual != null) {
            if (actual instanceof ConstraintViolationException) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }
}
