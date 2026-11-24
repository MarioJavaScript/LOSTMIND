import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

public final class VentanaLaberinto extends JFrame {
    private enum Algoritmo {
        DFS("DFS"), BFS("BFS"), DIJKSTRA("Dijkstra");

        private final String nombre;

        Algoritmo(String nombre) {
            this.nombre = nombre;
        }
    }

    private record Carga(Laberinto laberinto, Grafo grafo) { }

    private static final Color SUPERFICIE = new Color(31, 38, 50);
    private static final Color TEXTO = new Color(230, 236, 245);
    private final EnumMap<Algoritmo, Resultado> resultados = new EnumMap<>(Algoritmo.class);
    private final EnumMap<Algoritmo, PanelLaberinto> paneles = new EnumMap<>(Algoritmo.class);
    private final PanelLaberinto panelPrincipal = new PanelLaberinto();
    private final JTabbedPane vistas = new JTabbedPane();
    private final JTextArea informacion = new JTextArea();
    private final JLabel estado = new JLabel("Carga un laberinto para comenzar.");
    private final JButton botonDFS = boton("DFS");
    private final JButton botonBFS = boton("BFS");
    private final JButton botonDijkstra = boton("Dijkstra");
    private final JButton botonTres = boton("Tres soluciones");
    private final JButton botonPausa = boton("Pausar");
    private final JButton botonPaso = boton("Siguiente paso");
    private final JButton botonFinal = boton("Saltar al final");
    private final JSlider zoom = new JSlider(12, 48, 24);
    private final JSlider velocidad = new JSlider(1, 100, 65);
    private final Timer reloj = new Timer(0, evento -> siguientePaso());

    private Laberinto laberinto;
    private Grafo grafo;
    private SwingWorker<?, ?> trabajo;
    private long generacion;
    private boolean ocupado;
    private boolean animando;
    private Resultado resultadoAnimado;
    private Algoritmo algoritmoAnimado;
    private List<Vertice> orden = List.of();
    private final Set<Integer> visitados = new HashSet<>();
    private int paso;

    public VentanaLaberinto() {
        super("LOSTMIND · Laberintos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(820, 540));
        setSize(1120, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(PanelLaberinto.FONDO);
        setLayout(new BorderLayout(0, 0));
        construirMenu();
        add(construirBarra(), BorderLayout.NORTH);
        add(construirCentro(), BorderLayout.CENTER);
        add(construirControles(), BorderLayout.SOUTH);
        reloj.setDelay(demora());
        zoom.addChangeListener(evento -> ajustarZoom());
        velocidad.addChangeListener(evento -> reloj.setDelay(demora()));
        actualizarControles();
        actualizarInformacion();
    }

    private void construirMenu() {
        JMenuBar barra = new JMenuBar();
        barra.setBackground(SUPERFICIE);
        JMenu archivo = new JMenu("Archivo");
        archivo.setForeground(TEXTO);
        JMenuItem cargar = new JMenuItem("Cargar laberinto...");
        cargar.addActionListener(evento -> cargar());
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(evento -> dispose());
        archivo.add(cargar);
        archivo.addSeparator();
        archivo.add(salir);
        barra.add(archivo);
        setJMenuBar(barra);
    }

    private JToolBar construirBarra() {
        JToolBar barra = new JToolBar();
        barra.setFloatable(false);
        barra.setBackground(SUPERFICIE);
        barra.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
        JButton botonCargar = boton("Cargar laberinto");
        botonCargar.addActionListener(evento -> cargar());
        botonDFS.addActionListener(evento -> resolver(Algoritmo.DFS));
        botonBFS.addActionListener(evento -> resolver(Algoritmo.BFS));
        botonDijkstra.addActionListener(evento -> resolver(Algoritmo.DIJKSTRA));
        botonTres.addActionListener(evento -> mostrarTres());
        JButton botonTiempos = boton("Tiempos");
        botonTiempos.addActionListener(evento -> mostrarTiempos());
        JButton botonSalir = boton("Salir");
        botonSalir.addActionListener(evento -> dispose());
        for (JButton boton : new JButton[] { botonCargar, botonDFS, botonBFS,
                botonDijkstra, botonTres, botonTiempos, botonSalir }) {
            barra.add(boton);
            barra.addSeparator(new Dimension(5, 0));
        }
        return barra;
    }

    private JSplitPane construirCentro() {
        vistas.addTab("Exploración", crearScroll(panelPrincipal));
        vistas.setBackground(SUPERFICIE);
        vistas.setForeground(TEXTO);

        JPanel lateral = new JPanel(new BorderLayout(0, 16));
        lateral.setBackground(SUPERFICIE);
        lateral.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));
        lateral.setPreferredSize(new Dimension(270, 400));
        JLabel titulo = new JLabel("RESULTADOS");
        titulo.setForeground(TEXTO);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 15f));
        lateral.add(titulo, BorderLayout.NORTH);
        informacion.setEditable(false);
        informacion.setBackground(SUPERFICIE);
        informacion.setForeground(TEXTO);
        informacion.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        informacion.setLineWrap(true);
        informacion.setWrapStyleWord(true);
        lateral.add(informacion, BorderLayout.CENTER);
        lateral.add(construirLeyenda(), BorderLayout.SOUTH);

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, vistas, lateral);
        division.setResizeWeight(1.0);
        division.setDividerLocation(810);
        division.setBorder(null);
        return division;
    }

    private JPanel construirLeyenda() {
        JPanel leyenda = new JPanel(new GridLayout(0, 2, 8, 8));
        leyenda.setBackground(SUPERFICIE);
        TitledBorder borde = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 82, 101)), "Leyenda");
        borde.setTitleColor(TEXTO);
        leyenda.setBorder(borde);
        agregarClave(leyenda, "Pared", PanelLaberinto.PARED);
        agregarClave(leyenda, "Transitable", PanelLaberinto.TRANSITABLE);
        agregarClave(leyenda, "Entrada", PanelLaberinto.ENTRADA);
        agregarClave(leyenda, "Salida", PanelLaberinto.SALIDA);
        agregarClave(leyenda, "Visitado", PanelLaberinto.VISITADO);
        agregarClave(leyenda, "Actual", PanelLaberinto.ACTUAL);
        agregarClave(leyenda, "Camino final", PanelLaberinto.CAMINO);
        return leyenda;
    }

    private void agregarClave(JPanel leyenda, String nombre, Color color) {
        JLabel muestra = new JLabel("■");
        muestra.setForeground(color);
        muestra.setFont(muestra.getFont().deriveFont(17f));
        JLabel texto = new JLabel(nombre);
        texto.setForeground(TEXTO);
        leyenda.add(muestra);
        leyenda.add(texto);
    }

    private JPanel construirControles() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(SUPERFICIE);
        pie.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controles.setBackground(SUPERFICIE);
        botonPausa.addActionListener(evento -> alternarPausa());
        botonPaso.addActionListener(evento -> {
            reloj.stop();
            botonPausa.setText("Continuar");
            siguientePaso();
        });
        botonFinal.addActionListener(evento -> terminarAnimacion());
        controles.add(botonPausa);
        controles.add(botonPaso);
        controles.add(botonFinal);
        controles.add(etiqueta("Velocidad"));
        velocidad.setBackground(SUPERFICIE);
        velocidad.setPreferredSize(new Dimension(105, 28));
        controles.add(velocidad);
        controles.add(etiqueta("Zoom"));
        zoom.setBackground(SUPERFICIE);
        zoom.setPreferredSize(new Dimension(105, 28));
        controles.add(zoom);
        pie.add(controles, BorderLayout.NORTH);
        estado.setForeground(TEXTO);
        estado.setBorder(BorderFactory.createEmptyBorder(8, 3, 2, 0));
        pie.add(estado, BorderLayout.SOUTH);
        return pie;
    }

    private static JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    private static JButton boton(String texto) {
        JButton boton = new JButton(texto);
        boton.setFocusPainted(false);
        boton.setBackground(new Color(53, 68, 87));
        boton.setForeground(TEXTO);
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 102, 127)),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        return boton;
    }

    private void cargar() {
        JFileChooser selector = new JFileChooser(Path.of(".").toAbsolutePath().toFile());
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path ruta = selector.getSelectedFile().toPath();
        cancelarTrabajo();
        detenerAnimacion();
        ocupado = true;
        actualizarControles();
        estado.setText("Cargando " + ruta.getFileName() + "...");
        long solicitud = generacion;
        SwingWorker<Carga, Void> nuevo = new SwingWorker<>() {
            @Override
            protected Carga doInBackground() throws LaberintoInvalidoException {
                Laberinto cargado = Laberinto.cargar(ruta.toString());
                return new Carga(cargado, new Grafo(cargado));
            }

            @Override
            protected void done() {
                if (solicitud != generacion) return;
                ocupado = false;
                try {
                    Carga carga = get();
                    laberinto = carga.laberinto();
                    grafo = carga.grafo();
                    resultados.clear();
                    quitarComparaciones();
                    panelPrincipal.setLaberinto(laberinto);
                    vistas.setSelectedIndex(0);
                    estado.setText("Laberinto cargado: " + laberinto.getFilas()
                            + " filas × " + laberinto.getColumnas() + " columnas.");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    mostrarError(error);
                } catch (ExecutionException | CancellationException error) {
                    mostrarError(error);
                }
                actualizarInformacion();
                actualizarControles();
            }
        };
        trabajo = nuevo;
        nuevo.execute();
    }

    private void resolver(Algoritmo algoritmo) {
        if (grafo == null || ocupado) return;
        detenerAnimacion();
        quitarComparaciones();
        vistas.setSelectedIndex(0);
        panelPrincipal.setEstado(Set.of(), -1, Set.of());
        ocupado = true;
        actualizarControles();
        estado.setText("Calculando " + algoritmo.nombre + "...");
        long solicitud = ++generacion;
        Grafo grafoActual = grafo;
        SwingWorker<Resultado, Void> nuevo = new SwingWorker<>() {
            @Override
            protected Resultado doInBackground() {
                ejecutar(grafoActual, algoritmo);
                return resultadoDe(grafoActual, algoritmo);
            }

            @Override
            protected void done() {
                if (solicitud != generacion) return;
                ocupado = false;
                try {
                    Resultado resultado = get();
                    resultados.put(algoritmo, resultado);
                    actualizarInformacion();
                    iniciarAnimacion(algoritmo, resultado);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    mostrarError(error);
                } catch (ExecutionException | CancellationException error) {
                    mostrarError(error);
                }
                actualizarControles();
            }
        };
        trabajo = nuevo;
        nuevo.execute();
    }

    private void mostrarTres() {
        if (grafo == null || ocupado) return;
        detenerAnimacion();
        ocupado = true;
        actualizarControles();
        estado.setText("Preparando las tres soluciones...");
        long solicitud = ++generacion;
        Grafo grafoActual = grafo;
        EnumMap<Algoritmo, Resultado> vigentes = new EnumMap<>(resultados);
        SwingWorker<EnumMap<Algoritmo, Resultado>, Void> nuevo = new SwingWorker<>() {
            @Override
            protected EnumMap<Algoritmo, Resultado> doInBackground() {
                for (Algoritmo algoritmo : Algoritmo.values()) {
                    if (!vigentes.containsKey(algoritmo)) {
                        ejecutar(grafoActual, algoritmo);
                        vigentes.put(algoritmo, resultadoDe(grafoActual, algoritmo));
                    }
                }
                return vigentes;
            }

            @Override
            protected void done() {
                if (solicitud != generacion) return;
                ocupado = false;
                try {
                    resultados.putAll(get());
                    mostrarComparaciones();
                    actualizarInformacion();
                    estado.setText("Tres soluciones listas. Selecciona una pestaña para ver el resultado.");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    mostrarError(error);
                } catch (ExecutionException | CancellationException error) {
                    mostrarError(error);
                }
                actualizarControles();
            }
        };
        trabajo = nuevo;
        nuevo.execute();
    }

    private static void ejecutar(Grafo grafo, Algoritmo algoritmo) {
        switch (algoritmo) {
            case DFS -> grafo.DFS();
            case BFS -> grafo.BFS();
            case DIJKSTRA -> grafo.caminoMasCorto();
        }
    }

    private static Resultado resultadoDe(Grafo grafo, Algoritmo algoritmo) {
        return switch (algoritmo) {
            case DFS -> grafo.getUltimoResultadoDFS();
            case BFS -> grafo.getUltimoResultadoBFS();
            case DIJKSTRA -> grafo.getUltimoResultadoDijkstra();
        };
    }

    private void mostrarComparaciones() {
        quitarComparaciones();
        for (Algoritmo algoritmo : Algoritmo.values()) {
            PanelLaberinto panel = new PanelLaberinto();
            panel.setTamanoCelda(zoom.getValue());
            panel.setLaberinto(laberinto);
            Resultado resultado = resultados.get(algoritmo);
            panel.setEstado(ids(resultado.getVisitados()), -1, ids(resultado.getCamino()));
            paneles.put(algoritmo, panel);
            vistas.addTab(algoritmo.nombre, crearScroll(panel));
        }
        vistas.setSelectedIndex(1);
    }

    private void quitarComparaciones() {
        while (vistas.getTabCount() > 1) vistas.removeTabAt(1);
        paneles.clear();
    }

    private void iniciarAnimacion(Algoritmo algoritmo, Resultado resultado) {
        algoritmoAnimado = algoritmo;
        resultadoAnimado = resultado;
        orden = resultado.getVisitados();
        visitados.clear();
        paso = 0;
        animando = true;
        botonPausa.setText("Pausar");
        reloj.setDelay(demora());
        reloj.start();
        estado.setText(algoritmo.nombre + ": exploración 0 / " + orden.size());
    }

    private void siguientePaso() {
        if (!animando) return;
        if (paso == orden.size()) {
            terminarAnimacion();
            return;
        }
        Vertice vertice = orden.get(paso++);
        visitados.add(vertice.getId());
        panelPrincipal.setEstado(visitados, vertice.getId(), Set.of());
        estado.setText(algoritmoAnimado.nombre + ": exploración " + paso
                + " / " + orden.size());
    }

    private void terminarAnimacion() {
        if (!animando) return;
        reloj.stop();
        animando = false;
        panelPrincipal.setEstado(ids(resultadoAnimado.getVisitados()), -1,
                ids(resultadoAnimado.getCamino()));
        estado.setText(resultadoAnimado.isEncontrado()
                ? algoritmoAnimado.nombre + ": camino de "
                        + (resultadoAnimado.getCamino().size() - 1) + " movimientos."
                : algoritmoAnimado.nombre + ": No existe camino.");
        actualizarControles();
    }

    private void detenerAnimacion() {
        reloj.stop();
        animando = false;
        actualizarControles();
    }

    private void alternarPausa() {
        if (!animando) return;
        if (reloj.isRunning()) {
            reloj.stop();
            botonPausa.setText("Continuar");
        } else {
            reloj.start();
            botonPausa.setText("Pausar");
        }
    }

    private void cancelarTrabajo() {
        generacion++;
        if (trabajo != null && !trabajo.isDone()) trabajo.cancel(true);
    }

    private void ajustarZoom() {
        panelPrincipal.setTamanoCelda(zoom.getValue());
        for (PanelLaberinto panel : paneles.values()) panel.setTamanoCelda(zoom.getValue());
    }

    private int demora() {
        return 25 + (100 - velocidad.getValue()) * 14;
    }

    private void actualizarControles() {
        boolean disponible = laberinto != null && !ocupado;
        botonDFS.setEnabled(disponible);
        botonBFS.setEnabled(disponible);
        botonDijkstra.setEnabled(disponible);
        botonTres.setEnabled(disponible);
        botonPausa.setEnabled(animando);
        botonPaso.setEnabled(animando);
        botonFinal.setEnabled(animando);
    }

    private void actualizarInformacion() {
        StringBuilder texto = new StringBuilder();
        for (Algoritmo algoritmo : Algoritmo.values()) {
            Resultado resultado = resultados.get(algoritmo);
            texto.append(algoritmo.nombre).append('\n');
            if (resultado == null) {
                texto.append("  Pendiente\n\n");
            } else {
                texto.append("  Visitados: ").append(resultado.getVisitados().size()).append('\n');
                if (resultado.isEncontrado()) {
                    texto.append("  Longitud: ").append(resultado.getCamino().size() - 1)
                            .append(" movimientos\n");
                } else {
                    texto.append("  No existe camino\n");
                }
                texto.append("  Tiempo: ").append(resultado.getTiempoNanos())
                        .append(" ns\n\n");
            }
        }
        informacion.setText(texto.toString());
    }

    private void mostrarTiempos() {
        StringBuilder texto = new StringBuilder("Tiempo de búsqueda y reconstrucción:\n\n");
        for (Algoritmo algoritmo : Algoritmo.values()) {
            Resultado resultado = resultados.get(algoritmo);
            texto.append(algoritmo.nombre).append(": ")
                    .append(resultado == null ? "pendiente" : resultado.getTiempoNanos() + " ns")
                    .append('\n');
        }
        JOptionPane.showMessageDialog(this, texto.toString(), "Tiempos",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarError(Throwable error) {
        Throwable causa = error instanceof ExecutionException && error.getCause() != null
                ? error.getCause() : error;
        String motivo = causa.getMessage() == null ? causa.toString() : causa.getMessage();
        estado.setText("No se pudo completar la operación.");
        JOptionPane.showMessageDialog(this, motivo, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static Set<Integer> ids(List<Vertice> vertices) {
        Set<Integer> ids = new HashSet<>();
        for (Vertice vertice : vertices) ids.add(vertice.getId());
        return ids;
    }

    private static JScrollPane crearScroll(PanelLaberinto panel) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.getViewport().setBackground(PanelLaberinto.FONDO);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }
}
