import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

// Se eligieron listas de adyacencia para guardar solo los vecinos transitables.
public final class Grafo {
    private record Paso(int id, int padre) { }
    private record Distancia(int id, long valor) { }

    private final Vertices vertices = new Vertices();
    private final Map<Integer, List<Arcos>> adyacencia = new HashMap<>();
    private final int idEntrada;
    private final int idSalida;
    private Resultado ultimoResultadoDFS;
    private Resultado ultimoResultadoBFS;
    private Resultado ultimoResultadoDijkstra;

    public Grafo(Laberinto lab) {
        Objects.requireNonNull(lab, "El laberinto no puede ser nulo");
        int columnas = lab.getColumnas();
        for (int fila = 0; fila < lab.getFilas(); fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                if (lab.esTransitable(fila, columna)) {
                    int id = fila * columnas + columna;
                    vertices.agregar(new Vertice(id, fila, columna));
                }
            }
        }

        for (int id : vertices.idsOrdenados()) {
            Vertice vertice = vertices.obtener(id);
            int fila = vertice.getFila();
            int columna = vertice.getColumna();
            List<Arcos> vecinos = new ArrayList<>(4);
            agregarVecino(lab, vecinos, id, fila - 1, columna);
            agregarVecino(lab, vecinos, id, fila, columna + 1);
            agregarVecino(lab, vecinos, id, fila + 1, columna);
            agregarVecino(lab, vecinos, id, fila, columna - 1);
            adyacencia.put(id, vecinos);
        }

        idEntrada = lab.getFilaEntrada() * columnas + lab.getColumnaEntrada();
        idSalida = lab.getFilaSalida() * columnas + lab.getColumnaSalida();
    }

    private void agregarVecino(Laberinto lab, List<Arcos> vecinos, int origen,
                               int fila, int columna) {
        if (lab.esTransitable(fila, columna)) {
            vecinos.add(new Arcos(origen, fila * lab.getColumnas() + columna));
        }
    }

    public List<Vertice> DFS() {
        long inicio = System.nanoTime();
        List<Vertice> visitados = new ArrayList<>();
        Set<Integer> vistos = new HashSet<>();
        Map<Integer, Integer> predecesores = new HashMap<>();
        Deque<Paso> pila = new ArrayDeque<>();
        pila.push(new Paso(idEntrada, -1));
        boolean encontrado = false;

        while (!pila.isEmpty()) {
            Paso paso = pila.pop();
            if (!vistos.add(paso.id())) {
                continue;
            }
            predecesores.put(paso.id(), paso.padre());
            visitados.add(vertices.obtener(paso.id()));
            if (paso.id() == idSalida) {
                encontrado = true;
                break;
            }

            List<Arcos> vecinos = adyacencia.get(paso.id());
            // La pila recibe izquierda, abajo, derecha y arriba.
            for (int indice = vecinos.size() - 1; indice >= 0; indice--) {
                int destino = vecinos.get(indice).getIdDestino();
                if (!vistos.contains(destino)) {
                    pila.push(new Paso(destino, paso.id()));
                }
            }
        }

        List<Vertice> camino = reconstruir(predecesores, encontrado);
        ultimoResultadoDFS = new Resultado(visitados, camino, encontrado,
                System.nanoTime() - inicio);
        return ultimoResultadoDFS.getVisitados();
    }

    public List<Vertice> BFS() {
        long inicio = System.nanoTime();
        List<Vertice> visitados = new ArrayList<>();
        Set<Integer> descubiertos = new HashSet<>();
        Map<Integer, Integer> predecesores = new HashMap<>();
        Queue<Integer> cola = new ArrayDeque<>();
        descubiertos.add(idEntrada);
        predecesores.put(idEntrada, -1);
        cola.add(idEntrada);
        boolean encontrado = false;

        while (!cola.isEmpty()) {
            int id = cola.remove();
            visitados.add(vertices.obtener(id));
            if (id == idSalida) {
                encontrado = true;
                break;
            }
            for (Arcos arco : adyacencia.get(id)) {
                int destino = arco.getIdDestino();
                if (descubiertos.add(destino)) {
                    predecesores.put(destino, id);
                    cola.add(destino);
                }
            }
        }

        List<Vertice> camino = reconstruir(predecesores, encontrado);
        ultimoResultadoBFS = new Resultado(visitados, camino, encontrado,
                System.nanoTime() - inicio);
        return ultimoResultadoBFS.getVisitados();
    }

    public List<Vertice> caminoMasCorto() {
        long inicio = System.nanoTime();
        List<Vertice> visitados = new ArrayList<>();
        Map<Integer, Long> distancias = new HashMap<>();
        Map<Integer, Integer> predecesores = new HashMap<>();
        Set<Integer> definitivos = new HashSet<>();
        for (int id : vertices.idsOrdenados()) {
            distancias.put(id, Long.MAX_VALUE);
        }
        distancias.put(idEntrada, 0L);
        predecesores.put(idEntrada, -1);

        PriorityQueue<Distancia> pendientes = new PriorityQueue<>(
                Comparator.comparingLong(Distancia::valor).thenComparingInt(Distancia::id));
        pendientes.add(new Distancia(idEntrada, 0));
        boolean encontrado = false;

        while (!pendientes.isEmpty()) {
            Distancia actual = pendientes.remove();
            if (actual.valor() != distancias.get(actual.id())
                    || !definitivos.add(actual.id())) {
                continue;
            }
            visitados.add(vertices.obtener(actual.id()));
            if (actual.id() == idSalida) {
                encontrado = true;
                break;
            }
            for (Arcos arco : adyacencia.get(actual.id())) {
                int destino = arco.getIdDestino();
                long nuevaDistancia = actual.valor() + arco.getCosto();
                if (nuevaDistancia < distancias.get(destino)) {
                    distancias.put(destino, nuevaDistancia);
                    predecesores.put(destino, actual.id());
                    pendientes.add(new Distancia(destino, nuevaDistancia));
                }
            }
        }

        List<Vertice> camino = reconstruir(predecesores, encontrado);
        ultimoResultadoDijkstra = new Resultado(visitados, camino, encontrado,
                System.nanoTime() - inicio);
        return ultimoResultadoDijkstra.getVisitados();
    }

    private List<Vertice> reconstruir(Map<Integer, Integer> predecesores,
                                      boolean encontrado) {
        if (!encontrado) {
            return List.of();
        }
        List<Vertice> camino = new ArrayList<>();
        for (int id = idSalida; id != -1; id = predecesores.get(id)) {
            camino.add(vertices.obtener(id));
        }
        Collections.reverse(camino);
        return camino;
    }

    public Resultado getUltimoResultadoDFS() {
        return ultimoResultadoDFS;
    }

    public Resultado getUltimoResultadoBFS() {
        return ultimoResultadoBFS;
    }

    public Resultado getUltimoResultadoDijkstra() {
        return ultimoResultadoDijkstra;
    }
}
