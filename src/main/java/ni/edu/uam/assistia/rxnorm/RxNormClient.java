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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente para el servicio getDrugs de RxNorm.
 * Descarta presentaciones compuestas (packs y combinaciones) y devuelve
 * como máximo diez resultados con un nombre limpio y su dosis.
 */
public class RxNormClient {

    private static final String URL_BASE =
            "https://rxnav.nlm.nih.gov/REST/drugs.json?name=";
    private static final int MAX_RESULTADOS = 8;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("SCD", "SBD");

    /** Captura la dosis, por ejemplo "500 MG", "32 MG/ML" o "500 MG/5 ML". */
    private static final Pattern DOSIS = Pattern.compile(
            "(?i)\\b\\d+(?:[.,]\\d+)?\\s*"
                    + "(?:MG/ML|MCG/ML|MG/G|MG|MCG|G|ML|%)"
                    + "(?:\\s*/\\s*\\d+(?:[.,]\\d+)?\\s*(?:MG|MCG|G|ML))?");

    /** Quita prefijos como "8 HR" o "12 H". */
    private static final Pattern PREFIJO = Pattern.compile(
            "(?i)^\\d+(?:[.,]\\d+)?\\s*(?:HR|H)\\s+");

    /** Palabras de forma farmacéutica que no aportan al nombre corto. */
    private static final Pattern FORMA = Pattern.compile(
            "(?i)\\b(oral|tablet|tableta|capsule|cápsula|solution|solución|"
                    + "suspension|suspensión|injection|inyección|extended release|"
                    + "delayed release|chewable|film coated|granules|pack|"
                    + "suppository|supositorio|ophthalmic|oftálmico|powder|polvo|"
                    + "inhalation|inhalación|nasal|rectal|topical|tópico|cream|crema|"
                    + "gel|patch|parche|drops|gotas)\\b");

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
     * Solo usa los grupos SCD (genérico) y SBD (marca), que son medicamentos
     * concretos. Descarta BPCK/GPCK (packs) y nombres con varios principios
     * activos. Limita a diez resultados y quita duplicados por nombre + dosis.
     */
    List<RxNormResultado> parsear(String cuerpo) {
        try {
            JsonNode grupos = json.readTree(cuerpo)
                    .path("drugGroup")
                    .path("conceptGroup");
            Map<String, RxNormResultado> unicos = new LinkedHashMap<>();

            if (grupos.isArray()) {
                for (JsonNode grupo : grupos) {
                    agregarPropiedades(grupo.path("conceptProperties"),
                            texto(grupo, "tty"), unicos);
                }
            } else if (grupos.isObject()) {
                agregarPropiedades(grupos.path("conceptProperties"),
                        texto(grupos, "tty"), unicos);
            }

            return unicos.values().stream().limit(MAX_RESULTADOS).toList();
        } catch (IOException error) {
            throw new RxNormException(
                    "RxNorm devolvió una respuesta que no se pudo leer.", error);
        }
    }

    private static void agregarPropiedades(JsonNode propiedades, String tty,
                                           Map<String, RxNormResultado> unicos) {
        if (!propiedades.isArray() || tty == null || !TIPOS_PERMITIDOS.contains(tty)) {
            return;
        }
        for (JsonNode propiedad : propiedades) {
            String rxcui = texto(propiedad, "rxcui");
            String original = texto(propiedad, "name");
            if (rxcui == null || original == null || esCombinado(original)) {
                continue;
            }

            String dosis = extraerDosis(original);
            String nombre = limpiarNombre(original);
            if (nombre.isBlank()) {
                continue;
            }

            String clave = (nombre + "|" + dosis).toLowerCase(Locale.ROOT);
            unicos.putIfAbsent(clave,
                    new RxNormResultado(rxcui, nombre, dosis, original));
        }
    }

    private static boolean esCombinado(String nombre) {
        return nombre.contains("{")
                || nombre.contains("}")
                || nombre.contains(" / ");
    }

    private static String extraerDosis(String nombre) {
        Matcher matcher = DOSIS.matcher(nombre);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String limpiarNombre(String nombre) {
        String sinMarca = nombre.replaceAll("\\s*\\[[^\\]]*\\]", " ");
        String sinPrefijo = PREFIJO.matcher(sinMarca).replaceFirst(" ");
        String sinDosis = DOSIS.matcher(sinPrefijo).replaceFirst(" ");
        String limpio = FORMA.matcher(sinDosis)
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
        limpio = limpio.replaceAll("(?i)\\s+for$", "");
        limpio = limpio.replaceAll("^[\\s,;./-]+|[\\s,;./-]+$", "");
        return limpio.isBlank() ? sinPrefijo.replaceAll("\\s+", " ").trim() : limpio;
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
