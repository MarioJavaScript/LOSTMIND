import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;

public final class PanelLaberinto extends JPanel {
    public static final Color FONDO = new Color(19, 24, 34);
    public static final Color PARED = new Color(82, 99, 122);
    public static final Color TRANSITABLE = new Color(37, 47, 61);
    public static final Color ENTRADA = new Color(67, 192, 150);
    public static final Color SALIDA = new Color(247, 190, 91);
    public static final Color VISITADO = new Color(76, 123, 181);
    public static final Color ACTUAL = new Color(226, 112, 126);
    public static final Color CAMINO = new Color(177, 126, 242);

    private static final int MARGEN = 16;
    private Laberinto laberinto;
    private char[][] matriz;
    private int tamanoCelda = 24;
    private Set<Integer> visitados = Set.of();
    private Set<Integer> camino = Set.of();
    private int actual = -1;

    public PanelLaberinto() {
        setBackground(FONDO);
        actualizarTamano();
    }

    public void setLaberinto(Laberinto nuevo) {
        laberinto = nuevo;
        matriz = nuevo == null ? null : nuevo.getMatriz();
        setEstado(Set.of(), -1, Set.of());
        actualizarTamano();
    }

    public void setTamanoCelda(int nuevoTamano) {
        tamanoCelda = nuevoTamano;
        actualizarTamano();
    }

    public void setEstado(Set<Integer> nuevosVisitados, int nuevoActual,
                          Set<Integer> nuevoCamino) {
        visitados = new HashSet<>(nuevosVisitados);
        actual = nuevoActual;
        camino = new HashSet<>(nuevoCamino);
        repaint();
    }

    private void actualizarTamano() {
        int ancho = laberinto == null ? 560 : laberinto.getColumnas() * tamanoCelda + 2 * MARGEN;
        int alto = laberinto == null ? 400 : laberinto.getFilas() * tamanoCelda + 2 * MARGEN;
        setPreferredSize(new Dimension(ancho, alto));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graficos) {
        super.paintComponent(graficos);
        if (laberinto == null) {
            dibujarVacio(graficos);
            return;
        }
        Graphics2D g = (Graphics2D) graficos.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle visible = g.getClipBounds();
        int inicioFila = Math.max(0, Math.floorDiv(visible.y - MARGEN, tamanoCelda));
        int finFila = Math.min(laberinto.getFilas(),
                Math.floorDiv(visible.y + visible.height - MARGEN, tamanoCelda) + 1);
        int inicioColumna = Math.max(0, Math.floorDiv(visible.x - MARGEN, tamanoCelda));
        int finColumna = Math.min(laberinto.getColumnas(),
                Math.floorDiv(visible.x + visible.width - MARGEN, tamanoCelda) + 1);
        int lado = tamanoCelda - 2;
        int arco = Math.max(4, tamanoCelda / 4);
        for (int fila = inicioFila; fila < finFila; fila++) {
            for (int columna = inicioColumna; columna < finColumna; columna++) {
                g.setColor(colorCelda(fila, columna));
                g.fillRoundRect(MARGEN + columna * tamanoCelda + 1,
                        MARGEN + fila * tamanoCelda + 1, lado, lado, arco, arco);
            }
        }
        g.dispose();
    }

    private Color colorCelda(int fila, int columna) {
        char base = matriz[fila][columna];
        Color color = base == '+' || base == '-' || base == '|' ? PARED : TRANSITABLE;
        if (fila == laberinto.getFilaEntrada() && columna == laberinto.getColumnaEntrada()) {
            color = ENTRADA;
        }
        if (fila == laberinto.getFilaSalida() && columna == laberinto.getColumnaSalida()) {
            color = SALIDA;
        }
        int id = fila * laberinto.getColumnas() + columna;
        boolean extremo = color == ENTRADA || color == SALIDA;
        if (visitados.contains(id) && !extremo) {
            color = VISITADO;
        }
        if (id == actual) {
            color = ACTUAL;
        }
        if (camino.contains(id) && !extremo) {
            color = CAMINO;
        }
        return color;
    }

    private void dibujarVacio(Graphics graficos) {
        graficos.setColor(new Color(157, 170, 190));
        graficos.drawString("Carga un laberinto para comenzar", 24, 36);
    }
}
