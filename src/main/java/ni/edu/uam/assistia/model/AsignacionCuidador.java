package ni.edu.uam.assistia.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "asignaciones_cuidador",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_asignacion_paciente_cuidador",
                columnNames = {"paciente_id", "cuidador_id"}
        )
)

public class AsignacionCuidador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuidador_id", nullable = false)
    private Cuidador cuidador;

    @Column(length = 60)
    private String parentesco;

    @Column(name = "responsable_principal", nullable = false)
    private boolean responsablePrincipal;

    protected AsignacionCuidador() {
    }

    public AsignacionCuidador(
            Paciente paciente, Cuidador cuidador,
            String parentesco, boolean responsablePrincipal) {
        this.paciente = paciente;
        this.cuidador = cuidador;
        this.parentesco = parentesco;
        this.responsablePrincipal = responsablePrincipal;
    }
}
