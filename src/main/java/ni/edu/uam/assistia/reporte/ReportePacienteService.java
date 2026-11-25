
package ni.edu.uam.assistia.reporte;

import ni.edu.uam.assistia.model.DetalleTratamiento;
import ni.edu.uam.assistia.model.Paciente;
import ni.edu.uam.assistia.model.Tratamiento;
import ni.edu.uam.assistia.util.Tx;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportePacienteService {

    private static final String PLANTILLA =
            "/ni/edu/uam/assistia/reportes/expediente-paciente.jrxml";

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private JasperReport plantillaCompilada;

    public JasperPrint generarReporte(Long pacienteId) {

        if (pacienteId == null || pacienteId <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un paciente válido.");
        }

        try {
            DatosReporte datos = obtenerDatos(pacienteId);

            JasperReport reporte = obtenerPlantilla();

            Map<String, Object> parametros = new HashMap<>();

            parametros.put("NOMBRE_PACIENTE",
                    datos.paciente().getNombre());

            parametros.put("FECHA_NACIMIENTO",
                    FECHA.format(datos.paciente().getFechaNacimiento()));

            parametros.put("TELEFONO",
                    datos.paciente().getTelefono() == null
                            ? "No registrado"
                            : datos.paciente().getTelefono());

            parametros.put("GENERADO",
                    FECHA_HORA.format(LocalDateTime.now()));

            JRBeanCollectionDataSource fuente =
                    new JRBeanCollectionDataSource(datos.filas());

            return JasperFillManager.fillReport(
                    reporte,
                    parametros,
                    fuente
            );

        } catch (JRException | IOException error) {
            throw new ReporteException(
                    "No se pudo generar el expediente del paciente.",
                    error
            );
        }
    }

    public Path exportarPdf(JasperPrint impreso, Path destino) {

        if (impreso == null) {
            throw new IllegalArgumentException(
                    "No existe un reporte generado.");
        }

        if (destino == null) {
            throw new IllegalArgumentException(
                    "Seleccione dónde guardar el archivo PDF.");
        }

        try {
            Path ruta = destino.toAbsolutePath();

            if (ruta.getParent() != null) {
                Files.createDirectories(ruta.getParent());
            }

            JasperExportManager.exportReportToPdfFile(
                    impreso,
                    ruta.toString()
            );

            return ruta;

        } catch (JRException | IOException error) {
            throw new ReporteException(
                    "No se pudo exportar el expediente PDF.",
                    error
            );
        }
    }

    public Path generarPdf(Long pacienteId, Path destino) {

        if (destino == null) {
            throw new IllegalArgumentException(
                    "Seleccione dónde guardar el archivo PDF.");
        }

        return exportarPdf(
                generarReporte(pacienteId),
                destino
        );
    }

    private DatosReporte obtenerDatos(Long pacienteId) {

        return Tx.leer(session -> {

            Paciente paciente = session.find(
                    Paciente.class,
                    pacienteId
            );

            if (paciente == null) {
                throw new IllegalArgumentException(
                        "El paciente seleccionado no existe.");
            }

            List<Tratamiento> tratamientos =
                    session.createSelectionQuery("""
                            SELECT DISTINCT t
                            FROM Tratamiento t
                            JOIN FETCH t.paciente
                            JOIN FETCH t.medico
                            LEFT JOIN FETCH t.detalles d
                            LEFT JOIN FETCH d.medicamento
                            WHERE t.paciente.id = :pacienteId
                            ORDER BY t.fechaInicio DESC
                            """, Tratamiento.class)
                            .setParameter("pacienteId", pacienteId)
                            .getResultList();

            List<FilaPacienteReporte> filas = new ArrayList<>();

            for (Tratamiento tratamiento : tratamientos) {

                if (tratamiento.getDetalles() == null
                        || tratamiento.getDetalles().isEmpty()) {

                    filas.add(new FilaPacienteReporte(
                            "Sin medicamentos registrados",
                            "—",
                            "—",
                            "—",
                            "—",
                            "Sin instrucciones",
                            FECHA.format(tratamiento.getFechaInicio()),
                            tratamiento.getFechaFin() == null
                                    ? "Sin fecha final"
                                    : FECHA.format(tratamiento.getFechaFin()),
                            tratamiento.getMedico().getNombre(),
                            tratamiento.getMedico().getEspecialidad(),
                            tratamiento.isActivo() ? "Activo" : "Inactivo"
                    ));

                    continue;
                }

                for (DetalleTratamiento detalle : tratamiento.getDetalles()) {

                    var medicamento = detalle.getMedicamento();

                    filas.add(new FilaPacienteReporte(
                            medicamento.getNombre(),
                            medicamento.getConcentracion(),
                            medicamento.getPresentacion(),
                            detalle.getDosisIndicada().toPlainString(),
                            detalle.getUnidad(),
                            detalle.getInstrucciones(),
                            FECHA.format(tratamiento.getFechaInicio()),
                            tratamiento.getFechaFin() == null
                                    ? "Sin fecha final"
                                    : FECHA.format(tratamiento.getFechaFin()),
                            tratamiento.getMedico().getNombre(),
                            tratamiento.getMedico().getEspecialidad(),
                            tratamiento.isActivo() ? "Activo" : "Inactivo"
                    ));
                }
            }

            return new DatosReporte(paciente, filas);
        });
    }

    private synchronized JasperReport obtenerPlantilla()
            throws JRException, IOException {

        if (plantillaCompilada == null) {

            try (InputStream entrada =
                         ReportePacienteService.class
                                 .getResourceAsStream(PLANTILLA)) {

                if (entrada == null) {
                    throw new ReporteException(
                            "No se encontró la plantilla: " + PLANTILLA);
                }

                plantillaCompilada =
                        JasperCompileManager.compileReport(entrada);
            }
        }

        return plantillaCompilada;
    }

    private record DatosReporte(
            Paciente paciente,
            List<FilaPacienteReporte> filas
    ) {
    }
}
