package ni.edu.uam.assistia.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_toma",
        uniqueConstraints = @UniqueConstraint(name = "uq_registro_toma_detalle_hora",
                columnNames = {"detalle_tratamiento_id", "fecha_hora_programada"}),
        check = @CheckConstraint(name = "ck_registro_toma_estado_hora", constraint = """
                (estado = 'PENDIENTE' AND fecha_hora_real IS NULL)
                OR (estado = 'REALIZADA' AND fecha_hora_real IS NOT NULL)
                """))
@Getter
public class RegistroToma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_tratamiento_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_registro_toma_detalle"))
    private DetalleTratamiento detalleTratamiento;

    @Column(name = "fecha_hora_programada", nullable = false)
    private LocalDateTime fechaHoraProgramada;

    @Column(name = "fecha_hora_real")
    private LocalDateTime fechaHoraReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoToma estado = EstadoToma.PENDIENTE;

    @Column(length = 300)
    private String observacion;

    protected RegistroToma() {
    }

    public RegistroToma(DetalleTratamiento detalle, LocalDateTime programada, String observacion) {
        this.detalleTratamiento = detalle;
        this.fechaHoraProgramada = programada;
        this.observacion = observacion;
    }

    public void marcarRealizada(LocalDateTime horaReal) {
        estado = EstadoToma.REALIZADA;
        fechaHoraReal = horaReal;
    }

    public void reprogramar(LocalDateTime nuevaHora) {
        fechaHoraProgramada = nuevaHora;
    }
}
