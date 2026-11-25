package utils;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

// vertical gradient background (the navy sidebar)
public class GradientPanel extends JPanel {

    private Color top = Theme.NAVY_DEEP;
    private Color bottom = Theme.NAVY;

    public Color getTopColor() { return top; }
    public void setTopColor(Color c) { top = c; repaint(); }
    public Color getBottomColor() { return bottom; }
    public void setBottomColor(Color c) { bottom = c; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
