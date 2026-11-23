import java.nio.file.Path;

public final class PruebaCarga {
    private PruebaCarga() {
    }

    public static void main(String[] args) {
        String base = args.length == 0 ? "ejemplos" : args[0];
        int aprobados = 0;

        aprobados += probarValido(base, "con_marcas.txt", 5, 7, 0, 2, 4, 4);
        aprobados += probarValido(base, "por_perimetro.txt", 5, 5, 0, 2, 4, 2);
        aprobados += probarValido(base, "no_cuadrado.txt", 3, 9, 0, 4, 2, 4);
        aprobados += probarValido(base, "con_numerales.txt", 5, 5, 0, 2, 4, 2);
        aprobados += probarValido(base, "sin_solucion.txt", 5, 5, 0, 2, 4, 2);
        aprobados += probarValido(base, "sin_solucion_exploracion.txt", 12, 30, 0, 2, 11, 2);
        aprobados += probarValido(base, "espacios_finales.txt", 3, 6, 0, 2, 2, 2);
        aprobados += probarValido(base, "espacios_extremos.txt", 3, 6, 0, 1, 2, 1);
        aprobados += probarValido(base, "grande_25x60.txt", 25, 60, 0, 1, 24, 58);
        aprobados += probarValido(base, "minimo.txt", 1, 2, 0, 0, 0, 1);

        aprobados += probarInvalido(base, "renglones_distintos.txt", "columnas");
        aprobados += probarInvalido(base, "caracter_invalido.txt", "Caracter invalido");
        aprobados += probarInvalido(base, "tabulacion.txt", "tabulacion");
        aprobados += probarInvalido(base, "dos_e.txt", "2 E");
        aprobados += probarInvalido(base, "e_sin_s.txt", "una E y una S");
        aprobados += probarInvalido(base, "perimetro_tres.txt", "hay 3");
        aprobados += probarInvalido(base, "vacio.txt", "vacio");

        System.out.println("OK: " + aprobados + " ejemplos comprobados.");
    }

    private static int probarValido(String base, String archivo, int filas, int columnas,
                                   int filaEntrada, int columnaEntrada,
                                   int filaSalida, int columnaSalida) {
        String ruta = Path.of(base, archivo).toString();
        try {
            Laberinto laberinto = Laberinto.cargar(ruta);
            if (laberinto.getFilas() != filas || laberinto.getColumnas() != columnas
                    || laberinto.getFilaEntrada() != filaEntrada
                    || laberinto.getColumnaEntrada() != columnaEntrada
                    || laberinto.getFilaSalida() != filaSalida
                    || laberinto.getColumnaSalida() != columnaSalida
                    || !laberinto.esTransitable(filaEntrada, columnaEntrada)
                    || !laberinto.esTransitable(filaSalida, columnaSalida)) {
                throw new AssertionError("Dimensiones o entrada/salida incorrectas: " + ruta);
            }
            if (archivo.equals("con_numerales.txt") && !laberinto.esTransitable(1, 1)) {
                throw new AssertionError("El numeral debe ser transitable: " + ruta);
            }
            if (archivo.equals("espacios_finales.txt")
                    && laberinto.getMatriz()[0][columnas - 1] != ' ') {
                throw new AssertionError("Se perdio el espacio final: " + ruta);
            }
            if (archivo.equals("espacios_extremos.txt")) {
                for (char[] renglon : laberinto.getMatriz()) {
                    if (renglon[0] != ' ' || renglon[columnas - 1] != ' ') {
                        throw new AssertionError("Se perdio un espacio extremo: " + ruta);
                    }
                }
            }
            System.out.println("OK " + ruta);
            return 1;
        } catch (LaberintoInvalidoException excepcion) {
            throw new AssertionError("Se rechazo un ejemplo valido: " + ruta + ": "
                    + excepcion.getMessage(), excepcion);
        }
    }

    private static int probarInvalido(String base, String archivo, String motivoEsperado) {
        String ruta = Path.of(base, "invalidos", archivo).toString();
        try {
            Laberinto.cargar(ruta);
            throw new AssertionError("Se acepto un ejemplo invalido: " + ruta);
        } catch (LaberintoInvalidoException excepcion) {
            if (!excepcion.getMessage().contains(motivoEsperado)) {
                throw new AssertionError("Motivo inesperado para " + ruta + ": "
                        + excepcion.getMessage(), excepcion);
            }
            System.out.println("OK rechazo " + ruta + ": " + excepcion.getMessage());
            return 1;
        }
    }
}
