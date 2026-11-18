
package ni.edu.uam.assistia.reporte;

public class FilaPacienteReporte {

    private final String medicamento;
    private final String concentracion;
    private final String presentacion;
    private final String dosis;
    private final String unidad;
    private final String instrucciones;
    private final String fechaInicio;
    private final String fechaFin;
    private final String medico;
    private final String especialidad;
    private final String estado;

    public FilaPacienteReporte(
            String medicamento,
            String concentracion,
            String presentacion,
            String dosis,
            String unidad,
            String instrucciones,
            String fechaInicio,
            String fechaFin,
            String medico,
            String especialidad,
            String estado) {

        this.medicamento = valor(medicamento);
        this.concentracion = valor(concentracion);
        this.presentacion = valor(presentacion);
        this.dosis = valor(dosis);
        this.unidad = valor(unidad);
        this.instrucciones = valor(instrucciones);
        this.fechaInicio = valor(fechaInicio);
        this.fechaFin = valor(fechaFin);
        this.medico = valor(medico);
        this.especialidad = valor(especialidad);
        this.estado = valor(estado);
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "—" : texto;
    }

    public String getMedicamento() {
        return medicamento;
    }

    public String getConcentracion() {
        return concentracion;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public String getDosis() {
        return dosis;
    }

    public String getUnidad() {
        return unidad;
    }

    public String getInstrucciones() {
        return instrucciones;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public String getMedico() {
        return medico;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getEstado() {
        return estado;
    }
}
