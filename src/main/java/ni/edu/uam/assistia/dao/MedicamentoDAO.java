package ni.edu.uam.assistia.dao;

import ni.edu.uam.assistia.exception.PersistenciaException;
import ni.edu.uam.assistia.model.Medicamento;
import ni.edu.uam.assistia.util.Tx;

import java.util.List;

public class MedicamentoDAO {

    public void crear(Medicamento m) {
        Tx.ejecutarVoid(s -> s.persist(m));
    }

    public List<Medicamento> listar() {
        return Tx.leer(s -> s.createQuery(
                "FROM Medicamento m ORDER BY m.nombre, m.presentacion, m.concentracion",
                Medicamento.class).list());
    }

    public Medicamento buscarPorId(Long id) {
        return Tx.leer(s -> s.find(Medicamento.class, id));
    }

    /** Carga la entidad gestionada y la modifica dentro de la transacción (dirty checking). */
    public void actualizar(Medicamento datos) {
        Tx.ejecutarVoid(s -> {
            Medicamento actual = s.find(Medicamento.class, datos.getId());
            if (actual == null) {
                throw new PersistenciaException("El medicamento ya no existe. Actualice la lista.");
            }
            actual.setNombre(datos.getNombre());
            actual.setPrincipioActivo(datos.getPrincipioActivo());
            actual.setPresentacion(datos.getPresentacion());
            actual.setConcentracion(datos.getConcentracion());
        });
    }

    /** Si hay tratamientos que lo referencian, PostgreSQL rechaza el DELETE (llave foránea) y se hace rollback. */
    public void eliminar(Long id) {
        Tx.ejecutarVoid(s -> {
            Medicamento m = s.find(Medicamento.class, id);
            if (m == null) {
                throw new PersistenciaException("El medicamento ya no existe. Actualice la lista.");
            }
            s.remove(m);
        });
    }
}
