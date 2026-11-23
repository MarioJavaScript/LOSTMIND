import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

public final class Vertices {
    private final Hashtable<Integer, Vertice> vertices = new Hashtable<>();

    public void agregar(Vertice vertice) {
        Objects.requireNonNull(vertice, "El vertice no puede ser nulo");
        if (vertices.putIfAbsent(vertice.getId(), vertice) != null) {
            throw new IllegalArgumentException("Ya existe un vertice con id " + vertice.getId());
        }
    }

    public Vertice obtener(int id) {
        return vertices.get(id);
    }

    public boolean contiene(int id) {
        return vertices.containsKey(id);
    }

    public int tamano() {
        return vertices.size();
    }

    public List<Integer> idsOrdenados() {
        List<Integer> ids = new ArrayList<>(vertices.keySet());
        Collections.sort(ids);
        return ids;
    }
}
