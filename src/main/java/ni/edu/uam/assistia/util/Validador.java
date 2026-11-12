package ni.edu.uam.assistia.util;

import ni.edu.uam.assistia.exception.ValidacionException;

public final class Validador {

    private Validador() { }

    /** Texto obligatorio: se recorta y se valida longitud máxima. */
    public static String obligatorio(String valor, String campo, int max) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty()) {
            throw new ValidacionException("El campo «" + campo + "» es obligatorio.");
        }
        if (v.length() > max) {
            throw new ValidacionException("El campo «" + campo + "» no puede superar " + max + " caracteres.");
        }
        return v;
    }

    /** Teléfono opcional: devuelve null si está vacío. */
    public static String telefonoOpcional(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (!v.matches("[0-9+()\\- ]{7,20}")) {
            throw new ValidacionException("El teléfono no es válido. Use solo números, espacios, +, - y paréntesis (7 a 20 caracteres).");
        }
        return v;
    }
}
