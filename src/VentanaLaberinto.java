import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

public final class VentanaLaberinto extends JFrame {
    private enum Algoritmo {
        DFS("DFS"), BFS("BFS"), DIJKSTRA("Dijkstra");

        private final String nombre;

        Algoritmo(String nombre) {
            this.nombre = nombre;
        }
    }

    private record Carga(Laberinto laberinto, Grafo grafo) { }

    private static final Color FONDO = PanelLaberinto.FONDO;
    private static final Color SUPERFICIE = new Color(30, 40, 55);
    private static final Color TARJETA = new Color(39, 52, 70);
    private static final Color TEXTO = new Color(238, 242, 247);
    private static final Color SECUNDARIO = new Color(179, 194, 211);
    private static final Font FUENTE = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font FUENTE_TITULO = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    private final EnumMap<Algoritmo, Resultado> resultados = new EnumMap<>(Algoritmo.class);
    private final PanelLaberinto panelPrincipal = new PanelLaberinto();
    private final JScrollPane scrollPrincipal = scroll(panelPrincipal);
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel centro = new JPanel(tarjetas);
    private final JPanel filaComparacion = new JPanel();
    private final JPanel resultadosPanel = new JPanel();
    private final JLabel titulo = etiqueta("Carga un laberinto para comenzar", FUENTE_TITULO);
    private final JLabel estado = etiqueta("Carga un laberinto para comenzar", FUENTE);
    private final JProgressBar progreso = new JProgressBar();
    private final JButton botonDFS = boton("DFS");
    private final JButton botonBFS = boton("BFS");
    private final JButton botonDijkstra = boton("Dijkstra");
    private final JButton botonComparar = boton("Comparar los tres");
    private final JButton botonPausa = boton("Pausar");
    private final JButton botonPaso = boton("Siguiente paso");
    private final JButton botonFinal = boton("Saltar al final");
    private final JButton botonAjustar = boton("Ajustar");
    private final JSlider zoom = slider(24, 64, 24);
    private final JSlider velocidad = slider(1, 100, 65);
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
        setMinimumSize(new Dimension(860, 580));
        setSize(1180, 740);
        setLocationRelativeTo(null);
        getContentPane().setBackground(FONDO);
        setLayout(new BorderLayout());
        construirMenu();
        add(construirBarra(), BorderLayout.NORTH);
        add(construirCentro(), BorderLayout.CENTER);
        add(construirPie(), BorderLayout.SOUTH);
        reloj.setDelay(demora());
        zoom.addChangeListener(evento -> panelPrincipal.setTamanoCelda(zoom.getValue()));
        velocidad.addChangeListener(evento -> reloj.setDelay(demora()));
        actualizarResultados();
        actualizarControles();
    }

    private void construirMenu() {
        JMenuBar barra = new JMenuBar();
        barra.setBackground(SUPERFICIE);
        JMenu archivo = new JMenu("Archivo");
        archivo.setForeground(TEXTO);
        archivo.setFont(FUENTE);
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

    private JPanel construirBarra() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 9));
        barra.setBackground(SUPERFICIE);
        JButton cargar = boton("Cargar laberinto");
        cargar.addActionListener(evento -> cargar());
        botonDFS.addActionListener(evento -> resolver(Algoritmo.DFS));
        botonBFS.addActionListener(evento -> resolver(Algoritmo.BFS));
        botonDijkstra.addActionListener(evento -> resolver(Algoritmo.DIJKSTRA));
        botonComparar.addActionListener(evento -> mostrarTres());
        JButton tiempos = boton("Tiempos");
        tiempos.addActionListener(evento -> mostrarTiempos());
        JButton salir = boton("Salir");
        salir.addActionListener(evento -> dispose());
        barra.add(cargar);
        barra.add(separador());
        barra.add(etiqueta("Resolver:", FUENTE));
        barra.add(botonDFS);
        barra.add(botonBFS);
        barra.add(botonDijkstra);
        barra.add(separador());
        barra.add(botonComparar);
        barra.add(tiempos);
        barra.add(separador());
        barra.add(salir);
        return barra;
    }

    private JSplitPane construirCentro() {
        centro.setBackground(FONDO);
        centro.add(construirVacio(), "vacio");
        centro.add(construirPrincipal(), "principal");
        centro.add(construirComparacion(), "comparacion");
        tarjetas.show(centro, "vacio");

        resultadosPanel.setLayout(new BoxLayout(resultadosPanel, BoxLayout.Y_AXIS));
        resultadosPanel.setBackground(SUPERFICIE);
        JScrollPane lateral = new JScrollPane(resultadosPanel);
        lateral.getViewport().setBackground(SUPERFICIE);
        lateral.setBorder(BorderFactory.createEmptyBorder());
        lateral.setPreferredSize(new Dimension(290, 400));
        lateral.setMinimumSize(new Dimension(270, 200));
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centro, lateral);
        division.setResizeWeight(1.0);
        division.setDividerLocation(870);
        division.setBorder(null);
        return division;
    }

    private JPanel construirVacio() {
        JPanel vacio = new JPanel(new GridBagLayout());
        vacio.setBackground(FONDO);
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBackground(FONDO);
        JLabel mensaje = etiqueta("Carga un laberinto para comenzar",
                new Font(Font.SANS_SERIF, Font.BOLD, 24));
        mensaje.setAlignmentX(CENTER_ALIGNMENT);
        JButton cargar = boton("Cargar laberinto");
        cargar.setAlignmentX(CENTER_ALIGNMENT);
        cargar.addActionListener(evento -> cargar());
        contenido.add(mensaje);
        contenido.add(Box.createVerticalStrut(20));
        contenido.add(cargar);
        vacio.add(contenido);
        return vacio;
    }

    private JPanel construirPrincipal() {
        JPanel principal = new JPanel(new BorderLayout(0, 8));
        principal.setBackground(FONDO);
        JPanel encabezado = new JPanel(new BorderLayout(0, 6));
        encabezado.setBackground(SUPERFICIE);
        encabezado.setBorder(BorderFactory.createEmptyBorder(12, 16, 10, 16));
        encabezado.add(titulo, BorderLayout.NORTH);
        progreso.setStringPainted(true);
        progreso.setString("0 / 0");
        progreso.setFont(FUENTE);
        encabezado.add(progreso, BorderLayout.SOUTH);
        principal.add(encabezado, BorderLayout.NORTH);
        principal.add(scrollPrincipal, BorderLayout.CENTER);
        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setBackground(SUPERFICIE);
        inferior.add(construirLeyenda(), BorderLayout.NORTH);
        inferior.add(construirControles(), BorderLayout.SOUTH);
        principal.add(inferior, BorderLayout.SOUTH);
        return principal;
    }

    private JPanel construirLeyenda() {
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 7));
        leyenda.setBackground(SUPERFICIE);
        agregarClave(leyenda, "Pared", PanelLaberinto.PARED);
        agregarClave(leyenda, "Transitable", PanelLaberinto.TRANSITABLE);
        agregarClave(leyenda, "Entrada E", PanelLaberinto.ENTRADA);
        agregarClave(leyenda, "Salida S", PanelLaberinto.SALIDA);
        agregarClave(leyenda, "Visitado", PanelLaberinto.VISITADO);
        agregarClave(leyenda, "Actual", PanelLaberinto.ACTUAL);
        agregarClave(leyenda, "Camino final", PanelLaberinto.CAMINO);
        return leyenda;
    }

    private void agregarClave(JPanel leyenda, String nombre, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setBackground(SUPERFICIE);
        JLabel muestra = new JLabel("■");
        muestra.setForeground(color);
        muestra.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        item.add(muestra);
        item.add(etiqueta(nombre, FUENTE));
        leyenda.add(item);
    }

    private JPanel construirControles() {
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.CENTER, 9, 7));
        controles.setBackground(SUPERFICIE);
        botonPausa.addActionListener(evento -> alternarPausa());
        botonPaso.addActionListener(evento -> {
            reloj.stop();
            botonPausa.setText("Continuar");
            siguientePaso();
        });
        botonFinal.addActionListener(evento -> terminarAnimacion());
        botonAjustar.addActionListener(evento -> ajustarZoom());
        controles.add(botonPausa);
        controles.add(botonPaso);
        controles.add(botonFinal);
        controles.add(etiqueta("Velocidad:", FUENTE));
        controles.add(velocidad);
        controles.add(etiqueta("Zoom:", FUENTE));
        controles.add(zoom);
        controles.add(botonAjustar);
        return controles;
    }

    private JPanel construirComparacion() {
        JPanel comparacion = new JPanel(new BorderLayout(0, 10));
        comparacion.setBackground(FONDO);
        JPanel cabecera = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        cabecera.setBackground(SUPERFICIE);
        JButton volver = boton("Volver");
        volver.addActionListener(evento -> tarjetas.show(centro, "principal"));
        cabecera.add(volver);
        cabecera.add(etiqueta("Comparación de soluciones", FUENTE_TITULO));
        comparacion.add(cabecera, BorderLayout.NORTH);
        filaComparacion.setLayout(new BoxLayout(filaComparacion, BoxLayout.X_AXIS));
        filaComparacion.setBackground(FONDO);
        filaComparacion.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(filaComparacion);
        scroll.getViewport().setBackground(FONDO);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        comparacion.add(scroll, BorderLayout.CENTER);
        return comparacion;
    }

    private JPanel construirPie() {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(SUPERFICIE);
        pie.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        pie.add(estado, BorderLayout.WEST);
        return pie;
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
                    panelPrincipal.setLaberinto(laberinto);
                    panelPrincipal.setEstado(Set.of(), -1, Set.of());
                    progreso.setIndeterminate(false);
                    progreso.setMaximum(1);
                    progreso.setValue(0);
                    progreso.setString("Sin exploración");
                    titulo.setText("Laberinto: " + ruta.getFileName());
                    tarjetas.show(centro, "principal");
                    estado.setText("Cargado: " + ruta.getFileName() + " ("
                            + laberinto.getFilas() + "x" + laberinto.getColumnas() + ")");
                    SwingUtilities.invokeLater(() -> ajustarZoom());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    mostrarError(error);
                } catch (ExecutionException | CancellationException error) {
                    mostrarError(error);
                }
                actualizarResultados();
                actualizarControles();
            }
        };
        trabajo = nuevo;
        nuevo.execute();
    }

    private void resolver(Algoritmo algoritmo) {
        if (grafo == null || ocupado) return;
        detenerAnimacion();
        tarjetas.show(centro, "principal");
        panelPrincipal.setEstado(Set.of(), -1, Set.of());
        ocupado = true;
        actualizarControles();
        titulo.setText("Algoritmo: " + algoritmo.nombre + " · Calculando...");
        progreso.setIndeterminate(true);
        estado.setText("Resolviendo con " + algoritmo.nombre + "...");
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
                progreso.setIndeterminate(false);
                try {
                    Resultado resultado = get();
                    resultados.put(algoritmo, resultado);
                    actualizarResultados();
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
        if (animando) terminarAnimacion();
        ocupado = true;
        actualizarControles();
        estado.setText("Preparando comparación...");
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
                    actualizarResultados();
                    mostrarComparacion();
                    estado.setText("Comparación lista.");
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

    private void mostrarComparacion() {
        filaComparacion.removeAll();
        for (Algoritmo algoritmo : Algoritmo.values()) {
            Resultado resultado = resultados.get(algoritmo);
            filaComparacion.add(construirTarjetaComparacion(algoritmo, resultado));
            filaComparacion.add(Box.createHorizontalStrut(12));
        }
        filaComparacion.revalidate();
        filaComparacion.repaint();
        tarjetas.show(centro, "comparacion");
    }

    private JPanel construirTarjetaComparacion(Algoritmo algoritmo, Resultado resultado) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 10));
        tarjeta.setBackground(TARJETA);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(76, 94, 116)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        tarjeta.setPreferredSize(new Dimension(350, 390));
        tarjeta.setMaximumSize(new Dimension(350, 390));
        tarjeta.add(etiqueta(algoritmo.nombre, FUENTE_TITULO), BorderLayout.NORTH);
        PanelLaberinto mini = new PanelLaberinto();
        mini.setTamanoCelda(Math.max(5, Math.min(18,
                Math.min(300 / laberinto.getColumnas(), 220 / laberinto.getFilas()))));
        mini.setLaberinto(laberinto);
        mini.setEstado(ids(resultado.getVisitados()), -1, ids(resultado.getCamino()));
        JScrollPane miniScroll = scroll(mini);
        miniScroll.setPreferredSize(new Dimension(320, 250));
        tarjeta.add(miniScroll, BorderLayout.CENTER);
        JPanel datos = new JPanel(new GridLayout(0, 1, 0, 4));
        datos.setBackground(TARJETA);
        datos.add(etiqueta("Visitados: " + resultado.getVisitados().size(), FUENTE));
        datos.add(etiqueta(resultado.isEncontrado()
                ? "Longitud: " + (resultado.getCamino().size() - 1) + " movimientos"
                : "No existe camino", FUENTE));
        datos.add(etiqueta("Tiempo: " + tiempo(resultado), FUENTE));
        tarjeta.add(datos, BorderLayout.SOUTH);
        return tarjeta;
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

    private void iniciarAnimacion(Algoritmo algoritmo, Resultado resultado) {
        algoritmoAnimado = algoritmo;
        resultadoAnimado = resultado;
        orden = resultado.getVisitados();
        visitados.clear();
        paso = 0;
        animando = true;
        progreso.setMaximum(Math.max(1, orden.size()));
        progreso.setValue(0);
        progreso.setString("0 / " + orden.size());
        titulo.setText("Algoritmo: " + algoritmo.nombre + " · Paso 0 de " + orden.size());
        botonPausa.setText("Pausar");
        reloj.setDelay(demora());
        reloj.start();
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
        progreso.setValue(paso);
        progreso.setString(paso + " / " + orden.size());
        titulo.setText("Algoritmo: " + algoritmoAnimado.nombre + " · Paso " + paso
                + " de " + orden.size());
    }

    private void terminarAnimacion() {
        if (!animando) return;
        reloj.stop();
        animando = false;
        panelPrincipal.setEstado(ids(resultadoAnimado.getVisitados()), -1,
                ids(resultadoAnimado.getCamino()));
        progreso.setValue(orden.size());
        progreso.setString(orden.size() + " / " + orden.size());
        titulo.setText("Algoritmo: " + algoritmoAnimado.nombre + " · Resultado final");
        estado.setText(resultadoAnimado.isEncontrado()
                ? "Camino de " + (resultadoAnimado.getCamino().size() - 1) + " movimientos."
                : "No existe camino entre la entrada y la salida.");
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
        if (laberinto == null) return;
        Dimension area = scrollPrincipal.getViewport().getExtentSize();
        if (area.width <= 0 || area.height <= 0) return;
        // Reserva espacio para márgenes y barras de desplazamiento.
        int ancho = Math.max(1, (area.width - 48) / laberinto.getColumnas());
        int alto = Math.max(1, (area.height - 48) / laberinto.getFilas());
        zoom.setValue(Math.max(24, Math.min(64, Math.min(ancho, alto))));
        panelPrincipal.setTamanoCelda(zoom.getValue());
    }

    private int demora() {
        return 25 + (100 - velocidad.getValue()) * 14;
    }

    private void actualizarControles() {
        boolean disponible = laberinto != null && !ocupado;
        for (JButton boton : new JButton[] { botonDFS, botonBFS, botonDijkstra,
                botonComparar }) {
            boton.setEnabled(disponible);
            boton.setToolTipText(laberinto == null ? "Primero carga un laberinto"
                    : ocupado ? "Espera a que termine la operación" : null);
        }
        botonPausa.setEnabled(animando);
        botonPaso.setEnabled(animando);
        botonFinal.setEnabled(animando);
        botonAjustar.setEnabled(laberinto != null);
        zoom.setEnabled(laberinto != null);
        velocidad.setEnabled(animando);
    }

    private void actualizarResultados() {
        resultadosPanel.removeAll();
        resultadosPanel.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        JLabel encabezado = etiqueta("RESULTADOS", FUENTE_TITULO);
        encabezado.setAlignmentX(LEFT_ALIGNMENT);
        resultadosPanel.add(encabezado);
        resultadosPanel.add(Box.createVerticalStrut(12));
        long mejorVisita = Long.MAX_VALUE;
        long mejorCamino = Long.MAX_VALUE;
        int cantidad = 0;
        int conCamino = 0;
        for (Resultado resultado : resultados.values()) {
            cantidad++;
            mejorVisita = Math.min(mejorVisita, resultado.getVisitados().size());
            if (resultado.isEncontrado()) {
                conCamino++;
                mejorCamino = Math.min(mejorCamino, resultado.getCamino().size() - 1);
            }
        }
        for (Algoritmo algoritmo : Algoritmo.values()) {
            Resultado resultado = resultados.get(algoritmo);
            JPanel tarjeta = new JPanel();
            tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
            tarjeta.setBackground(TARJETA);
            tarjeta.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(76, 94, 116)),
                    BorderFactory.createEmptyBorder(10, 11, 10, 11)));
            tarjeta.setAlignmentX(LEFT_ALIGNMENT);
            tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
            tarjeta.add(etiqueta(algoritmo.nombre, FUENTE_TITULO));
            tarjeta.add(Box.createVerticalStrut(6));
            if (resultado == null) {
                tarjeta.add(etiqueta("Pendiente", FUENTE));
            } else {
                tarjeta.add(etiqueta("Visitados: " + resultado.getVisitados().size(), FUENTE));
                tarjeta.add(Box.createVerticalStrut(3));
                tarjeta.add(etiqueta(resultado.isEncontrado()
                        ? "Longitud: " + (resultado.getCamino().size() - 1) + " movimientos"
                        : "Sin camino", FUENTE));
                tarjeta.add(Box.createVerticalStrut(3));
                tarjeta.add(etiqueta("Tiempo: " + tiempo(resultado), FUENTE));
                if (cantidad >= 2 && resultado.getVisitados().size() == mejorVisita) {
                    tarjeta.add(distincion("Menos visitados"));
                }
                if (conCamino >= 2 && resultado.isEncontrado()
                        && resultado.getCamino().size() - 1 == mejorCamino) {
                    tarjeta.add(distincion("Camino mínimo"));
                }
            }
            resultadosPanel.add(tarjeta);
            resultadosPanel.add(Box.createVerticalStrut(10));
        }
        resultadosPanel.revalidate();
        resultadosPanel.repaint();
    }

    private static JLabel distincion(String texto) {
        JLabel etiqueta = etiqueta("★ " + texto, FUENTE);
        etiqueta.setForeground(PanelLaberinto.SALIDA);
        return etiqueta;
    }

    private void mostrarTiempos() {
        StringBuilder texto = new StringBuilder("Búsqueda y reconstrucción:\n\n");
        for (Algoritmo algoritmo : Algoritmo.values()) {
            Resultado resultado = resultados.get(algoritmo);
            texto.append(algoritmo.nombre).append(": ")
                    .append(resultado == null ? "pendiente" : tiempo(resultado)).append('\n');
        }
        JOptionPane.showMessageDialog(this, texto.toString(), "Tiempos",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarError(Throwable error) {
        Throwable causa = error instanceof ExecutionException && error.getCause() != null
                ? error.getCause() : error;
        String motivo = causa.getMessage() == null ? causa.toString() : causa.getMessage();
        estado.setText("No se pudo cargar o resolver el laberinto.");
        JOptionPane.showMessageDialog(this, motivo, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static String tiempo(Resultado resultado) {
        return resultado.getTiempoNanos() + " ns ("
                + String.format(Locale.ROOT, "%.3f", resultado.getTiempoNanos() / 1_000_000.0)
                + " ms)";
    }

    private static Set<Integer> ids(List<Vertice> vertices) {
        Set<Integer> ids = new HashSet<>();
        for (Vertice vertice : vertices) ids.add(vertice.getId());
        return ids;
    }

    private static JLabel etiqueta(String texto, Font fuente) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente);
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    private static JButton boton(String texto) {
        JButton boton = new JButton(texto);
        boton.setFont(FUENTE);
        boton.setFocusPainted(false);
        boton.setBackground(new Color(53, 68, 87));
        boton.setForeground(TEXTO);
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(83, 105, 132)),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        return boton;
    }

    private static JSlider slider(int minimo, int maximo, int inicial) {
        JSlider slider = new JSlider(minimo, maximo, inicial);
        slider.setBackground(SUPERFICIE);
        slider.setPreferredSize(new Dimension(100, 28));
        return slider;
    }

    private static JLabel separador() {
        JLabel separador = etiqueta("│", FUENTE);
        separador.setForeground(SECUNDARIO);
        return separador;
    }

    private static JScrollPane scroll(PanelLaberinto panel) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.getViewport().setBackground(FONDO);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }
}
