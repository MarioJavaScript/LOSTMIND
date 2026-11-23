import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PruebaAlgoritmos {
    private PruebaAlgoritmos() {
    }

    public static void main(String[] args) throws IOException, LaberintoInvalidoException {
        Path carpeta = Path.of(args.length == 0 ? "ejemplos" : args[0]);
        List<Path> archivos;
        try (var entradas = Files.list(carpeta)) {
            archivos = entradas.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.getFileName().toString().endsWith(".txt"))
                    .sorted(Comparator.comparing(ruta -> ruta.getFileName().toString()))
                    .toList();
        }
        if (archivos.isEmpty()) {
            throw new AssertionError("No hay ejemplos validos en " + carpeta);
        }

        for (Path archivo : archivos) {
            Laberinto laberinto = Laberinto.cargar(archivo.toString());
            Grafo grafo = new Grafo(laberinto);
            String nombre = archivo.getFileName().toString();

            List<Vertice> dfs = grafo.DFS();
            Resultado resultadoDFS = grafo.getUltimoResultadoDFS();
            verificar(laberinto, nombre, "DFS", dfs, resultadoDFS);

            List<Vertice> bfs = grafo.BFS();
            Resultado resultadoBFS = grafo.getUltimoResultadoBFS();
            verificar(laberinto, nombre, "BFS", bfs, resultadoBFS);

            List<Vertice> dijkstra = grafo.caminoMasCorto();
            Resultado resultadoDijkstra = grafo.getUltimoResultadoDijkstra();
            verificar(laberinto, nombre, "Dijkstra", dijkstra, resultadoDijkstra);

            if (resultadoBFS.isEncontrado() != resultadoDijkstra.isEncontrado()
                    || longitud(resultadoBFS) != longitud(resultadoDijkstra)) {
                throw new AssertionError("BFS y Dijkstra discrepan en " + nombre);
            }
            if (nombre.startsWith("sin_solucion") && (resultadoDFS.isEncontrado()
                    || resultadoBFS.isEncontrado() || resultadoDijkstra.isEncontrado())) {
                throw new AssertionError("Debe indicarse que no existe camino en " + nombre);
            }

            // Repetir las busquedas comprueba que cada ejecucion reinicia su estado.
            if (!dfs.equals(grafo.DFS()) || !bfs.equals(grafo.BFS())
                    || !dijkstra.equals(grafo.caminoMasCorto())) {
                throw new AssertionError("El orden cambio al repetir la busqueda en " + nombre);
            }
        }
        System.out.println("OK: " + archivos.size() + " laberintos comprobados.");
    }

    private static void verificar(Laberinto laberinto, String nombre, String algoritmo,
                                 List<Vertice> devueltos, Resultado resultado) {
        if (!devueltos.equals(resultado.getVisitados()) || devueltos.isEmpty()) {
            throw new AssertionError("Exploracion incorrecta: " + nombre + " " + algoritmo);
        }
        Set<Integer> ids = new HashSet<>();
        for (Vertice vertice : devueltos) {
            if (!ids.add(vertice.getId())
                    || !laberinto.esTransitable(vertice.getFila(), vertice.getColumna())) {
                throw new AssertionError("Visita repetida o invalida: " + nombre + " " + algoritmo);
            }
        }

        int idEntrada = laberinto.getFilaEntrada() * laberinto.getColumnas()
                + laberinto.getColumnaEntrada();
        int idSalida = laberinto.getFilaSalida() * laberinto.getColumnas()
                + laberinto.getColumnaSalida();
        if (devueltos.get(0).getId() != idEntrada) {
            throw new AssertionError("La exploracion no comienza en la entrada: " + nombre);
        }

        List<Vertice> camino = resultado.getCamino();
        if (resultado.isEncontrado()) {
            if (camino.isEmpty() || camino.get(0).getId() != idEntrada
                    || camino.get(camino.size() - 1).getId() != idSalida
                    || devueltos.get(devueltos.size() - 1).getId() != idSalida) {
                throw new AssertionError("Extremos del camino incorrectos: " + nombre
                        + " " + algoritmo);
            }
            for (int indice = 1; indice < camino.size(); indice++) {
                Vertice anterior = camino.get(indice - 1);
                Vertice actual = camino.get(indice);
                int movimientos = Math.abs(anterior.getFila() - actual.getFila())
                        + Math.abs(anterior.getColumna() - actual.getColumna());
                if (movimientos != 1 || !laberinto.esTransitable(actual.getFila(),
                        actual.getColumna())) {
                    throw new AssertionError("Movimiento invalido: " + nombre + " " + algoritmo);
                }
            }
        } else if (!camino.isEmpty() || ids.contains(idSalida)) {
            throw new AssertionError("Camino inexistente mal informado: " + nombre + " " + algoritmo);
        }

        String longitud = resultado.isEncontrado()
                ? Integer.toString(camino.size() - 1) : "sin camino";
        System.out.println(nombre + " | " + algoritmo + " | visitados=" + devueltos.size()
                + " | solucion=" + resultado.isEncontrado() + " | movimientos=" + longitud
                + " | tiempo=" + resultado.getTiempoNanos() + " ns");
    }

    private static int longitud(Resultado resultado) {
        return resultado.isEncontrado() ? resultado.getCamino().size() - 1 : -1;
    }
}
