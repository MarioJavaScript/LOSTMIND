import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

public final class MainSwing {
    private MainSwing() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("control", new Color(31, 38, 50));
            UIManager.put("info", new Color(31, 38, 50));
            UIManager.put("nimbusBase", new Color(42, 58, 76));
            UIManager.put("nimbusLightBackground", new Color(31, 38, 50));
            UIManager.put("text", new Color(230, 236, 245));
            new VentanaLaberinto().setVisible(true);
        });
    }
}
