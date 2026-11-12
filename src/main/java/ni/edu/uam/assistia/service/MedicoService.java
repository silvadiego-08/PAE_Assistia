package ni.edu.uam.assistia.service;

import ni.edu.uam.assistia.dao.MedicoDAO;
import ni.edu.uam.assistia.exception.RegistroEnUsoException;
import ni.edu.uam.assistia.exception.ValidacionException;
import ni.edu.uam.assistia.model.Medico;
import ni.edu.uam.assistia.util.Validador;

import java.util.List;

public class MedicoService {

    private final MedicoDAO dao = new MedicoDAO();

    public List<Medico> listar() {
        return dao.listar();
    }

    public Medico registrar(String nombre, String especialidad, String telefono) {
        Medico m = new Medico();
        aplicar(m, nombre, especialidad, telefono);
        dao.crear(m);
        return m;
    }

    public void actualizar(Long id, String nombre, String especialidad, String telefono) {
        if (id == null) {
            throw new ValidacionException("Seleccione un médico de la tabla antes de actualizar.");
        }
        Medico datos = new Medico();
        datos.setId(id);
        aplicar(datos, nombre, especialidad, telefono);
        dao.actualizar(datos);
    }

    public void eliminar(Medico m) {
        if (m == null || m.getId() == null) {
            throw new ValidacionException("Seleccione un médico de la tabla antes de eliminar.");
        }
        try {
            dao.eliminar(m.getId());
        } catch (RegistroEnUsoException e) {
            throw new RegistroEnUsoException("No se puede eliminar al médico «" + m.getNombre()
                    + "» porque aparece en uno o más tratamientos. "
                    + "Se conserva el registro para no perder el historial.", e);
        }
    }

    private static void aplicar(Medico m, String nombre, String especialidad, String telefono) {
        m.setNombre(Validador.obligatorio(nombre, "Nombre", 120));
        m.setEspecialidad(Validador.obligatorio(especialidad, "Especialidad", 80));
        m.setTelefono(Validador.telefonoOpcional(telefono));
    }
}
