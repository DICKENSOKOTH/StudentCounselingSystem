package utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JLabel;

// a big number with a caption and a coloured edge (dashboard statistics)
public class StatCard extends Ui.RoundPanel {

    private final JLabel caption = Ui.muted("CAPTION");
    private final JLabel value = new JLabel("0");
    private Color accent = Theme.TEAL;

    public StatCard() {
        setLayout(new BorderLayout(0, 4));
        value.setFont(Theme.font(Font.BOLD, 34f));
        value.setForeground(Theme.NAVY);
        add(caption, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
    }

    public String getCaption() { return caption.getText(); }
    public void setCaption(String text) { caption.setText(text.toUpperCase()); }
    public String getValue() { return value.getText(); }
    public void setValue(String text) { value.setText(text); }
    public Color getAccent() { return accent; }
    public void setAccent(Color accent) { this.accent = accent; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(accent);
        g2.fill(new RoundRectangle2D.Float(0, 14, 5, getHeight() - 31, 5, 5));   // coloured edge
        g2.dispose();
    }
}
