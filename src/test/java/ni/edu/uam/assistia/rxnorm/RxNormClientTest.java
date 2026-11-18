package ni.edu.uam.assistia.rxnorm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RxNormClientTest {

    private final RxNormClient client = new RxNormClient();

    @Test
    void eliminaDuplicadosPorRxcuiYConservaElOrden() {
        String json = """
                {
                  "drugGroup": {
                    "conceptGroup": [
                      {
                        "tty": "BPCK",
                        "conceptProperties": [
                          {"rxcui": "750149", "name": "Z-PAK"},
                          {"rxcui": "750157", "name": "TRI-PAK"}
                        ]
                      },
                      {"tty": "GPCK"},
                      {
                        "tty": "SCD",
                        "conceptProperties": [
                          {"rxcui": "750149", "name": "Z-PAK repetido"},
                          {"rxcui": "105260", "name": "Suspensión oral"}
                        ]
                      },
                      {
                        "tty": "SBD",
                        "conceptProperties": [
                          {"rxcui": null, "name": "Sin identificador"},
                          {"rxcui": "999", "name": "   "}
                        ]
                      }
                    ]
                  }
                }
                """;

        List<RxNormResultado> resultados = client.parsear(json);

        assertEquals(List.of(
                new RxNormResultado("750149", "Z-PAK"),
                new RxNormResultado("750157", "TRI-PAK"),
                new RxNormResultado("105260", "Suspensión oral")
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
