package ni.edu.uam.assistia.model;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "pacientes")

public class Paciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(length = 25)
    private String telefono;

    protected Paciente() {
    }

    public Paciente(String nombre, LocalDate fechaNacimiento, String telefono) {
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void actualizar(String nombre, LocalDate fechaNacimiento, String telefono) {
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
    }
}
