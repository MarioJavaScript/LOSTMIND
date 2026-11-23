import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public final class Despliegue {
    private static final String LEYENDA = "Pared: + - |  Transitable: espacio  "
            + "Entrada: E  Salida: S  Visitado: .  Actual: @  Camino final: *";

    private static final long PAUSA_MAXIMA = 5000;

    private final Laberinto laberinto;
    private final Resultado resultado;
    private final Scanner entrada;
    private final char[][] matriz;
    private final boolean usarAnsi;

    public Despliegue(Laberinto laberinto, Resultado resultado, Scanner entrada) {
        this.laberinto = laberinto;
        this.resultado = resultado;
        this.entrada = entrada;
        this.matriz = laberinto.getMatriz();
        this.usarAnsi = System.console() != null && System.getenv("NO_COLOR") == null;
    }

    public void despliegaLaberinto(List<Vertice> ordenVisita) {
        String control = pedirControl();
        boolean saltar = control.equalsIgnoreCase("f");
        boolean manual = control.isEmpty();
        long pausa = manual || saltar ? 0 : Long.parseLong(control);
        Set<Integer> visitados = new HashSet<>();

        for (int paso = 0; paso < ordenVisita.size() && !saltar; paso++) {
            Vertice actual = ordenVisita.get(paso);
            visitados.add(actual.getId());
            imprimir(renderizar(visitados, actual.getId(), Set.of(),
                    "Paso " + (paso + 1) + " de " + ordenVisita.size()));
            if (manual) {
                System.out.print("ENTER para avanzar; f para saltar al final: ");
                if (!entrada.hasNextLine() || entrada.nextLine().strip().equalsIgnoreCase("f")) {
                    saltar = true;
                }
            } else if (pausa > 0) {
                try {
                    Thread.sleep(pausa);
                } catch (InterruptedException excepcion) {
                    Thread.currentThread().interrupt();
                    saltar = true;
                }
            }
        }

        mostrarFinal(null, true);
    }

    private String pedirControl() {
        while (true) {
            System.out.print("ENTER para avanzar paso a paso o milisegundos de pausa (ej. 100): ");
            if (!entrada.hasNextLine()) {
                return "f";
            }
            // strip() solo se aplica a la entrada del menu, nunca a las lineas del laberinto.
            String valor = entrada.nextLine().strip();
            if (valor.isEmpty() || valor.equalsIgnoreCase("f")) {
                return valor;
            }
            try {
                long milisegundos = Long.parseLong(valor);
                if (milisegundos >= 0 && milisegundos <= PAUSA_MAXIMA) {
                    return valor;
                }
                System.out.println("La pausa debe estar entre 0 y " + PAUSA_MAXIMA + " ms.");
                continue;
            } catch (NumberFormatException excepcion) {
                // Pedir otro valor.
            }
            System.out.println("Escribe ENTER, f o un numero de milisegundos entre 0 y "
                    + PAUSA_MAXIMA + ".");
        }
    }

    public void mostrarFinal(String nombre, boolean limpiar) {
        Set<Integer> visitados = new HashSet<>();
        for (Vertice vertice : resultado.getVisitados()) {
            visitados.add(vertice.getId());
        }
        Set<Integer> camino = new HashSet<>();
        for (Vertice vertice : resultado.getCamino()) {
            camino.add(vertice.getId());
        }

        StringBuilder titulo = new StringBuilder();
        if (nombre != null) {
            titulo.append(nombre).append('\n');
        }
        titulo.append("Resultado final");
        StringBuilder salida = new StringBuilder(renderizar(visitados, -1, camino,
                titulo.toString()));
        if (resultado.isEncontrado()) {
            salida.append("Longitud: ").append(resultado.getCamino().size() - 1)
                    .append(" movimientos\n");
        } else {
            salida.append("No existe camino entre la entrada y la salida\n");
        }
        salida.append("Vertices visitados: ").append(resultado.getVisitados().size())
                .append('\n').append("Tiempo: ").append(resultado.getTiempoNanos())
                .append(" nanosegundos\n");
        imprimir(salida.toString(), limpiar);
    }

    private String renderizar(Set<Integer> visitados, int actual, Set<Integer> camino,
                             String titulo) {
        StringBuilder salida = new StringBuilder((laberinto.getColumnas() + 1)
                * laberinto.getFilas() + 180);
        salida.append(titulo).append('\n');
        for (int fila = 0; fila < laberinto.getFilas(); fila++) {
            for (int columna = 0; columna < laberinto.getColumnas(); columna++) {
                int id = fila * laberinto.getColumnas() + columna;
                char celda = matriz[fila][columna];
                // Las capas se aplican sobre el caracter original.
                if (laberinto.esTransitable(fila, columna)) {
                    celda = ' ';
                }
                if (esEntrada(fila, columna)) {
                    celda = 'E';
                }
                if (esSalida(fila, columna)) {
                    celda = 'S';
                }
                if (visitados.contains(id) && !esExtremo(fila, columna)) {
                    celda = '.';
                }
                if (camino.contains(id) && !esExtremo(fila, columna)) {
                    celda = '*';
                }
                if (id == actual) {
                    celda = '@';
                }
                salida.append(celda);
            }
            salida.append('\n');
        }
        salida.append(LEYENDA).append('\n');
        return salida.toString();
    }

    private boolean esEntrada(int fila, int columna) {
        return fila == laberinto.getFilaEntrada()
                && columna == laberinto.getColumnaEntrada();
    }

    private boolean esSalida(int fila, int columna) {
        return fila == laberinto.getFilaSalida()
                && columna == laberinto.getColumnaSalida();
    }

    private boolean esExtremo(int fila, int columna) {
        return esEntrada(fila, columna) || esSalida(fila, columna);
    }

    private void imprimir(String texto) {
        imprimir(texto, true);
    }

    private void imprimir(String texto, boolean limpiar) {
        if (!limpiar) {
            System.out.print("\n==============================\n" + texto);
        } else if (usarAnsi) {
            System.out.print("\033[H\033[2J" + texto);
        } else {
            System.out.print("\n------------------------------\n" + texto);
        }
        System.out.flush();
    }
}
