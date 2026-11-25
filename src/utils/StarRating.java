package utils;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

// five clickable stars (1 to 5)
public class StarRating extends JComponent {

    private int rating;
    private int hover;   // star under the mouse, 0 when none

    public StarRating() { this(5); }   // for the Design tab

    public StarRating(int rating) {
        this.rating = rating;
        setPreferredSize(new Dimension(5 * 38, 38));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { StarRating.this.rating = starAt(e.getX()); repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = 0; repaint(); }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hover = starAt(e.getX()); repaint(); }
        });
    }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; repaint(); }

    private int starAt(int x) { return Math.max(1, Math.min(5, x / 38 + 1)); }

    // text version for tables: ★★★★☆
    public static String stars(Integer rating) {
        if (rating == null) { return "-"; }
        return "\u2605".repeat(rating) + "\u2606".repeat(5 - rating);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        int shown = hover > 0 ? hover : rating;
        for (int i = 1; i <= 5; i++) {
            g2.setColor(i <= shown ? Theme.GOLD : Theme.LINE);
            g2.fill(star((i - 1) * 38 + 19, 19, 16, 7));
        }
        g2.dispose();
    }

    // five-pointed star around (cx, cy)
    private Path2D star(double cx, double cy, double outer, double inner) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? outer : inner, a = Math.PI / 5 * i - Math.PI / 2;
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            if (i == 0) { p.moveTo(x, y); } else { p.lineTo(x, y); }
        }
        p.closePath();
        return p;
    }
}
