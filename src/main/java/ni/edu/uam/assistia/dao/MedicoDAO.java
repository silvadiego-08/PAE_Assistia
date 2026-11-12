package ni.edu.uam.assistia.dao;

import ni.edu.uam.assistia.exception.PersistenciaException;
import ni.edu.uam.assistia.model.Medico;
import ni.edu.uam.assistia.util.Tx;

import java.util.List;

public class MedicoDAO {

    public void crear(Medico m) {
        Tx.ejecutarVoid(s -> s.persist(m));
    }

    public List<Medico> listar() {
        return Tx.leer(s -> s.createQuery("FROM Medico m ORDER BY m.nombre", Medico.class).list());
    }

    public Medico buscarPorId(Long id) {
        return Tx.leer(s -> s.find(Medico.class, id));
    }

    public void actualizar(Medico datos) {
        Tx.ejecutarVoid(s -> {
            Medico actual = s.find(Medico.class, datos.getId());
            if (actual == null) {
                throw new PersistenciaException("El médico ya no existe. Actualice la lista.");
            }
            actual.setNombre(datos.getNombre());
            actual.setEspecialidad(datos.getEspecialidad());
            actual.setTelefono(datos.getTelefono());
        });
    }

    public void eliminar(Long id) {
        Tx.ejecutarVoid(s -> {
            Medico m = s.find(Medico.class, id);
            if (m == null) {
                throw new PersistenciaException("El médico ya no existe. Actualice la lista.");
            }
            s.remove(m);
        });
    }
}
