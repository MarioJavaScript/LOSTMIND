import java.util.List;
import java.util.Scanner;

public final class Main {
    private enum Algoritmo { DFS, BFS, DIJKSTRA }

    private final Scanner entrada = new Scanner(System.in);
    private Laberinto laberinto;
    private Grafo grafo;

    public static void main(String[] args) {
        new Main().ejecutar();
    }

    private void ejecutar() {
        while (true) {
            imprimirMenu();
            if (!entrada.hasNextLine()) {
                return;
            }
            // strip() solo se aplica a la entrada del menu, nunca a las lineas del laberinto.
            String opcion = entrada.nextLine().strip();
            switch (opcion) {
                case "1" -> cargar();
                case "2" -> resolver(Algoritmo.DFS);
                case "3" -> resolver(Algoritmo.BFS);
                case "4" -> resolver(Algoritmo.DIJKSTRA);
                case "5" -> mostrarTodas();
                case "6" -> {
                    System.out.println("Hasta luego.");
                    return;
                }
                default -> System.out.println("Opcion invalida. Elige un numero del 1 al 6.");
            }
        }
    }

    private void imprimirMenu() {
        System.out.println("\n1. Cargar laberinto");
        System.out.println("2. Resolver laberinto con DFS");
        System.out.println("3. Resolver laberinto con BFS");
        System.out.println("4. Resolver laberinto con Dijkstra (camino mas corto)");
        System.out.println("5. Mostrar las tres soluciones");
        System.out.println("6. Salir");
        System.out.print("Opcion: ");
    }

    private void cargar() {
        System.out.print("Ruta del archivo: ");
        if (!entrada.hasNextLine()) {
            return;
        }
        String ruta = entrada.nextLine();
        // "Copiar como ruta" de Windows agrega comillas en los extremos; se quitan (sin trim).
        if (ruta.length() >= 2 && ruta.startsWith("\"") && ruta.endsWith("\"")) {
            ruta = ruta.substring(1, ruta.length() - 1);
        }
        try {
            Laberinto nuevo = Laberinto.cargar(ruta);
            Grafo nuevoGrafo = new Grafo(nuevo);
            laberinto = nuevo;
            grafo = nuevoGrafo;
            System.out.println("Laberinto cargado: " + laberinto.getFilas()
                    + " filas, " + laberinto.getColumnas() + " columnas.");
        } catch (LaberintoInvalidoException | RuntimeException excepcion) {
            System.out.println("No se pudo cargar el laberinto: " + excepcion.getMessage());
        }
    }

    private void resolver(Algoritmo algoritmo) {
        if (!hayLaberinto()) {
            return;
        }
        List<Vertice> visitados = switch (algoritmo) {
            case DFS -> grafo.DFS();
            case BFS -> grafo.BFS();
            case DIJKSTRA -> grafo.caminoMasCorto();
        };
        Resultado resultado = resultado(algoritmo);
        new Despliegue(laberinto, resultado, entrada).despliegaLaberinto(visitados);
    }

    private void mostrarTodas() {
        if (!hayLaberinto()) {
            return;
        }
        for (Algoritmo algoritmo : Algoritmo.values()) {
            if (resultado(algoritmo) == null) {
                switch (algoritmo) {
                    case DFS -> grafo.DFS();
                    case BFS -> grafo.BFS();
                    case DIJKSTRA -> grafo.caminoMasCorto();
                }
            }
            new Despliegue(laberinto, resultado(algoritmo), entrada)
                    .mostrarFinal(algoritmo == Algoritmo.DIJKSTRA
                            ? "Dijkstra" : algoritmo.name(), false);
        }
    }

    private Resultado resultado(Algoritmo algoritmo) {
        return switch (algoritmo) {
            case DFS -> grafo.getUltimoResultadoDFS();
            case BFS -> grafo.getUltimoResultadoBFS();
            case DIJKSTRA -> grafo.getUltimoResultadoDijkstra();
        };
    }

    private boolean hayLaberinto() {
        if (laberinto != null) {
            return true;
        }
        System.out.println("Primero debes cargar un laberinto.");
        return false;
    }
}
