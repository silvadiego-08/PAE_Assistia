package ni.edu.uam.assistia.service;

import ni.edu.uam.assistia.dao.MedicamentoDAO;
import ni.edu.uam.assistia.exception.RegistroDuplicadoException;
import ni.edu.uam.assistia.exception.RegistroEnUsoException;
import ni.edu.uam.assistia.exception.ValidacionException;
import ni.edu.uam.assistia.model.Medicamento;
import ni.edu.uam.assistia.util.Validador;

import java.util.List;

public class MedicamentoService {

    private final MedicamentoDAO dao = new MedicamentoDAO();

    public List<Medicamento> listar() {
        return dao.listar();
    }

    public Medicamento registrar(String nombre, String principioActivo, String presentacion, String concentracion) {
        Medicamento m = new Medicamento();
        aplicar(m, nombre, principioActivo, presentacion, concentracion);
        try {
            dao.crear(m);
        } catch (RegistroDuplicadoException e) {
            throw duplicado(e);
        }
        return m;
    }

    public void actualizar(Long id, String nombre, String principioActivo, String presentacion, String concentracion) {
        if (id == null) {
            throw new ValidacionException("Seleccione un medicamento de la tabla antes de actualizar.");
        }
        Medicamento datos = new Medicamento();
        datos.setId(id);
        aplicar(datos, nombre, principioActivo, presentacion, concentracion);
        try {
            dao.actualizar(datos);
        } catch (RegistroDuplicadoException e) {
            throw duplicado(e);
        }
    }

    public void eliminar(Medicamento m) {
        if (m == null || m.getId() == null) {
            throw new ValidacionException("Seleccione un medicamento de la tabla antes de eliminar.");
        }
        try {
            dao.eliminar(m.getId());
        } catch (RegistroEnUsoException e) {
            throw new RegistroEnUsoException("No se puede eliminar «" + m.getNombre()
                    + "» porque ya forma parte de uno o más tratamientos. "
                    + "Se conserva el registro para no perder el historial.", e);
        }
    }

    private static void aplicar(Medicamento m, String nombre, String principioActivo,
                                String presentacion, String concentracion) {
        m.setNombre(Validador.obligatorio(nombre, "Nombre", 120));
        m.setPrincipioActivo(Validador.obligatorio(principioActivo, "Principio activo", 120));
        m.setPresentacion(Validador.obligatorio(presentacion, "Presentación", 60));
        m.setConcentracion(Validador.obligatorio(concentracion, "Concentración", 60));
    }

    private static RegistroDuplicadoException duplicado(Throwable causa) {
        return new RegistroDuplicadoException(
                "Ya existe un medicamento con el mismo nombre, presentación y concentración.", causa);
    }
}
