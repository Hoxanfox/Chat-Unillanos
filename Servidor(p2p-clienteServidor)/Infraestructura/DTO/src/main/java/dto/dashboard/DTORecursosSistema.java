package dto.dashboard;

/**
 * DTO para encapsular datos de recursos del sistema
 * Usado para transferir información entre capas sin acoplar al gestor
 */
public class DTORecursosSistema {
    private final long memoriaUsada;
    private final long memoriaTotal;
    private final double cpuUsage;

    public DTORecursosSistema(long memoriaUsada, long memoriaTotal, double cpuUsage) {
        this.memoriaUsada = memoriaUsada;
        this.memoriaTotal = memoriaTotal;
        this.cpuUsage = cpuUsage;
    }

    public long getMemoriaUsada() {
        return memoriaUsada;
    }

    public long getMemoriaTotal() {
        return memoriaTotal;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public double getPorcentajeMemoria() {
        return memoriaTotal > 0 ? (memoriaUsada * 100.0) / memoriaTotal : 0;
    }
}

