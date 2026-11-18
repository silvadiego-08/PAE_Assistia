package ni.edu.uam.assistia.service;

import ni.edu.uam.assistia.dao.RegistroTomaDAO;
import ni.edu.uam.assistia.dao.RegistroTomaDAO.Toma;
import ni.edu.uam.assistia.dao.TratamientoDAO;
import ni.edu.uam.assistia.exception.RegistroDuplicadoException;
import ni.edu.uam.assistia.exception.ValidacionException;
import ni.edu.uam.assistia.model.DetalleTratamiento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RegistroTomaService {
    private final RegistroTomaDAO tomas = new RegistroTomaDAO();
    private final TratamientoDAO tratamientos = new TratamientoDAO();

    public List<DetalleTratamiento> listarDetalles(Long pacienteId) {
        return tratamientos.medicamentosActivos(pacienteId);
    }

    public List<Toma> listar(Long pacienteId, LocalDate dia) {
        return tomas.listar(pacienteId, dia);
    }

    public void programar(Long pacienteId, Long detalleId, LocalDateTime hora, String observacion) {
        if (detalleId == null) {
            throw new ValidacionException("Selecciona un medicamento del tratamiento.");
        }
        String nota = observacion == null || observacion.isBlank() ? null : observacion.strip();
        if (nota != null && nota.codePointCount(0, nota.length()) > 300) {
            throw new ValidacionException("La observación admite hasta 300 caracteres.");
        }
        guardarHorario(() -> tomas.programar(pacienteId, detalleId, hora, nota));
    }

    public void reprogramar(Long tomaId, Long pacienteId, LocalDateTime hora) {
        guardarHorario(() -> tomas.reprogramar(tomaId, pacienteId, hora));
    }

    public void marcarRealizada(Long tomaId, Long pacienteId) {
        tomas.marcarRealizada(tomaId, pacienteId);
    }

    private void guardarHorario(Runnable operacion) {
        try {
            operacion.run();
        } catch (RegistroDuplicadoException error) {
            throw new RegistroDuplicadoException("Ya existe una toma para ese medicamento del tratamiento a esa hora.", error);
        }
    }
}
