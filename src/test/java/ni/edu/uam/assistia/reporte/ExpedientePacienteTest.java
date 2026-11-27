package ni.edu.uam.assistia.reporte;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpedientePacienteTest {
    @Test
    void conservaLaIdentificacionConYSinTratamientos() throws Exception {
        try (var entrada = ReportePacienteService.class.getResourceAsStream(
                "/ni/edu/uam/assistia/reportes/expediente-paciente.jrxml")) {
            var plantilla = JasperCompileManager.compileReport(entrada);
            String aviso = "Este paciente no tiene tratamientos registrados.";
            var vacio = textos(plantilla, List.of());
            assertTrue(vacio.contains("Nombres y apellidos: Paciente de prueba"));
            assertTrue(vacio.contains("Fecha de nacimiento: 01/02/2000"));
            assertTrue(vacio.contains("Teléfono de emergencia: No registrado"));
            assertTrue(vacio.contains("Alergias: No registradas"));
            assertTrue(vacio.contains("Historial médico: No registrado"));
            assertTrue(vacio.contains("Elaborado por: Equipo Assistia"));
            assertTrue(vacio.contains(aviso));

            var conTratamiento = textos(plantilla, List.of(Map.of("medicamento", "Producto de prueba")));
            assertTrue(conTratamiento.contains("Nombres y apellidos: Paciente de prueba"));
            assertTrue(conTratamiento.contains("Producto de prueba"));
            assertFalse(conTratamiento.contains(aviso));
        }
    }

    private List<String> textos(JasperReport plantilla, List<Map<String, ?>> filas) throws JRException {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("NOMBRE_PACIENTE", "Paciente de prueba");
        parametros.put("FECHA_NACIMIENTO", "01/02/2000");
        parametros.put("TELEFONO", "No registrado");
        parametros.put("ALERGIAS", "No registradas");
        parametros.put("HISTORIAL_MEDICO", "No registrado");
        parametros.put("ELABORADO_POR", "Equipo Assistia");
        JasperPrint impreso = JasperFillManager.fillReport(plantilla, parametros, new JRMapCollectionDataSource(filas));
        assertEquals(1, impreso.getPages().size());
        return impreso.getPages().getFirst().getElements().stream()
                .filter(JRPrintText.class::isInstance)
                .map(JRPrintText.class::cast)
                .map(JRPrintText::getFullText)
                .toList();
    }
}
