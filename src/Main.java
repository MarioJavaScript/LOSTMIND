import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public final class Main {
    private enum Algoritmo { DFS, BFS, DIJKSTRA }

    private final Scanner entrada = new Scanner(System.in);
    private Laberinto laberinto;
    private Grafo grafo;
    private TemaConsola tema = TemaConsola.ASCII;

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Main programa = new Main();
        if (List.of(args).contains("--unicode")) {
            programa.tema = TemaConsola.UNICODE;
        } else {
            programa.elegirTema();
        }
        programa.ejecutar();
    }

    private void elegirTema() {
        System.out.print("Estilo de símbolos: 1) ASCII  2) Unicode (ENTER = ASCII)\nOpcion: ");
        if (entrada.hasNextLine() && entrada.nextLine().strip().equals("2")) {
            tema = TemaConsola.UNICODE;
        }
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
        System.out.print("Ruta del archivo (ENTER vacio para elegir un ejemplo): ");
        if (!entrada.hasNextLine()) {
            return;
        }
        String ruta = entrada.nextLine();
        if (ruta.isEmpty()) {
            ruta = elegirEjemplo();
            if (ruta == null) {
                return;
            }
        }
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

    // Lista los .txt de ejemplos/ (y ejemplos/invalidos/) y devuelve la ruta elegida.
    private String elegirEjemplo() {
        List<Path> archivos;
        try (var flujo = Files.walk(Path.of("ejemplos"), 2)) {
            archivos = flujo.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.getFileName().toString().endsWith(".txt"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException | RuntimeException excepcion) {
            System.out.println("No se pudo leer la carpeta ejemplos: " + excepcion.getMessage());
            return null;
        }
        if (archivos.isEmpty()) {
            System.out.println("No hay ejemplos en la carpeta ejemplos.");
            return null;
        }
        for (int indice = 0; indice < archivos.size(); indice++) {
            System.out.println((indice + 1) + ") " + archivos.get(indice).toString().replace(java.io.File.separatorChar, '/'));
        }
        System.out.print("Numero de ejemplo: ");
        if (!entrada.hasNextLine()) {
            return null;
        }
        try {
            int numero = Integer.parseInt(entrada.nextLine().strip());
            if (numero >= 1 && numero <= archivos.size()) {
                return archivos.get(numero - 1).toString();
            }
        } catch (NumberFormatException excepcion) {
            // Se informa abajo.
        }
        System.out.println("Ejemplo invalido; no se cargo nada.");
        return null;
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
        Despliegue despliegue = new Despliegue(laberinto, resultado, entrada, tema);
        despliegue.despliegaLaberinto(visitados);
        tema = despliegue.getTema();
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
            Despliegue despliegue = new Despliegue(laberinto, resultado(algoritmo), entrada, tema);
            despliegue.mostrarFinal(algoritmo == Algoritmo.DIJKSTRA
                    ? "Dijkstra" : algoritmo.name(), false);
            tema = despliegue.getTema();
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
