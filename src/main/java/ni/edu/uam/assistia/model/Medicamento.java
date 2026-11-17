package ni.edu.uam.assistia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Catálogo de productos. La concentración es una característica del PRODUCTO (ej. "500 mg").
 * La dosis que toma el paciente (ej. "1 tableta cada 8 horas") se guarda en el Tratamiento, no aquí.
 */
@Entity
@Table(name = "medicamento",
        uniqueConstraints = @UniqueConstraint(name = "uk_medicamento_producto",
                columnNames = {"nombre", "presentacion", "concentracion"}))
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "principio_activo", nullable = false, length = 120)
    private String principioActivo;

    @Column(name = "presentacion", nullable = false, length = 60)
    private String presentacion;

    @Column(name = "concentracion", nullable = false, length = 60)
    private String concentracion;

    public Medicamento() { }

    public Medicamento(String nombre, String principioActivo, String presentacion, String concentracion) {
        this.nombre = nombre;
        this.principioActivo = principioActivo;
        this.presentacion = presentacion;
        this.concentracion = concentracion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPrincipioActivo() { return principioActivo; }
    public void setPrincipioActivo(String principioActivo) { this.principioActivo = principioActivo; }

    public String getPresentacion() { return presentacion; }
    public void setPresentacion(String presentacion) { this.presentacion = presentacion; }

    public String getConcentracion() { return concentracion; }
    public void setConcentracion(String concentracion) { this.concentracion = concentracion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medicamento otro)) return false;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    /** Útil para ComboBox en las pantallas de otros módulos. */
    @Override
    public String toString() {
        return nombre + " " + concentracion + " (" + presentacion + ")";
    }
}
