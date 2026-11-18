package ni.edu.uam.assistia.rxnorm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente mínimo para el servicio getDrugs de RxNorm.
 * No conoce pacientes, tratamientos ni credenciales: solo envía el nombre buscado.
 */
public class RxNormClient {

    private static final String URL_BASE =
            "https://rxnav.nlm.nih.gov/REST/drugs.json?name=";

    private final HttpClient http;
    private final ObjectMapper json;

    public RxNormClient() {
        this(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                new ObjectMapper());
    }

    RxNormClient(HttpClient http, ObjectMapper json) {
        this.http = http;
        this.json = json;
    }

    /**
     * Busca medicamentos por nombre y devuelve una lista sin duplicados por rxcui.
     *
     * @throws RxNormException si el término está vacío, falla la red,
     *                         RxNorm responde con error o el JSON no se puede leer.
     */
    public List<RxNormResultado> buscar(String termino) {
        if (termino == null || termino.isBlank()) {
            throw new RxNormException("Escribe el nombre de un medicamento antes de buscar.");
        }

        String nombre = termino.trim();
        String parametro = URLEncoder.encode(nombre, StandardCharsets.UTF_8)
                .replace("+", "%20");
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(URL_BASE + parametro))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "Assistia/1.0")
                .GET()
                .build();

        HttpResponse<String> respuesta;
        try {
            respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        } catch (IOException error) {
            throw new RxNormException(
                    "No se pudo consultar RxNorm. Revisa tu conexión e inténtalo de nuevo.", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new RxNormException("La búsqueda en RxNorm fue interrumpida.", error);
        }

        if (respuesta.statusCode() != 200) {
            throw new RxNormException(
                    "RxNorm respondió con error HTTP " + respuesta.statusCode() + ".");
        }
        return parsear(respuesta.body());
    }

    /**
     * Recorre drugGroup.conceptGroup[].conceptProperties[].
     * Los grupos sin conceptProperties se ignoran y se conserva el primer
     * resultado de cada rxcui.
     */
    List<RxNormResultado> parsear(String cuerpo) {
        try {
            JsonNode grupos = json.readTree(cuerpo)
                    .path("drugGroup")
                    .path("conceptGroup");
            Map<String, RxNormResultado> unicos = new LinkedHashMap<>();

            if (grupos.isArray()) {
                for (JsonNode grupo : grupos) {
                    agregarPropiedades(grupo.path("conceptProperties"), unicos);
                }
            } else if (grupos.isObject()) {
                agregarPropiedades(grupos.path("conceptProperties"), unicos);
            }

            return List.copyOf(unicos.values());
        } catch (IOException error) {
            throw new RxNormException(
                    "RxNorm devolvió una respuesta que no se pudo leer.", error);
        }
    }

    private static void agregarPropiedades(JsonNode propiedades,
                                           Map<String, RxNormResultado> unicos) {
        if (!propiedades.isArray()) {
            return;
        }
        for (JsonNode propiedad : propiedades) {
            String rxcui = texto(propiedad, "rxcui");
            String nombre = texto(propiedad, "name");
            if (rxcui == null || nombre == null) {
                continue;
            }
            unicos.putIfAbsent(rxcui, new RxNormResultado(rxcui, nombre));
        }
    }

    private static String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        String texto = valor.asText().trim();
        return texto.isEmpty() ? null : texto;
    }
}
