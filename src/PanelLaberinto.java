import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

public final class PanelLaberinto extends JPanel implements Scrollable {
    public static final Color FONDO = new Color(18, 24, 35);
    public static final Color PARED = new Color(44, 58, 76);
    public static final Color TRANSITABLE = new Color(220, 231, 239);
    public static final Color ENTRADA = new Color(70, 210, 157);
    public static final Color SALIDA = new Color(255, 194, 91);
    public static final Color VISITADO = new Color(151, 203, 226);
    public static final Color ACTUAL = new Color(255, 225, 95);
    public static final Color CAMINO = new Color(239, 105, 173);

    private static final int MARGEN = 16;
    private Laberinto laberinto;
    private char[][] matriz;
    private int tamanoCelda = 24;
    private Set<Integer> visitados = Set.of();
    private Set<Integer> camino = Set.of();
    private int actual = -1;

    public PanelLaberinto() {
        setBackground(FONDO);
        setOpaque(true);
    }

    public void setLaberinto(Laberinto nuevo) {
        laberinto = nuevo;
        matriz = nuevo == null ? null : nuevo.getMatriz();
        setEstado(Set.of(), -1, Set.of());
        revalidate();
        repaint();
    }

    public void setTamanoCelda(int nuevoTamano) {
        tamanoCelda = Math.max(5, nuevoTamano);
        revalidate();
        repaint();
    }

    public void setEstado(Set<Integer> nuevosVisitados, int nuevoActual,
                          Set<Integer> nuevoCamino) {
        visitados = new HashSet<>(nuevosVisitados);
        actual = nuevoActual;
        camino = new HashSet<>(nuevoCamino);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (laberinto == null) return new Dimension(500, 360);
        return new Dimension(laberinto.getColumnas() * tamanoCelda + 2 * MARGEN,
                laberinto.getFilas() * tamanoCelda + 2 * MARGEN);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
        return tamanoCelda;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
        return Math.max(tamanoCelda, (orientacion == SwingConstants.VERTICAL
                ? visible.height : visible.width)
                - tamanoCelda);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport vista
                && getPreferredSize().width < vista.getWidth();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport vista
                && getPreferredSize().height < vista.getHeight();
    }

    @Override
    protected void paintComponent(Graphics graficos) {
        super.paintComponent(graficos);
        if (laberinto == null) return;
        Graphics2D g = (Graphics2D) graficos.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle visible = g.getClipBounds();
        if (visible == null) visible = new Rectangle(0, 0, getWidth(), getHeight());
        int origenX = Math.max(MARGEN, (getWidth() - laberinto.getColumnas() * tamanoCelda) / 2);
        int origenY = Math.max(MARGEN, (getHeight() - laberinto.getFilas() * tamanoCelda) / 2);
        int inicioFila = Math.max(0, Math.floorDiv(visible.y - origenY, tamanoCelda));
        int finFila = Math.min(laberinto.getFilas(),
                Math.floorDiv(visible.y + visible.height - origenY, tamanoCelda) + 1);
        int inicioColumna = Math.max(0, Math.floorDiv(visible.x - origenX, tamanoCelda));
        int finColumna = Math.min(laberinto.getColumnas(),
                Math.floorDiv(visible.x + visible.width - origenX, tamanoCelda) + 1);
        int separacion = tamanoCelda >= 12 ? 2 : 1;
        int lado = tamanoCelda - separacion;
        int arco = Math.max(3, tamanoCelda / 4);
        for (int fila = inicioFila; fila < finFila; fila++) {
            for (int columna = inicioColumna; columna < finColumna; columna++) {
                int x = origenX + columna * tamanoCelda + separacion / 2;
                int y = origenY + fila * tamanoCelda + separacion / 2;
                int id = fila * laberinto.getColumnas() + columna;
                boolean esEntrada = fila == laberinto.getFilaEntrada()
                        && columna == laberinto.getColumnaEntrada();
                boolean esSalida = fila == laberinto.getFilaSalida()
                        && columna == laberinto.getColumnaSalida();
                Color color = colorCelda(fila, columna, id, esEntrada, esSalida);
                g.setColor(color);
                g.fillRoundRect(x, y, lado, lado, arco, arco);
                if (id == actual || camino.contains(id) && !esEntrada && !esSalida) {
                    g.setColor(id == actual && !camino.contains(id)
                            ? new Color(255, 255, 225) : new Color(255, 229, 243));
                    g.drawRoundRect(x + 1, y + 1, lado - 2, lado - 2, arco, arco);
                    if (tamanoCelda >= 18) {
                        g.drawRoundRect(x + 2, y + 2, lado - 4, lado - 4, arco, arco);
                    }
                }
                if (tamanoCelda >= 12 && (esEntrada || esSalida)) {
                    dibujarLetra(g, esEntrada ? "E" : "S", x, y, lado);
                }
            }
        }
        g.dispose();
    }

    private Color colorCelda(int fila, int columna, int id,
                             boolean esEntrada, boolean esSalida) {
        char base = matriz[fila][columna];
        Color color = base == '+' || base == '-' || base == '|' ? PARED : TRANSITABLE;
        if (esEntrada) color = ENTRADA;
        if (esSalida) color = SALIDA;
        if (visitados.contains(id) && !esEntrada && !esSalida) color = VISITADO;
        if (id == actual) color = ACTUAL;
        if (camino.contains(id) && !esEntrada && !esSalida) color = CAMINO;
        return color;
    }

    private void dibujarLetra(Graphics2D g, String letra, int x, int y, int lado) {
        Font anterior = g.getFont();
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, tamanoCelda * 2 / 3)));
        FontMetrics medidas = g.getFontMetrics();
        g.setColor(new Color(24, 35, 46));
        g.drawString(letra, x + (lado - medidas.stringWidth(letra)) / 2,
                y + (lado - medidas.getHeight()) / 2 + medidas.getAscent());
        g.setFont(anterior);
    }
}
