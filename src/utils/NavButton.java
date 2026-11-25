package utils;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;

// menu entry in the sidebar (highlighted when its page is open)
public class NavButton extends JButton {

    private boolean active, over;

    public NavButton() { this("Menu"); }

    public NavButton(String text) {
        super(text);
        setFont(Theme.font(Font.BOLD, 14f));
        setForeground(Color.WHITE);
        setHorizontalAlignment(LEFT);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 10));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { over = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { over = false; repaint(); }
        });
    }

    public void setActive(boolean on) { active = on; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        if (active || over) {
            g2.setColor(new Color(255, 255, 255, active ? 34 : 16));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
        }
        if (active) {
            g2.setColor(Theme.GOLD);
            g2.fill(new RoundRectangle2D.Float(0, 9, 4, getHeight() - 18, 4, 4));
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
