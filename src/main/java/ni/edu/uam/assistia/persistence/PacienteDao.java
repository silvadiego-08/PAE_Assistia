package ni.edu.uam.assistia.persistence;

import java.time.LocalDate;
import java.util.List;
import ni.edu.uam.assistia.model.AsignacionCuidador;
import ni.edu.uam.assistia.model.Cuidador;
import ni.edu.uam.assistia.model.Paciente;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

public final class PacienteDao {
    private final HibernateConfig hibernate;

    public PacienteDao(HibernateConfig hibernate) {
        this.hibernate = hibernate;
    }

    public List<Paciente> listar() {
        return hibernate.sessionFactory().fromTransaction(session -> session
                .createSelectionQuery("from Paciente p order by p.nombre, p.id", Paciente.class)
                .getResultList());
    }

    public void guardar(Long id, String nombre, LocalDate nacimiento, String telefono) {
        nombre = nombre.strip();
        telefono = telefono.strip();
        if (nombre.isEmpty() || nombre.length() > 120) {
            throw new IllegalArgumentException("Escribe un nombre de entre 1 y 120 caracteres.");
        }
        if (nacimiento == null || nacimiento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Selecciona una fecha de nacimiento que no sea futura.");
        }
        if (telefono.length() > 25) {
            throw new IllegalArgumentException("El teléfono debe tener como máximo 25 caracteres.");
        }
        String nombreFinal = nombre;
        String telefonoFinal = telefono.isEmpty() ? null : telefono;
        hibernate.sessionFactory().inTransaction(session -> {
            if (id == null) {
                session.persist(new Paciente(nombreFinal, nacimiento, telefonoFinal));
            } else {
                buscarPaciente(session, id).actualizar(nombreFinal, nacimiento, telefonoFinal);
            }
        });
    }

    public void eliminar(long id) {
        try {
            hibernate.sessionFactory().inTransaction(session -> session.remove(buscarPaciente(session, id)));
        } catch (RuntimeException error) {
            if (esRestriccion(error, "23503")) {
                throw new IllegalArgumentException("El paciente tiene registros vinculados y no se puede eliminar.", error);
            }
            throw error;
        }
    }

    public List<AsignacionCuidador> listarAsignaciones(long pacienteId) {
        return hibernate.sessionFactory().fromTransaction(session -> session
                .createSelectionQuery("""
                        select a from AsignacionCuidador a
                        join fetch a.cuidador c
                        where a.paciente.id = :pacienteId
                        order by c.nombre, a.id
                        """, AsignacionCuidador.class)
                .setParameter("pacienteId", pacienteId)
                .getResultList());
    }

    public void asignar(long pacienteId, long cuidadorId, String parentesco, boolean principal) {
        String parentescoLimpio = parentesco.strip();
        if (parentescoLimpio.length() > 60) {
            throw new IllegalArgumentException("El parentesco debe tener como máximo 60 caracteres.");
        }
        try {
            hibernate.sessionFactory().inTransaction(session -> {
                Paciente paciente = buscarPaciente(session, pacienteId);
                Cuidador cuidador = session.find(Cuidador.class, cuidadorId);
                if (cuidador == null) {
                    throw new IllegalArgumentException("El cuidador seleccionado ya no existe.");
                }
                session.persist(new AsignacionCuidador(paciente, cuidador,
                        parentescoLimpio.isEmpty() ? null : parentescoLimpio, principal));
            });
        } catch (RuntimeException error) {
            if (esRestriccion(error, "23505")) {
                throw new IllegalArgumentException("Este cuidador ya está asignado al paciente.", error);
            }
            throw error;
        }
    }

    public void quitarAsignacion(long pacienteId, long asignacionId) {
        hibernate.sessionFactory().inTransaction(session -> {
            AsignacionCuidador asignacion = session.find(AsignacionCuidador.class, asignacionId);
            if (asignacion == null || !asignacion.getPaciente().getId().equals(pacienteId)) {
                throw new IllegalArgumentException("La asignación ya no está disponible para este paciente.");
            }
            session.remove(asignacion);
        });
    }

    private Paciente buscarPaciente(Session session, long id) {
        Paciente paciente = session.find(Paciente.class, id);
        if (paciente == null) {
            throw new IllegalArgumentException("El paciente seleccionado ya no existe.");
        }
        return paciente;
    }

    private boolean esRestriccion(Throwable error, String estadoSql) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException restriccion
                    && estadoSql.equals(restriccion.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
