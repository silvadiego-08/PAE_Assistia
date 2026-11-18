package ni.edu.uam.assistia.service;

import ni.edu.uam.assistia.dao.TratamientoDAO;
import ni.edu.uam.assistia.exception.ValidacionException;
import ni.edu.uam.assistia.model.DetalleTratamiento;
import ni.edu.uam.assistia.model.Tratamiento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public class TratamientoService {
    public static final List<String> UNIDADES = List.of("mg", "g", "ml", "mcg", "tableta", "cápsula", "gotas", "unidad");

    private final TratamientoDAO dao = new TratamientoDAO();

    public void validarDetalle(DetalleTratamiento detalle) {
        if (detalle == null || detalle.getMedicamento() == null) {
            throw new ValidacionException("Seleccione un medicamento.");
        }
        validarId(detalle.getMedicamento().getId(), "Seleccione un medicamento válido.");

        BigDecimal dosis = detalle.getDosisIndicada();
        if (dosis == null || dosis.signum() <= 0) {
            throw new ValidacionException("La dosis debe ser mayor que cero.");
        }
        BigDecimal valor = dosis.stripTrailingZeros();
        if (valor.scale() > 3 || valor.precision() - (long) valor.scale() > 9) {
            throw new ValidacionException("La dosis admite hasta nueve dígitos enteros y tres decimales.");
        }

        String unidad = detalle.getUnidad();
        if (unidad == null || !UNIDADES.contains(unidad.strip().toLowerCase(Locale.ROOT))) {
            throw new ValidacionException("Seleccione una unidad válida.");
        }
        String instrucciones = detalle.getInstrucciones();
        if (instrucciones != null && instrucciones.length() > 500) {
            throw new ValidacionException("Las instrucciones admiten hasta 500 caracteres.");
        }
    }

    public void validarTratamiento(Tratamiento tratamiento, boolean exigirDetalles) {
        if (tratamiento == null) {
            throw new ValidacionException("Ingrese los datos del tratamiento.");
        }
        if (tratamiento.getPaciente() == null) {
            throw new ValidacionException("Seleccione un paciente.");
        }
        if (tratamiento.getMedico() == null) {
            throw new ValidacionException("Seleccione un médico.");
        }
        validarId(tratamiento.getPaciente().getId(), "Seleccione un paciente válido.");
        validarId(tratamiento.getMedico().getId(), "Seleccione un médico válido.");

        if (tratamiento.getFechaInicio() == null) {
            throw new ValidacionException("Seleccione la fecha de inicio.");
        }
        if (tratamiento.getFechaFin() != null && tratamiento.getFechaFin().isBefore(tratamiento.getFechaInicio())) {
            throw new ValidacionException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
        List<DetalleTratamiento> detalles = tratamiento.getDetalles();
        if (exigirDetalles && (detalles == null || detalles.isEmpty())) {
            throw new ValidacionException("Agregue al menos un medicamento al tratamiento.");
        }
        if (detalles != null) {
            detalles.forEach(this::validarDetalle);
        }
    }

    public void guardar(Tratamiento tratamiento) {
        validarTratamiento(tratamiento, true);
        dao.guardar(tratamiento);
    }

    public List<Tratamiento> listar() {
        return dao.listar();
    }

    public Tratamiento buscarPorId(Long id) {
        validarId(id, "Seleccione un tratamiento válido.");
        return dao.buscarPorId(id);
    }

    public void actualizar(Long id, Tratamiento tratamiento) {
        validarId(id, "Seleccione un tratamiento válido.");
        validarTratamiento(tratamiento, false);
        dao.actualizar(id, tratamiento);
    }

    public void agregarDetalle(Long tratamientoId, DetalleTratamiento detalle) {
        validarId(tratamientoId, "Seleccione un tratamiento válido.");
        validarDetalle(detalle);
        dao.agregarDetalle(tratamientoId, detalle);
    }

    public void desactivar(Long id) {
        validarId(id, "Seleccione un tratamiento válido.");
        dao.desactivar(id);
    }

    public List<DetalleTratamiento> medicamentosActivos(Long pacienteId) {
        validarId(pacienteId, "Seleccione un paciente válido.");
        return dao.medicamentosActivos(pacienteId);
    }

    public List<Tratamiento> listarPorPaciente(Long pacienteId) {
        validarId(pacienteId, "Seleccione un paciente válido.");
        return dao.listarPorPaciente(pacienteId);
    }

    private void validarId(Long id, String mensaje) {
        if (id == null || id <= 0) {
            throw new ValidacionException(mensaje);
        }
    }
}
