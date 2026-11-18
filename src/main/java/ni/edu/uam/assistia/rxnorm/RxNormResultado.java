package ni.edu.uam.assistia.rxnorm;

/**
 * Un resultado de la búsqueda de medicamentos en RxNorm.
 * El rxcui es el identificador estable que se usa para quitar duplicados.
 */
public record RxNormResultado(String rxcui, String nombre) {
}
