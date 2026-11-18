package ni.edu.uam.assistia.dao;

import jakarta.persistence.LockModeType;
import ni.edu.uam.assistia.exception.ValidacionException;
import ni.edu.uam.assistia.model.DetalleTratamiento;
import ni.edu.uam.assistia.model.Medicamento;
import ni.edu.uam.assistia.model.Medico;
import ni.edu.uam.assistia.model.Paciente;
import ni.edu.uam.assistia.model.Tratamiento;
import ni.edu.uam.assistia.util.Tx;
import org.hibernate.Session;

import java.util.List;

public class TratamientoDAO {
    private static final String TRATAMIENTOS_CON_DETALLES = """
            SELECT DISTINCT t
            FROM Tratamiento t
            JOIN FETCH t.paciente
            JOIN FETCH t.medico
            LEFT JOIN FETCH t.detalles d
            LEFT JOIN FETCH d.medicamento
            """;

    public void guardar(Tratamiento tratamiento) {
        Tx.ejecutarVoid(session -> {
            asignarDatos(session, tratamiento, tratamiento);
            for (DetalleTratamiento detalle : tratamiento.getDetalles()) {
                vincularMedicamento(session, detalle);
                detalle.setTratamiento(tratamiento);
            }
            session.persist(tratamiento);
        });
    }

    public List<Tratamiento> listar() {
        return Tx.leer(session -> session.createSelectionQuery(
                TRATAMIENTOS_CON_DETALLES + "ORDER BY t.id DESC", Tratamiento.class).getResultList());
    }

    public Tratamiento buscarPorId(Long id) {
        return Tx.leer(session -> session.createSelectionQuery(
                TRATAMIENTOS_CON_DETALLES + "WHERE t.id = :id", Tratamiento.class)
                .setParameter("id", id)
                .uniqueResult());
    }

    public void actualizar(Long id, Tratamiento datos) {
        Tx.ejecutarVoid(session -> {
            Tratamiento existente = session.find(Tratamiento.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (existente == null) {
                throw new ValidacionException("El tratamiento seleccionado ya no existe.");
            }
            if (!existente.getPaciente().getId().equals(datos.getPaciente().getId()) && tieneTomas(session, id)) {
                throw new ValidacionException("Este tratamiento tiene tomas registradas y debe conservar su paciente.");
            }
            asignarDatos(session, existente, datos);
            if (datos.getDetalles() != null) {
                for (DetalleTratamiento detalle : datos.getDetalles()) {
                    if (detalle.getId() == null) {
                        agregarDetalle(session, existente, detalle);
                    }
                }
            }
        });
    }

    private boolean tieneTomas(Session session, Long tratamientoId) {
        return !session.createSelectionQuery("""
                SELECT r.id FROM RegistroToma r
                WHERE r.detalleTratamiento.tratamiento.id = :tratamiento
                """, Long.class)
                .setParameter("tratamiento", tratamientoId)
                .setMaxResults(1)
                .getResultList().isEmpty();
    }

    public void agregarDetalle(Long tratamientoId, DetalleTratamiento detalle) {
        Tx.ejecutarVoid(session -> agregarDetalle(session,
                obtener(session, Tratamiento.class, tratamientoId, "tratamiento"), detalle));
    }

    public void desactivar(Long id) {
        Tx.ejecutarVoid(session -> obtener(session, Tratamiento.class, id, "tratamiento").setActivo(false));
    }

    public List<DetalleTratamiento> medicamentosActivos(Long pacienteId) {
        return Tx.leer(session -> session.createSelectionQuery("""
                SELECT d
                FROM DetalleTratamiento d
                JOIN FETCH d.tratamiento t
                JOIN FETCH d.medicamento m
                WHERE t.paciente.id = :pacienteId
                  AND t.activo = true
                ORDER BY t.fechaInicio DESC, t.id DESC, m.nombre, d.id
                """, DetalleTratamiento.class)
                .setParameter("pacienteId", pacienteId)
                .getResultList());
    }

    public List<Tratamiento> listarPorPaciente(Long pacienteId) {
        return Tx.leer(session -> session.createSelectionQuery(
                TRATAMIENTOS_CON_DETALLES + "WHERE t.paciente.id = :pacienteId ORDER BY t.fechaInicio DESC",
                Tratamiento.class)
                .setParameter("pacienteId", pacienteId)
                .getResultList());
    }

    private void asignarDatos(Session session, Tratamiento tratamiento, Tratamiento datos) {
        tratamiento.setPaciente(obtener(session, Paciente.class, datos.getPaciente().getId(), "paciente"));
        tratamiento.setMedico(obtener(session, Medico.class, datos.getMedico().getId(), "médico"));
        tratamiento.setFechaInicio(datos.getFechaInicio());
        tratamiento.setFechaFin(datos.getFechaFin());
    }

    private void agregarDetalle(Session session, Tratamiento tratamiento, DetalleTratamiento detalle) {
        vincularMedicamento(session, detalle);
        tratamiento.agregarDetalle(detalle);
        session.persist(detalle);
    }

    private void vincularMedicamento(Session session, DetalleTratamiento detalle) {
        detalle.setMedicamento(obtener(session, Medicamento.class, detalle.getMedicamento().getId(), "medicamento"));
    }

    private <T> T obtener(Session session, Class<T> tipo, Long id, String nombre) {
        T entidad = session.find(tipo, id);
        if (entidad == null) {
            throw new ValidacionException("El " + nombre + " seleccionado ya no existe.");
        }
        return entidad;
    }
}
