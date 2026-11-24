import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.util.Enumeration;

public final class MainSwing {
    private MainSwing() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FontUIResource fuente = new FontUIResource(Font.SANS_SERIF, Font.PLAIN, 13);
            Enumeration<Object> claves = UIManager.getDefaults().keys();
            while (claves.hasMoreElements()) {
                Object clave = claves.nextElement();
                if (UIManager.get(clave) instanceof FontUIResource) {
                    UIManager.put(clave, fuente);
                }
            }
            UIManager.put("control", new Color(31, 38, 50));
            UIManager.put("info", new Color(31, 38, 50));
            UIManager.put("nimbusBase", new Color(42, 58, 76));
            UIManager.put("nimbusLightBackground", new Color(31, 38, 50));
            UIManager.put("text", new Color(230, 236, 245));
            new VentanaLaberinto().setVisible(true);
        });
    }
}
