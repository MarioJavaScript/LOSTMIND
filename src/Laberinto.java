import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

public final class Laberinto {
    private final char[][] matriz;
    private final int filas;
    private final int columnas;
    private final int filaEntrada;
    private final int columnaEntrada;
    private final int filaSalida;
    private final int columnaSalida;

    private Laberinto(char[][] matriz, int filaEntrada, int columnaEntrada,
                      int filaSalida, int columnaSalida) {
        this.matriz = matriz;
        this.filas = matriz.length;
        this.columnas = matriz[0].length;
        this.filaEntrada = filaEntrada;
        this.columnaEntrada = columnaEntrada;
        this.filaSalida = filaSalida;
        this.columnaSalida = columnaSalida;
    }

    public static Laberinto cargar(String ruta) throws LaberintoInvalidoException {
        if (ruta == null || ruta.isEmpty()) {
            throw new LaberintoInvalidoException("La ruta del archivo esta vacia.");
        }

        List<String> renglones;
        try {
            renglones = Files.readAllLines(Path.of(ruta), StandardCharsets.UTF_8);
        } catch (InvalidPathException excepcion) {
            throw new LaberintoInvalidoException("La ruta del archivo no es valida: " + ruta, excepcion);
        } catch (IOException excepcion) {
            throw new LaberintoInvalidoException("No se puede leer el archivo: " + ruta + ". "
                    + excepcion.getMessage(), excepcion);
        }

        if (renglones.isEmpty()) {
            throw new LaberintoInvalidoException("El archivo esta vacio.");
        }

        int columnas = renglones.get(0).length();
        if (columnas == 0) {
            throw new LaberintoInvalidoException("El renglon 1 tiene longitud cero.");
        }

        char[][] matriz = new char[renglones.size()][columnas];
        int cantidadEntradas = 0;
        int cantidadSalidas = 0;
        int filaEntrada = -1;
        int columnaEntrada = -1;
        int filaSalida = -1;
        int columnaSalida = -1;
        int aberturas = 0;
        int primeraFilaBorde = -1;
        int primeraColumnaBorde = -1;
        int segundaFilaBorde = -1;
        int segundaColumnaBorde = -1;

        for (int fila = 0; fila < renglones.size(); fila++) {
            String renglon = renglones.get(fila);
            if (renglon.length() != columnas) {
                throw new LaberintoInvalidoException("El renglon " + (fila + 1)
                        + " tiene " + renglon.length() + " columnas; se esperaban " + columnas + ".");
            }
            for (int columna = 0; columna < columnas; columna++) {
                char celda = renglon.charAt(columna);
                if (!esCaracterValido(celda)) {
                    throw new LaberintoInvalidoException("Caracter invalido en fila " + (fila + 1)
                            + ", columna " + (columna + 1) + ": " + describir(celda) + ".");
                }
                matriz[fila][columna] = celda;

                if (celda == 'E') {
                    cantidadEntradas++;
                    filaEntrada = fila;
                    columnaEntrada = columna;
                } else if (celda == 'S') {
                    cantidadSalidas++;
                    filaSalida = fila;
                    columnaSalida = columna;
                }

                // Cada celda del borde se visita una sola vez.
                if (esTransitable(celda) && (fila == 0 || fila == renglones.size() - 1
                        || columna == 0 || columna == columnas - 1)) {
                    aberturas++;
                    if (aberturas == 1) {
                        primeraFilaBorde = fila;
                        primeraColumnaBorde = columna;
                    } else if (aberturas == 2) {
                        segundaFilaBorde = fila;
                        segundaColumnaBorde = columna;
                    }
                }
            }
        }

        if (cantidadEntradas != 0 || cantidadSalidas != 0) {
            if (cantidadEntradas != 1 || cantidadSalidas != 1) {
                throw new LaberintoInvalidoException("Se requiere exactamente una E y una S; hay "
                        + cantidadEntradas + " E y " + cantidadSalidas + " S.");
            }
        } else {
            if (aberturas != 2) {
                throw new LaberintoInvalidoException("Sin E ni S debe haber exactamente dos posiciones "
                        + "transitables en el borde; hay " + aberturas + ".");
            }
            filaEntrada = primeraFilaBorde;
            columnaEntrada = primeraColumnaBorde;
            filaSalida = segundaFilaBorde;
            columnaSalida = segundaColumnaBorde;
        }

        return new Laberinto(matriz, filaEntrada, columnaEntrada, filaSalida, columnaSalida);
    }

    private static boolean esCaracterValido(char celda) {
        return celda == '+' || celda == '-' || celda == '|' || esTransitable(celda);
    }

    private static boolean esTransitable(char celda) {
        return celda == ' ' || celda == '#' || celda == 'E' || celda == 'S';
    }

    private static String describir(char celda) {
        if (celda == '\t') {
            return "tabulacion";
        }
        return "'" + celda + "'";
    }

    public boolean esTransitable(int fila, int columna) {
        return fila >= 0 && fila < filas && columna >= 0 && columna < columnas
                && esTransitable(matriz[fila][columna]);
    }

    public int getFilas() {
        return filas;
    }

    public int getColumnas() {
        return columnas;
    }

    public int getFilaEntrada() {
        return filaEntrada;
    }

    public int getColumnaEntrada() {
        return columnaEntrada;
    }

    public int getFilaSalida() {
        return filaSalida;
    }

    public int getColumnaSalida() {
        return columnaSalida;
    }

    public char[][] getMatriz() {
        char[][] copia = new char[filas][];
        for (int fila = 0; fila < filas; fila++) {
            copia[fila] = matriz[fila].clone();
        }
        return copia;
    }
}
