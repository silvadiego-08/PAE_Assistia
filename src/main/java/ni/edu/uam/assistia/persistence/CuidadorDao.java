package ni.edu.uam.assistia.persistence;

import java.util.List;
import ni.edu.uam.assistia.model.Cuidador;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

public final class CuidadorDao {
    private final HibernateConfig hibernate;

    public CuidadorDao(HibernateConfig hibernate) {
        this.hibernate = hibernate;
    }

    public List<Cuidador> listar() {
        return hibernate.sessionFactory().fromTransaction(session ->
                session.createSelectionQuery(
                        "from Cuidador c order by c.nombre, c.id", Cuidador.class).getResultList());
    }

    public void crear(String nombre, String telefono, String correo) {
        hibernate.sessionFactory().inTransaction(session ->
                session.persist(new Cuidador(nombre, telefono, correo)));
    }

    public void actualizar(long id, String nombre, String telefono, String correo) {
        hibernate.sessionFactory().inTransaction(session ->
                buscar(session, id).actualizar(nombre, telefono, correo));
    }

    public void eliminar(long id) {
        try {
            hibernate.sessionFactory().inTransaction(session -> session.remove(buscar(session, id)));
        } catch (RuntimeException error) {
            for (Throwable causa = error; causa != null; causa = causa.getCause()) {
                if (causa instanceof ConstraintViolationException restriccion
                        && "23503".equals(restriccion.getSQLState())) {
                    throw new IllegalArgumentException(
                            "El cuidador está vinculado a un paciente. Quita la asignación antes de eliminarlo.",
                            error);
                }
            }
            throw error;
        }
    }

    private Cuidador buscar(Session session, long id) {
        Cuidador cuidador = session.find(Cuidador.class, id);
        if (cuidador == null) {
            throw new IllegalArgumentException("El cuidador seleccionado ya no existe.");
        }
        return cuidador;
    }
}
