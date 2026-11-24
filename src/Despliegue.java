import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Supplier;

public final class Despliegue {
    private static final long PAUSA_MAXIMA = 5000;

    private final Laberinto laberinto;
    private final Resultado resultado;
    private final Scanner entrada;
    private final char[][] matriz;
    private final boolean usarAnsi;
    private TemaConsola tema;

    public Despliegue(Laberinto laberinto, Resultado resultado, Scanner entrada) {
        this(laberinto, resultado, entrada, TemaConsola.ASCII);
    }

    public Despliegue(Laberinto laberinto, Resultado resultado, Scanner entrada,
                      TemaConsola tema) {
        this.laberinto = laberinto;
        this.resultado = resultado;
        this.entrada = entrada;
        this.matriz = laberinto.getMatriz();
        this.usarAnsi = System.console() != null && System.getenv("NO_COLOR") == null;
        this.tema = tema;
    }

    public TemaConsola getTema() {
        return tema;
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
            String titulo = "Paso " + (paso + 1) + " de " + ordenVisita.size();
            imprimir(() -> renderizar(visitados, actual.getId(), Set.of(), titulo), true);
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
        String tituloFinal = titulo.toString();
        imprimir(() -> textoFinal(visitados, camino, tituloFinal), limpiar);
    }

    private String textoFinal(Set<Integer> visitados, Set<Integer> camino, String titulo) {
        StringBuilder salida = new StringBuilder(renderizar(visitados, -1, camino, titulo));
        if (resultado.isEncontrado()) {
            salida.append("Longitud: ").append(resultado.getCamino().size() - 1)
                    .append(" movimientos\n");
        } else {
            salida.append("No existe camino entre la entrada y la salida\n");
        }
        salida.append("Vertices visitados: ").append(resultado.getVisitados().size())
                .append('\n').append("Tiempo: ").append(resultado.getTiempoNanos())
                .append(" nanosegundos\n");
        return salida.toString();
    }

    private String renderizar(Set<Integer> visitados, int actual, Set<Integer> camino,
                             String titulo) {
        StringBuilder salida = new StringBuilder((laberinto.getColumnas() + 1)
                * laberinto.getFilas() + 180);
        salida.append(titulo).append('\n');
        for (String renglon : vista(visitados, actual, camino)) {
            salida.append(renglon).append('\n');
        }
        salida.append(tema.leyenda()).append('\n');
        return salida.toString();
    }

    // Devuelve solo las filas del laberinto para comprobar su tamaño sin la leyenda.
    List<String> vista(Set<Integer> visitados, int actual, Set<Integer> camino) {
        List<String> renglones = new ArrayList<>(laberinto.getFilas());
        for (int fila = 0; fila < laberinto.getFilas(); fila++) {
            StringBuilder renglon = new StringBuilder(laberinto.getColumnas());
            for (int columna = 0; columna < laberinto.getColumnas(); columna++) {
                int id = fila * laberinto.getColumnas() + columna;
                char celda = tema.pared(matriz, fila, columna);
                // Las capas solo cambian la vista.
                if (laberinto.esTransitable(fila, columna)) {
                    celda = tema.transitable();
                }
                if (esEntrada(fila, columna)) {
                    celda = tema.entrada();
                }
                if (esSalida(fila, columna)) {
                    celda = tema.salida();
                }
                if (visitados.contains(id) && !esExtremo(fila, columna)) {
                    celda = tema.visitado();
                }
                if (id == actual) {
                    celda = tema.actual();
                }
                if (camino.contains(id) && !esExtremo(fila, columna)) {
                    celda = tema.camino();
                }
                renglon.append(celda);
            }
            renglones.add(renglon.toString());
        }
        return renglones;
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

    private void imprimir(Supplier<String> texto, boolean limpiar) {
        imprimirTexto(texto.get(), limpiar);
        // PrintStream registra errores de escritura sin lanzar IOException.
        if (tema == TemaConsola.UNICODE && System.out.checkError()) {
            tema = TemaConsola.ASCII;
            System.out.println("Aviso: fallo la salida Unicode; se usara ASCII.");
            imprimirTexto(texto.get(), limpiar);
        }
    }

    private void imprimirTexto(String texto, boolean limpiar) {
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
