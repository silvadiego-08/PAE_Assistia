package ni.edu.uam.assistia.dao;

import jakarta.persistence.LockModeType;
import ni.edu.uam.assistia.exception.ValidacionException;
import ni.edu.uam.assistia.model.DetalleTratamiento;
import ni.edu.uam.assistia.model.EstadoToma;
import ni.edu.uam.assistia.model.Medicamento;
import ni.edu.uam.assistia.model.RegistroToma;
import ni.edu.uam.assistia.model.Tratamiento;
import ni.edu.uam.assistia.util.Tx;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RegistroTomaDAO {
    public record Toma(Long id, Medicamento producto, BigDecimal dosis, String unidad,
                       Long tratamientoId, LocalDateTime programada,
                       LocalDateTime realizada, EstadoToma estado, String observacion) {
    }

    public List<Toma> listar(Long pacienteId, LocalDate dia) {
        return Tx.leer(session -> session.createSelectionQuery("""
                SELECT r.id, m, d.dosisIndicada, d.unidad, d.tratamiento.id,
                       r.fechaHoraProgramada, r.fechaHoraReal, r.estado, r.observacion
                FROM RegistroToma r
                JOIN r.detalleTratamiento d
                JOIN d.medicamento m
                WHERE d.tratamiento.paciente.id = :paciente
                  AND r.fechaHoraProgramada >= :inicio
                  AND r.fechaHoraProgramada < :fin
                ORDER BY r.fechaHoraProgramada, r.id
                """, Toma.class)
                .setParameter("paciente", pacienteId)
                .setParameter("inicio", dia.atStartOfDay())
                .setParameter("fin", dia.plusDays(1).atStartOfDay())
                .getResultList());
    }

    public void programar(Long pacienteId, Long detalleId, LocalDateTime hora, String observacion) {
        Tx.ejecutarVoid(session -> {
            DetalleTratamiento detalle = session.find(DetalleTratamiento.class, detalleId);
            if (detalle == null) {
                throw new ValidacionException("El medicamento del tratamiento ya no está disponible.");
            }
            validarHorario(session, detalle, pacienteId, hora);
            session.persist(new RegistroToma(detalle, hora, observacion));
        });
    }

    public void reprogramar(Long tomaId, Long pacienteId, LocalDateTime hora) {
        Tx.ejecutarVoid(session -> {
            RegistroToma toma = buscarPendiente(session, tomaId, pacienteId);
            validarHorario(session, toma.getDetalleTratamiento(), pacienteId, hora);
            toma.reprogramar(hora);
        });
    }

    public void marcarRealizada(Long tomaId, Long pacienteId) {
        Tx.ejecutarVoid(session ->
                buscarPendiente(session, tomaId, pacienteId).marcarRealizada(LocalDateTime.now()));
    }

    private RegistroToma buscarPendiente(Session session, Long tomaId, Long pacienteId) {
        RegistroToma toma = session.find(RegistroToma.class, tomaId, LockModeType.PESSIMISTIC_WRITE);
        if (toma == null || !pacienteId.equals(toma.getDetalleTratamiento().getTratamiento().getPaciente().getId())) {
            throw new ValidacionException("La toma seleccionada no pertenece al paciente.");
        }
        if (toma.getEstado() != EstadoToma.PENDIENTE) {
            throw new ValidacionException("Solo puedes modificar una toma pendiente. Recarga el listado.");
        }
        return toma;
    }

    private void validarHorario(Session session, DetalleTratamiento detalle, Long pacienteId, LocalDateTime hora) {
        // Comparte el bloqueo de la edición del tratamiento antes de validar su paciente.
        Tratamiento tratamiento = session.find(Tratamiento.class, detalle.getTratamiento().getId(),
                LockModeType.PESSIMISTIC_WRITE);
        if (!pacienteId.equals(tratamiento.getPaciente().getId())) {
            throw new ValidacionException("El medicamento seleccionado no pertenece al paciente.");
        }

        LocalDate dia = hora.toLocalDate();
        if (!tratamiento.isActivo() || dia.isBefore(tratamiento.getFechaInicio())
                || (tratamiento.getFechaFin() != null && dia.isAfter(tratamiento.getFechaFin()))) {
            throw new ValidacionException("Elige una fecha dentro de un tratamiento activo.");
        }
    }
}
