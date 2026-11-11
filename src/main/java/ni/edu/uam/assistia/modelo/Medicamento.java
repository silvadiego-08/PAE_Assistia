package ni.edu.uam.assistia.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "medicamento")
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 80)
    private String presentacion;

    @Column(length = 80)
    private String concentracion;

    public Medicamento() {
    }

    public Medicamento(String nombre, String presentacion, String concentracion) {
        this.nombre = nombre;
        this.presentacion = presentacion;
        this.concentracion = concentracion;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public String getConcentracion() {
        return concentracion;
    }

    @Override
    public String toString() {
        return nombre + (concentracion == null || concentracion.isBlank() ? "" : " " + concentracion);
    }
}
