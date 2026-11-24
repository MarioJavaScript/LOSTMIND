import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public final class PruebaTema {
    private PruebaTema() {
    }

    public static void main(String[] args) {
        try {
            Path carpeta = Path.of(args.length == 0 ? "ejemplos" : args[0]);
            List<Path> archivos;
            try (var flujo = Files.list(carpeta)) {
                archivos = flujo.filter(Files::isRegularFile)
                        .filter(ruta -> ruta.getFileName().toString().endsWith(".txt"))
                        .sorted(Comparator.comparing(Path::toString)).toList();
            }
            if (archivos.isEmpty()) {
                throw new AssertionError("No hay ejemplos validos.");
            }
            for (Path archivo : archivos) {
                comprobar(Laberinto.cargar(archivo.toString()), archivo.toString());
            }
            System.out.println("OK: " + archivos.size() + " vistas ASCII y Unicode comprobadas.");
        } catch (Exception | AssertionError fallo) {
            System.err.println("FALLO: " + fallo.getMessage());
            System.exit(1);
        }
    }

    private static void comprobar(Laberinto laberinto, String nombre) {
        char[][] original = laberinto.getMatriz();
        Resultado vacio = new Resultado(List.of(), List.of(), false, 0);
        List<String> ascii = new Despliegue(laberinto, vacio, new Scanner(""), TemaConsola.ASCII)
                .vista(Set.of(), -1, Set.of());
        List<String> unicode = new Despliegue(laberinto, vacio, new Scanner(""), TemaConsola.UNICODE)
                .vista(Set.of(), -1, Set.of());
        if (ascii.size() != laberinto.getFilas() || unicode.size() != ascii.size()) {
            throw new AssertionError(nombre + ": numero de filas distinto");
        }
        for (int fila = 0; fila < laberinto.getFilas(); fila++) {
            if (ascii.get(fila).length() != laberinto.getColumnas()
                    || unicode.get(fila).length() != ascii.get(fila).length()) {
                throw new AssertionError(nombre + ": numero de columnas distinto en fila " + fila);
            }
            for (int columna = 0; columna < laberinto.getColumnas(); columna++) {
                char a = ascii.get(fila).charAt(columna);
                char u = unicode.get(fila).charAt(columna);
                boolean paredAscii = a == '+' || a == '-' || a == '|';
                boolean paredUnicode = "─│┌┐└┘├┤┬┴┼".indexOf(u) >= 0;
                if (paredAscii != paredUnicode
                        || paredAscii == laberinto.esTransitable(fila, columna)) {
                    throw new AssertionError(nombre + ": pared o transitable cambiado en "
                            + fila + "," + columna);
                }
                if (original[fila][columna] == '+' && u == '+') {
                    throw new AssertionError(nombre + ": '+' sin convertir en " + fila + "," + columna);
                }
                comprobarEsquina(original, fila, columna, u, nombre);
            }
        }
        char[][] despues = laberinto.getMatriz();
        for (int fila = 0; fila < original.length; fila++) {
            if (!java.util.Arrays.equals(original[fila], despues[fila])) {
                throw new AssertionError(nombre + ": la matriz original cambio");
            }
        }
    }

    private static void comprobarEsquina(char[][] matriz, int fila, int columna,
                                         char visible, String nombre) {
        if (matriz[fila][columna] != '+') return;
        boolean arriba = pared(matriz, fila - 1, columna);
        boolean derecha = pared(matriz, fila, columna + 1);
        boolean abajo = pared(matriz, fila + 1, columna);
        boolean izquierda = pared(matriz, fila, columna - 1);
        char esperada = 0;
        if (arriba && derecha && !abajo && !izquierda) esperada = '└';
        if (arriba && izquierda && !abajo && !derecha) esperada = '┘';
        if (abajo && derecha && !arriba && !izquierda) esperada = '┌';
        if (abajo && izquierda && !arriba && !derecha) esperada = '┐';
        if (esperada != 0 && visible != esperada) {
            throw new AssertionError(nombre + ": esquina incorrecta en " + fila + "," + columna);
        }
    }

    private static boolean pared(char[][] matriz, int fila, int columna) {
        return fila >= 0 && fila < matriz.length && columna >= 0
                && columna < matriz[fila].length
                && "+-|".indexOf(matriz[fila][columna]) >= 0;
    }
}
