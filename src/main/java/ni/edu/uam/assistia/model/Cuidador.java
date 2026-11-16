package ni.edu.uam.assistia.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cuidadores")
@Getter
@Setter


public class Cuidador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 25)
    private String telefono;

    @Column(length = 150)
    private String correo;

    // Constructor requerido por JPA.
    protected Cuidador() {
    }

    public Cuidador(String nombre, String telefono, String correo) {
        actualizar(nombre, telefono, correo);
    }
    public void actualizar(String nombre, String telefono, String correo) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.correo = correo;
    }

}
