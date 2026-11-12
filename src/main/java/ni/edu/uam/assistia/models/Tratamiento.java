
package ni.edu.uam.assistia.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tratamientos")
@Getter
@Setter
@NoArgsConstructor
public class Tratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Temporales hasta integrar las entidades
    // Paciente y Medico de tus compañeros.
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "medico_id", nullable = false)
    private Long medicoId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(
            mappedBy = "tratamiento",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private List<DetalleTratamiento> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleTratamiento detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException(
                    "El detalle no puede ser nulo."
            );
        }

        detalles.add(detalle);
        detalle.setTratamiento(this);
    }
}
