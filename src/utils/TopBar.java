package utils;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;

// white strip with a thin line underneath (page title bar)
public class TopBar extends JPanel {

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.LINE);
        g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
    }
}
