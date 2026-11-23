public final class Arcos {
    private final int idOrigen;
    private final int idDestino;
    private final int costo;

    public Arcos(int idOrigen, int idDestino) {
        this.idOrigen = idOrigen;
        this.idDestino = idDestino;
        this.costo = 1;
    }

    public int getIdDestino() {
        return idDestino;
    }

    public int getCosto() {
        return costo;
    }
}
