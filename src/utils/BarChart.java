package utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

// a small animated bar chart (no chart library needed)
public class BarChart extends JComponent {

    private final List<String> labels = new ArrayList<>();
    private final List<Integer> values = new ArrayList<>();
    private final List<Color> colors = new ArrayList<>();
    private float progress = 1f;   // 0..1, drives the grow-in animation

    public BarChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(360, 200));
        setMinimumSize(new Dimension(120, 120));
        // in the Design tab there is no database, so show sample bars to make the chart visible
        if (java.beans.Beans.isDesignTime()) {
            labels.addAll(java.util.Arrays.asList("A", "B", "C", "D"));
            values.addAll(java.util.Arrays.asList(4, 7, 5, 9));
            colors.addAll(java.util.Arrays.asList(Theme.GOLD, Theme.TEAL, Theme.SUCCESS, Theme.BLUE));
        }
    }

    // replace the data and play the grow-in animation
    public void setData(List<String> newLabels, List<Integer> newValues, List<Color> newColors) {
        labels.clear(); labels.addAll(newLabels);
        values.clear(); values.addAll(newValues);
        colors.clear(); colors.addAll(newColors);
        progress = 0f;
        Timer t = new Timer(16, null);
        t.addActionListener(e -> {
            progress = Math.min(1f, progress + 0.06f);
            repaint();
            if (progress >= 1f) { t.stop(); }
        });
        t.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        int w = getWidth(), h = getHeight(), n = values.size();
        int max = 1, sum = 0;
        for (int v : values) { max = Math.max(max, v); sum += v; }
        if (n == 0 || sum == 0) {   // nothing to draw yet: say so instead of staying blank
            g2.setFont(Theme.font(Font.PLAIN, 13f));
            g2.setColor(Theme.INK_MUTED);
            String msg = "No data yet";
            g2.drawString(msg, (w - g2.getFontMetrics().stringWidth(msg)) / 2, h / 2);
            if (n == 0) { g2.dispose(); return; }
        }
        int top = 26, bottom = 28, slot = w / n, barW = Math.min(46, slot - 18);
        // base line
        g2.setColor(Theme.LINE);
        g2.drawLine(0, h - bottom, w, h - bottom);
        for (int i = 0; i < n; i++) {
            int bh = Math.max(0, (int) ((h - top - bottom) * (values.get(i) / (double) max) * progress));
            int x = i * slot + (slot - barW) / 2, y = h - bottom - bh;
            g2.setColor(colors.isEmpty() ? Theme.TEAL : colors.get(i % colors.size()));
            g2.fill(new RoundRectangle2D.Float(x, y, barW, Math.max(bh, 3), 10, 10));
            // value on top, label below
            g2.setFont(Theme.font(Font.BOLD, 13f));
            g2.setColor(Theme.NAVY);
            String v = String.valueOf(Math.round(values.get(i) * progress));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(v, x + (barW - fm.stringWidth(v)) / 2, y - 6);
            g2.setFont(Theme.font(Font.PLAIN, 12f));
            g2.setColor(Theme.INK_MUTED);
            fm = g2.getFontMetrics();
            g2.drawString(labels.get(i), i * slot + (slot - fm.stringWidth(labels.get(i))) / 2, h - 8);
        }
        g2.dispose();
    }
}
