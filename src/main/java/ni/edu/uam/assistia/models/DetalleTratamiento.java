
package ni.edu.uam.assistia.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "detalles_tratamiento")
@Getter
@Setter
@NoArgsConstructor
public class DetalleTratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tratamiento_id", nullable = false)
    private Tratamiento tratamiento;

    // Temporal hasta integrar Medicamento
    @Column(name = "medicamento_id", nullable = false)
    private Long medicamentoId;

    @Column(
            name = "dosis_indicada",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal dosisIndicada;

    @Column(nullable = false, length = 30)
    private String unidad;

    @Column(length = 500)
    private String instrucciones;
}
