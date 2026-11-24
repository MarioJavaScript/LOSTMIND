public enum TemaConsola {
    ASCII(' ', 'E', 'S', '.', '@', '*'),
    UNICODE(' ', '▶', '◆', '·', '◉', '●');

    private final char transitable;
    private final char entrada;
    private final char salida;
    private final char visitado;
    private final char actual;
    private final char camino;

    TemaConsola(char transitable, char entrada, char salida, char visitado,
                char actual, char camino) {
        this.transitable = transitable;
        this.entrada = entrada;
        this.salida = salida;
        this.visitado = visitado;
        this.actual = actual;
        this.camino = camino;
    }

    public char transitable() { return transitable; }
    public char entrada() { return entrada; }
    public char salida() { return salida; }
    public char visitado() { return visitado; }
    public char actual() { return actual; }
    public char camino() { return camino; }

    public String leyenda() {
        return this == ASCII
                ? "Pared: + - |  Transitable: espacio  Entrada: E  Salida: S"
                        + "  Visitado: .  Actual: @  Camino final: *"
                : "Pared: ─ │ ┌ ┐ └ ┘ ├ ┤ ┬ ┴ ┼  Transitable: espacio"
                        + "  Entrada: ▶  Salida: ◆  Visitado: ·  Actual: ◉  Camino final: ●";
    }

    public char pared(char[][] matriz, int fila, int columna) {
        char original = matriz[fila][columna];
        if (this == ASCII) {
            return original;
        }
        if (original == '-') {
            return '─';
        }
        if (original == '|') {
            return '│';
        }
        // Cada bit representa una pared vecina: arriba, derecha, abajo, izquierda.
        int vecinos = 0;
        if (hayPared(matriz, fila - 1, columna)) vecinos |= 1;
        if (hayPared(matriz, fila, columna + 1)) vecinos |= 2;
        if (hayPared(matriz, fila + 1, columna)) vecinos |= 4;
        if (hayPared(matriz, fila, columna - 1)) vecinos |= 8;
        return switch (vecinos) {
            case 6 -> '┌';
            case 12 -> '┐';
            case 3 -> '└';
            case 9 -> '┘';
            case 7 -> '├';
            case 13 -> '┤';
            case 14 -> '┬';
            case 11 -> '┴';
            case 2, 8, 10 -> '─';
            case 1, 4, 5 -> '│';
            default -> '┼';
        };
    }

    private static boolean hayPared(char[][] matriz, int fila, int columna) {
        if (fila < 0 || fila >= matriz.length || columna < 0
                || columna >= matriz[fila].length) {
            return false;
        }
        char celda = matriz[fila][columna];
        return celda == '+' || celda == '-' || celda == '|';
    }
}
