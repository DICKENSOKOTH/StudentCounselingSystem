package utils;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// minimise + maximise/restore + close buttons (the window has no title bar)
public class WindowButtons extends JPanel {

    private final JButton[] buttons = new JButton[3];

    public WindowButtons() {
        super(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        setOpaque(false);
        String[] symbols = {"\u2013", "\u25A1", "\u00D7"};
        for (int i = 0; i < 3; i++) {
            final int kind = i;   // 0 minimise, 1 maximise / restore, 2 close
            JButton b = new JButton(symbols[i]);
            b.setFont(Theme.font(Font.BOLD, kind == 1 ? 18f : 22f));
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder());   // Nimbus pads buttons, which would turn the symbol into "..."
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setPreferredSize(new Dimension(28, 32));
            b.setToolTipText(kind == 0 ? "Minimise" : kind == 1 ? "Maximise / restore" : "Close");
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> {
                if (kind == 2) { System.exit(0); }
                java.awt.Window win = SwingUtilities.getWindowAncestor(b);
                if (!(win instanceof Frame)) { return; }
                Frame f = (Frame) win;
                if (kind == 0) { f.setState(Frame.ICONIFIED); return; }
                // maximise to the usable screen area (keeps the taskbar visible) or go back to the normal size
                if ((f.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                    f.setExtendedState(Frame.NORMAL);
                } else {
                    f.setMaximizedBounds(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
                    f.setExtendedState(Frame.MAXIMIZED_BOTH);
                }
            });
            buttons[i] = b;
            add(b);
        }
        setSymbolColor(Theme.NAVY);
    }

    public Color getSymbolColor() { return buttons[0].getForeground(); }

    public void setSymbolColor(Color color) {
        for (JButton b : buttons) { b.setForeground(color); }
    }
}
