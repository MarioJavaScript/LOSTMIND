public class LaberintoInvalidoException extends Exception {
    public LaberintoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public LaberintoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
