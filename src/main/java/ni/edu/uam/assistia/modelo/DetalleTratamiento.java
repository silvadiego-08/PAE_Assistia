package ni.edu.uam.assistia.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "detalle_tratamiento")
public class DetalleTratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tratamiento_id", nullable = false)
    private Tratamiento tratamiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(length = 100)
    private String dosis;

    @Column(name = "frecuencia_horas")
    private Integer frecuenciaHoras;

    @Column(length = 300)
    private String indicaciones;

    public DetalleTratamiento() {
    }

    public DetalleTratamiento(Tratamiento tratamiento, Medicamento medicamento,
                              String dosis, Integer frecuenciaHoras, String indicaciones) {
        this.tratamiento = tratamiento;
        this.medicamento = medicamento;
        this.dosis = dosis;
        this.frecuenciaHoras = frecuenciaHoras;
        this.indicaciones = indicaciones;
    }

    public Long getId() {
        return id;
    }

    public Tratamiento getTratamiento() {
        return tratamiento;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public String getDosis() {
        return dosis;
    }

    public Integer getFrecuenciaHoras() {
        return frecuenciaHoras;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    @Override
    public String toString() {
        return medicamento.getNombre() + (dosis == null || dosis.isBlank() ? "" : " - " + dosis);
    }
}
