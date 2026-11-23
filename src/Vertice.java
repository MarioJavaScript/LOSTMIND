import java.util.Objects;

public final class Vertice {
    private final int id;
    private final int fila;
    private final int columna;

    public Vertice(int id, int fila, int columna) {
        this.id = id;
        this.fila = fila;
        this.columna = columna;
    }

    public int getId() {
        return id;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Vertice otro)) {
            return false;
        }
        return id == otro.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Vertice{id=" + id + ", fila=" + fila + ", columna=" + columna + "}";
    }
}
