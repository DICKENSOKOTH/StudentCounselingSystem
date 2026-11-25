package studentcounsellingsystem;

import forms.AdminPanel;
import forms.CounsellorPanel;
import forms.LoginPanel;
import forms.RegisterPanel;
import forms.StudentPanel;
import forms.WelcomePanel;
import java.awt.CardLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import models.User;
import utils.Session;

// the only window of the application: every screen is a JPanel (1100 x 700) shown with showScreen("NAME")
public class MainFrame extends javax.swing.JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final Map<String, JPanel> screens = new HashMap<>();
    private String current = "";
    private String selectedRole = User.STUDENT;   // chosen on the welcome page
    private Point dragOffset;

    public MainFrame() {
        setTitle("USIU-Africa | Student Counselling System");
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(cards);

        // screens that exist from the start
        addScreen("WELCOME", new WelcomePanel());
        addScreen("LOGIN", new LoginPanel());
        addScreen("REGISTER", new RegisterPanel());

        pack();                          // size comes from the panels (1100 x 700)
        setLocationRelativeTo(null);
        setupWindow();
        showScreen("WELCOME");
    }

    // add a screen, or replace the one with the same name
    public void addScreen(String name, JPanel panel) {
        JPanel old = screens.put(name, panel);
        if (old != null) { cards.remove(old); }
        cards.add(panel, name);
    }

    public void showScreen(String name) {
        if (!screens.containsKey(name)) {
            JOptionPane.showMessageDialog(this, "The " + name + " screen has not been added yet.");
            return;
        }
        current = name;
        cardLayout.show(cards, name);
    }

    public String getSelectedRole() { return selectedRole; }
    public void setSelectedRole(String role) { this.selectedRole = role; }

    // after a successful login: open the dashboard of that role
    public void openDashboard(User user) {
        Session.setUser(user);
        JPanel dashboard;   // the dashboards read the signed-in user from Session
        if (user.isStudent()) { dashboard = new StudentPanel(); }
        else if (user.isCounsellor()) { dashboard = new CounsellorPanel(); }
        else { dashboard = new AdminPanel(); }
        addScreen("DASHBOARD", dashboard);
        showScreen("DASHBOARD");
    }

    // back to the welcome page and forget the user
    public void logout() {
        Session.clear();
        showScreen("WELCOME");
        JPanel old = screens.remove("DASHBOARD");
        if (old != null) { cards.remove(old); }
    }

    // rounded corners (not when maximised), drag from anywhere, Esc leaves the welcome page
    private void setupWindow() {
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                // rounded corners only when the window is not maximised
                boolean maximised = (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
                try { setShape(maximised ? null : new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30)); }
                catch (UnsupportedOperationException ignored) { /* platform has no shaped windows */ }
            }
        });
        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragOffset = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragOffset != null && (getExtendedState() & MAXIMIZED_BOTH) != MAXIMIZED_BOTH) { setLocation(e.getXOnScreen() - dragOffset.x, e.getYOnScreen() - dragOffset.y); }
            }
        };
        cards.addMouseListener(drag);
        cards.addMouseMotionListener(drag);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "exit");
        getRootPane().getActionMap().put("exit", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { if ("WELCOME".equals(current)) { System.exit(0); } }
        });
    }
}
