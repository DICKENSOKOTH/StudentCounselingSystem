package utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

// small toolkit so every screen looks the same
public final class Ui {

    // date formats used in tables and lists
    public static final java.time.format.DateTimeFormatter DATE = java.time.format.DateTimeFormatter.ofPattern("EEE d MMM yyyy");
    public static final java.time.format.DateTimeFormatter TIME = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

    private Ui() { }

    // something that may fail (database, validation)
    public interface Task { void run() throws Exception; }

    //  messages 

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Student Counselling System", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // run a task and show any problem as a friendly message
    public static void run(Component parent, Task task) {
        try {
            task.run();
        } catch (SQLException ex) {
            error(parent, friendly(ex));
        } catch (Exception ex) {
            error(parent, ex.getMessage() == null ? "Something went wrong." : ex.getMessage());
        }
    }

    // turn database errors into plain language
    public static String friendly(SQLException ex) {
        String state = ex.getSQLState() == null ? "" : ex.getSQLState();
        if (state.startsWith("08") || ex.getMessage().contains("Could not connect") || ex.getMessage().contains("Communications link")) {
            return "Cannot reach the database.\nStart MySQL in XAMPP and import src/database/schema.sql.";
        }
        if (ex.getErrorCode() == 1062) { return "That value already exists (for example the email or registration number)."; }
        if (ex.getErrorCode() == 1451) { return "This record is still in use (it has appointments), so it cannot be deleted."; }
        return "Database error: " + ex.getMessage();
    }

    // show a form in a dialog; asks again while check() returns an error message
    public static boolean ask(Component parent, String title, JComponent body, Supplier<String> check) {
        while (true) {
            int choice = JOptionPane.showConfirmDialog(parent, body, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) { return false; }
            String problem = check == null ? null : check.get();
            if (problem == null) { return true; }
            error(parent, problem);
        }
    }

    //  text 

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(style, size));
        l.setForeground(color);
        return l;
    }

    public static JLabel heading(String text) { return label(text, Font.BOLD, 22f, Theme.NAVY); }
    public static JLabel muted(String text) { return label(text, Font.PLAIN, 13f, Theme.INK_MUTED); }

    //  inputs 

    private static final javax.swing.border.Border INPUT = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LINE), BorderFactory.createEmptyBorder(6, 10, 6, 10));

    // style a text field / password field that was placed in the Design tab
    public static void style(javax.swing.text.JTextComponent f) {
        f.setFont(Theme.font(Font.PLAIN, 14f));
        f.setBorder(INPUT);
    }

    // scroll pane around a table or text area
    public static void styleScroll(JScrollPane sp) {
        sp.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        sp.getViewport().setBackground(Color.WHITE);
    }

    public static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(Theme.font(Font.PLAIN, 14f));
        f.setBorder(INPUT);
        return f;
    }

    public static JPasswordField password(int columns) {
        JPasswordField f = new JPasswordField(columns);
        f.setFont(Theme.font(Font.PLAIN, 14f));
        f.setBorder(INPUT);
        return f;
    }

    public static JTextArea area(int rows, int columns) {
        JTextArea a = new JTextArea(rows, columns);
        a.setFont(Theme.font(Font.PLAIN, 14f));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        return a;
    }

    public static JScrollPane scrolled(JTextArea area) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        // keep the full number of rows even when a form is squeezed
        sp.setPreferredSize(new Dimension(300, area.getRows() * 20 + 14));
        sp.setMinimumSize(sp.getPreferredSize());
        return sp;
    }

    public static <T> JComboBox<T> combo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setFont(Theme.font(Font.PLAIN, 14f));
        return c;
    }

    // label + field rows for dialogs and profile pages
    public static class Form extends JPanel {
        private int row;

        public Form() {
            super(new GridBagLayout());
            setOpaque(false);
            // an invisible last row takes the spare height, so the fields stay at the top
            GridBagConstraints filler = new GridBagConstraints();
            filler.gridy = 1000; filler.weighty = 1;
            super.add(new JPanel() {{ setOpaque(false); }}, filler);
        }

        public void add(String label, JComponent field) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row++;
            c.insets = new Insets(5, 0, 5, 14);
            c.anchor = GridBagConstraints.NORTHWEST;
            c.gridx = 0;
            super.add(label(label, Font.BOLD, 13f, Theme.NAVY), c);
            c.gridx = 1; c.weightx = 1; c.insets = new Insets(5, 0, 5, 0); c.fill = GridBagConstraints.HORIZONTAL;
            super.add(field, c);
        }
    }

    //  buttons 

    // rounded flat button with a hover colour
    public static class PillButton extends JButton {
        private boolean over;

        public PillButton() { this("Button", Theme.GOLD, Theme.NAVY_DEEP); }   // for the Design tab

        public PillButton(String text, Color bg, Color fg) {
            super(text);
            setBackground(bg);
            setFont(Theme.font(Font.BOLD, 13f));
            setForeground(fg);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { over = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.hints(g2);
            Color c = !isEnabled() ? Theme.LINE : over ? Theme.mix(getBackground(), Color.WHITE, 0.2f) : getBackground();
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton button(String text, Color bg, Color fg) { return new PillButton(text, bg, fg); }
    public static JButton primary(String text) { return button(text, Theme.GOLD, Theme.NAVY_DEEP); }
    public static JButton secondary(String text) { return button(text, Theme.BLUE, Color.WHITE); }
    public static JButton danger(String text) { return button(text, Theme.DANGER, Color.WHITE); }
    public static JButton subtle(String text) { return button(text, new Color(0xE8, 0xEE, 0xF6), Theme.NAVY); }

    //  cards 

    public static class RoundPanel extends JPanel {
        private Color fill;

        public RoundPanel() { this(Color.WHITE); }   // for the Design tab

        public Color getFill() { return fill; }
        public void setFill(Color fill) { this.fill = fill; repaint(); }

        public RoundPanel(Color fill) {
            this.fill = fill;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.hints(g2);
            g2.setColor(new Color(0x1B, 0x3A, 0x66, 12));
            g2.fill(new RoundRectangle2D.Float(1, 3, getWidth() - 2, getHeight() - 3, 18, 18));
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 3, 18, 18));
            g2.setColor(Theme.LINE);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 3, 18, 18));
            g2.dispose();
        }
    }

    public static RoundPanel card() { return new RoundPanel(Color.WHITE); }

    //  tables 

    public static JTable table(String... columns) {
        JTable t = new JTable();
        styleTable(t, columns);
        return t;
    }

    // give any JTable the shared look and these column titles
    public static void styleTable(JTable t, String... columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        t.setModel(model);
        t.setRowHeight(38);
        t.setFont(Theme.font(Font.PLAIN, 13f));
        t.setForeground(Theme.NAVY);
        t.setShowVerticalLines(false);
        t.setGridColor(Theme.LINE);
        t.setSelectionBackground(new Color(0xFF, 0xEB, 0xBF));
        t.setSelectionForeground(Theme.NAVY);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);                 // click a header to sort
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(100, 40));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setBackground(Theme.NAVY);
                setForeground(Color.WHITE);
                setFont(Theme.font(Font.BOLD, 12f));
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!sel) { setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF8, 0xFA, 0xFD)); }
                setForeground(Theme.NAVY);
                String text = v == null ? "" : v.toString();
                switch (text) {   // coloured statuses
                    case "PENDING": setForeground(new Color(0xB9, 0x7A, 0x00)); setFont(Theme.font(Font.BOLD, 12f)); break;
                    case "APPROVED": setForeground(new Color(0x0E, 0x8F, 0x84)); setFont(Theme.font(Font.BOLD, 12f)); break;
                    case "COMPLETED": setForeground(Theme.SUCCESS); setFont(Theme.font(Font.BOLD, 12f)); break;
                    case "CANCELLED": setForeground(Theme.DANGER); setFont(Theme.font(Font.BOLD, 12f)); break;
                    default: setFont(Theme.font(Font.PLAIN, 13f));
                }
                if (text.startsWith("\u2605")) { setForeground(new Color(0xE0, 0x9A, 0x00)); }   // star ratings
                return this;
            }
        });
    }

    // column widths in pixels (the last columns share what is left)
    public static void widths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) { table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]); }
    }

    public static void setRows(JTable table, List<Object[]> rows) {
        DefaultTableModel m = (DefaultTableModel) table.getModel();
        m.setRowCount(0);
        for (Object[] r : rows) { m.addRow(r); }
    }

    // record navigation: First, Previous, Next, Last
    public static void first(JTable t) { select(t, 0); }
    public static void last(JTable t) { select(t, t.getRowCount() - 1); }
    public static void next(JTable t) { select(t, t.getSelectedRow() + 1); }
    public static void previous(JTable t) { select(t, t.getSelectedRow() < 0 ? 0 : t.getSelectedRow() - 1); }

    private static void select(JTable t, int row) {
        if (t.getRowCount() == 0) { return; }
        row = Math.max(0, Math.min(row, t.getRowCount() - 1));
        t.setRowSelectionInterval(row, row);
        t.scrollRectToVisible(t.getCellRect(row, 0, true));
    }

    // index of the selected row in the data list (-1 when nothing is selected)
    public static int selected(JTable table) {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : table.convertRowIndexToModel(row);
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // right-click menu for a table: names + actions
    public static void rowMenu(JTable table, String[] names, Runnable[] actions) {
        JPopupMenu menu = new JPopupMenu();
        for (int i = 0; i < names.length; i++) {
            JMenuItem item = new JMenuItem(names[i]);
            item.setFont(Theme.font(Font.PLAIN, 13f));
            final Runnable action = actions[i];
            item.addActionListener(e -> action.run());
            menu.add(item);
        }
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { popup(e); }
            @Override public void mouseReleased(MouseEvent e) { popup(e); }
            private void popup(MouseEvent e) {
                if (!e.isPopupTrigger()) { return; }
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) { table.setRowSelectionInterval(row, row); menu.show(table, e.getX(), e.getY()); }
            }
        });
    }

    // a row of buttons
    public static JPanel buttons(JButton... buttons) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        for (JButton b : buttons) { p.add(b); }
        return p;
    }

    //  printing 

    // print a table with a title and the date (opens the normal print dialog, "Print to PDF" works too)
    public static void printTable(Component parent, JTable table, String title) {
        try {
            java.text.MessageFormat header = new java.text.MessageFormat(title + "  -  " + java.time.LocalDate.now().format(DATE));
            java.text.MessageFormat footer = new java.text.MessageFormat("USIU-Africa Student Counselling System  -  page {0}");
            table.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (java.awt.print.PrinterException | java.awt.HeadlessException ex) {
            error(parent, "The table could not be printed: " + ex.getMessage());
        }
    }

    // window buttons (the window has no title bar) 

    public static JPanel windowButtons(Color color) {
        WindowButtons w = new WindowButtons();
        w.setSymbolColor(color);
        return w;
    }
}
