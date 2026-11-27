package ni.edu.uam.assistia.rxnorm;

/**
 * Un resultado limpio de la búsqueda en RxNorm.
 * rxcui se conserva para identificar el concepto, pero la pantalla solo
 * muestra el nombre y la dosis.
 */
public record RxNormResultado(String rxcui, String nombre, String dosis, String descripcion) {

    @Override
    public String toString() {
        return dosis == null || dosis.isBlank() ? nombre : nombre + " - " + dosis;
    }
}
