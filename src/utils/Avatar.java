package utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;

// round badge with the first letter of a name
public class Avatar extends JComponent {

    private String letter = "?";
    private Color color = Theme.TEAL;

    public Avatar() { setPreferredSize(new Dimension(42, 42)); }

    // the badge shows the first letter of the name
    public void setName(String name) {
        super.setName(name);
        letter = name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        repaint();
    }

    public Color getBadgeColor() { return color; }
    public void setBadgeColor(Color color) { this.color = color; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        g2.setColor(color);
        g2.fill(new Ellipse2D.Float(0, 0, 41, 41));
        g2.setColor(Theme.NAVY_DEEP);
        g2.setFont(Theme.font(Font.BOLD, 18f));
        int w = g2.getFontMetrics().stringWidth(letter);
        g2.drawString(letter, (42 - w) / 2f, 28);
        g2.dispose();
    }
}
