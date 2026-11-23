import java.util.List;

public final class Resultado {
    private final List<Vertice> visitados;
    private final List<Vertice> camino;
    private final boolean encontrado;
    private final long tiempoNanos;

    public Resultado(List<Vertice> visitados, List<Vertice> camino,
                     boolean encontrado, long tiempoNanos) {
        this.visitados = List.copyOf(visitados);
        this.camino = List.copyOf(camino);
        this.encontrado = encontrado;
        this.tiempoNanos = tiempoNanos;
    }

    public List<Vertice> getVisitados() {
        return visitados;
    }

    public List<Vertice> getCamino() {
        return camino;
    }

    public boolean isEncontrado() {
        return encontrado;
    }

    public long getTiempoNanos() {
        return tiempoNanos;
    }
}
