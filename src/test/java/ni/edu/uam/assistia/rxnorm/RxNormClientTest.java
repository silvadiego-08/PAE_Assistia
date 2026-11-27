package ni.edu.uam.assistia.rxnorm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RxNormClientTest {

    private final RxNormClient client = new RxNormClient();

    @Test
    void limpiaNombresFiltraPacksYCombinacionesYExtraeDosis() {
        String json = """
                {
                  "drugGroup": {
                    "conceptGroup": [
                      {
                        "tty": "BPCK",
                        "conceptProperties": [
                          {"rxcui": "1", "name": "{1 (acetaminophen 500 MG) } Pack"}
                        ]
                      },
                      {
                        "tty": "GPCK",
                        "conceptProperties": [
                          {"rxcui": "2", "name": "{1 (acetaminophen 500 MG) } Pack"}
                        ]
                      },
                      {
                        "tty": "SBD",
                        "conceptProperties": [
                          {"rxcui": "3", "name": "acetaminophen 32 MG/ML Oral Solution [Bactimicina]"},
                          {"rxcui": "4", "name": "acetaminophen 500 MG / chlorpheniramine maleate 2 MG Oral Tablet"}
                        ]
                      },
                      {
                        "tty": "SCD",
                        "conceptProperties": [
                          {"rxcui": "5", "name": "acetaminophen 500 MG Oral Tablet"},
                          {"rxcui": "6", "name": "acetaminophen 500 MG Oral Tablet"},
                          {"rxcui": "7", "name": "acetaminophen 500 MG / aspirin 250 MG Oral Tablet"},
                          {"rxcui": "8", "name": "acetaminophen 500 MG/5 ML Oral Solution"}
                        ]
                      }
                    ]
                  }
                }
                """;

        List<RxNormResultado> resultados = client.parsear(json);

        assertEquals(List.of(
                new RxNormResultado("3", "acetaminophen", "32 MG/ML",
                        "acetaminophen 32 MG/ML Oral Solution [Bactimicina]"),
                new RxNormResultado("5", "acetaminophen", "500 MG",
                        "acetaminophen 500 MG Oral Tablet"),
                new RxNormResultado("8", "acetaminophen", "500 MG/5 ML",
                        "acetaminophen 500 MG/5 ML Oral Solution")
        ), resultados);
    }

    @Test
    void unaRespuestaSinGruposDevuelveListaVacia() {
        assertTrue(client.parsear("{\"drugGroup\":{}}").isEmpty());
        assertTrue(client.parsear("{}").isEmpty());
    }

    @Test
    void unJsonInvalidoLanzaRxNormException() {
        assertThrows(RxNormException.class, () -> client.parsear("esto no es json"));
    }
}
