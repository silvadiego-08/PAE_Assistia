package ni.edu.uam.assistia.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_toma",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_registro_toma_detalle_hora",
                columnNames = {"detalle_tratamiento_id", "fecha_hora_programada"}))
public class RegistroToma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_tratamiento_id", nullable = false)
    private DetalleTratamiento detalleTratamiento;

    @Column(name = "fecha_hora_programada", nullable = false)
    private LocalDateTime fechaHoraProgramada;

    @Column(name = "fecha_hora_real")
    private LocalDateTime fechaHoraReal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoToma estado = EstadoToma.PENDIENTE;

    @Column(name = "observacion", length = 300)
    private String observacion;

    protected RegistroToma() {
    }

    public RegistroToma(DetalleTratamiento detalle,
                        LocalDateTime programada,
                        String observacion) {
        this.detalleTratamiento = detalle;
        this.fechaHoraProgramada = programada;
        this.observacion = observacion;
        this.estado = EstadoToma.PENDIENTE;
    }

    /**
     * Conserva la hora programada y guarda la hora real por separado.
     * Si la toma ya estaba realizada no modifica la hora real original.
     */
    public void marcarRealizada(LocalDateTime real) {
        if (estado == EstadoToma.REALIZADA) {
            return;
        }
        this.estado = EstadoToma.REALIZADA;
        this.fechaHoraReal = (real != null) ? real : LocalDateTime.now();
    }

    public void reprogramar(LocalDateTime nuevaFechaHora) {
        if (estado != EstadoToma.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden reprogramar tomas pendientes.");
        }
        this.fechaHoraProgramada = nuevaFechaHora;
    }

    public Long getId() {
        return id;
    }

    public DetalleTratamiento getDetalleTratamiento() {
        return detalleTratamiento;
    }

    public LocalDateTime getFechaHoraProgramada() {
        return fechaHoraProgramada;
    }

    public LocalDateTime getFechaHoraReal() {
        return fechaHoraReal;
    }

    public EstadoToma getEstado() {
        return estado;
    }

    public String getObservacion() {
        return observacion;
    }
}
