package ni.edu.uam.assistia.persistencia;

import ni.edu.uam.assistia.modelo.EstadoToma;

import java.time.LocalDateTime;

/**
 * Datos ya preparados para la tabla de JavaFX.
 */
public record TomaPendienteVista(
        Long id,
        String medicamento,
        LocalDateTime fechaHoraProgramada,
        EstadoToma estado,
        String observacion) {
}
