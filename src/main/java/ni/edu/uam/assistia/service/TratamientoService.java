
package ni.edu.uam.assistia.service;

import ni.edu.uam.assistia.dao.TratamientoDAO;
import ni.edu.uam.assistia.model.Tratamiento;
import ni.edu.uam.assistia.model.DetalleTratamiento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class TratamientoService {

    private final TratamientoDAO dao = new TratamientoDAO();

    // Unidades permitidas para registrar dosis
    private static final Set<String> UNIDADES_VALIDAS = Set.of(
            "mg",
            "g",
            "ml",
            "mcg",
            "tableta",
            "cápsula",
            "gotas",
            "unidad"
    );

    // Validar los datos de un medicamento
    public void validarDetalle(DetalleTratamiento detalle) {

        if (detalle == null) {
            throw new IllegalArgumentException(
                    "El detalle no puede estar vacío."
            );
        }

        if (detalle.getMedicamento() == null ||
                detalle.getMedicamento().getId() == null ||
                detalle.getMedicamento().getId() <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un medicamento válido."

            );
        }

        if (detalle.getDosisIndicada() == null ||
                detalle.getDosisIndicada()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "La dosis debe ser mayor que cero."
            );
        }

        if (detalle.getUnidad() == null ||
                !UNIDADES_VALIDAS.contains(
                        detalle.getUnidad().trim().toLowerCase()
                )) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una unidad válida."
            );
        }
    }

    // Validar los datos generales de un tratamiento
    public void validarTratamiento(
            Tratamiento tratamiento,
            boolean exigirDetalles) {

        if (tratamiento == null) {
            throw new IllegalArgumentException(
                    "El tratamiento no puede estar vacío."
            );
        }

        if (tratamiento.getPaciente() == null ||
                tratamiento.getPaciente().getId() == null ||
                tratamiento.getPaciente().getId() <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un paciente válido."
            );
        }

        if (tratamiento.getMedico() == null ||
                tratamiento.getMedico().getId() == null ||
                tratamiento.getMedico().getId() <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un médico válido."
            );
        }

        if (tratamiento.getFechaInicio() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar la fecha de inicio."
            );
        }

        if (tratamiento.getFechaFin() != null &&
                tratamiento.getFechaFin()
                        .isBefore(tratamiento.getFechaInicio())) {

            throw new IllegalArgumentException(
                    "La fecha final no puede ser anterior a la inicial."
            );
        }

        if (exigirDetalles &&
                (tratamiento.getDetalles() == null ||
                        tratamiento.getDetalles().isEmpty())) {

            throw new IllegalArgumentException(
                    "Debe agregar al menos un medicamento."
            );
        }

        if (tratamiento.getDetalles() != null) {

            for (DetalleTratamiento detalle :
                    tratamiento.getDetalles()) {

                validarDetalle(detalle);
            }
        }
    }

    // CREATE: guardar tratamiento
    public void guardar(Tratamiento tratamiento) {

        validarTratamiento(tratamiento, true);

        dao.guardar(tratamiento);
    }

    // READ: listar tratamientos
    public List<Tratamiento> listar() {

        return dao.listar();
    }

    // READ: buscar por ID
    public Tratamiento buscarPorId(Long id) {

        validarId(id);

        return dao.buscarPorId(id);
    }

    // UPDATE: actualizar tratamiento
    public void actualizar(Long id, Tratamiento tratamiento) {

        validarId(id);
        validarTratamiento(tratamiento, false);

        dao.actualizar(id, tratamiento);
    }

    // Agregar un medicamento a un tratamiento existente
    public void agregarDetalle(
            Long tratamientoId,
            DetalleTratamiento detalle) {

        validarId(tratamientoId);
        validarDetalle(detalle);

        dao.agregarDetalle(tratamientoId, detalle);
    }

    // Desactivar tratamiento sin borrar su historial
    public void desactivar(Long id) {

        validarId(id);

        dao.desactivar(id);
    }

    // CONSULTA HQL 1
    // Medicamentos de tratamientos activos de un paciente
    public List<DetalleTratamiento> medicamentosActivos(
            Long pacienteId) {

        validarId(pacienteId);

        return dao.medicamentosActivos(pacienteId);
    }

    // CONSULTA HQL 2
    // Tratamientos registrados para un paciente
    public List<Tratamiento> listarPorPaciente(
            Long pacienteId) {

        validarId(pacienteId);

        return dao.listarPorPaciente(pacienteId);
    }

    // Validación reutilizable de identificadores
    private void validarId(Long id) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador debe ser mayor que cero."
            );
        }
    }
}
