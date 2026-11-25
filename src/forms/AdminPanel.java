package forms;

import dao.AppointmentDAO;
import dao.FeedbackDAO;
import dao.ReportDAO;
import dao.UserDAO;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.JPasswordField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import models.Appointment;
import models.Feedback;
import models.User;
import studentcounsellingsystem.MainFrame;
import utils.Session;
import utils.StarRating;
import utils.Theme;
import utils.Ui;
import utils.Validate;

// admin portal: sidebar menu + 4 pages, all designed in the Design tab (overview with print, users, appointments, feedback)
public class AdminPanel extends javax.swing.JPanel {

    private final UserDAO userDAO = new UserDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private List<User> allUsers = new ArrayList<>();
    private List<User> shownUsers = new ArrayList<>();
    private List<Appointment> allAppointments = new ArrayList<>();
    private List<Appointment> shownAppointments = new ArrayList<>();
    private List<Feedback> feedbackList = new ArrayList<>();
    private final User user = Session.getUser();
    private final java.util.Map<String, utils.NavButton> navButtons = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, String[]> pageText = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Runnable> refreshers = new java.util.LinkedHashMap<>();

    public AdminPanel() {
        initComponents();
        applyTheme();
        refreshUserCard();
        lblRole.setText("Admin");
        lblDate.setText(LocalDate.now().format(Ui.DATE));

        // the menu: button, page title, subtitle and what to load when the page opens
        addPage("OVERVIEW", navOverview, "Overview", "How the counselling centre is doing", this::refreshOverview);
        addPage("USERS", navUsers, "Users", "Add, edit, reset passwords and remove accounts", this::refreshUsers);
        addPage("APPOINTMENTS", navAppointments, "Appointments", "All bookings in the system", this::refreshAppointments);
        addPage("FEEDBACK", navFeedback, "Feedback", "Ratings and comments from students", this::refreshFeedback);
        showPage("OVERVIEW");
    }

    // look of the tables and fields (the layout itself is in the Design tab) 

    private void applyTheme() {
        Ui.styleTable(tblWorkload, "Counsellor", "Total", "Pending", "Approved", "Completed", "Cancelled", "Avg rating");
        Ui.styleTable(tblUsers, "Name", "Email", "Role", "Phone", "Joined");
        Ui.styleTable(tblAppointments, "Date", "Time", "Student", "Counsellor", "Reason", "Status", "Rating");
        Ui.styleTable(tblFeedback, "Session date", "Student", "Counsellor", "Rating", "Comments");
        for (javax.swing.JScrollPane sp : new javax.swing.JScrollPane[]{scrollWorkload, scrollUsers, scrollAppts, scrollFeedback}) { Ui.styleScroll(sp); }
        tblWorkload.setRowHeight(30);
        Ui.style(txtSearch);
        txtSearch.setToolTipText("Search by name or email");
        // search while typing
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { showUsers(); }
            @Override public void removeUpdate(DocumentEvent e) { showUsers(); }
            @Override public void changedUpdate(DocumentEvent e) { showUsers(); }
        });
        // right-click menus
        Ui.rowMenu(tblUsers, new String[]{"Edit", "Reset password", "Delete"}, new Runnable[]{this::editUser, this::resetPassword, this::deleteUser});
        Ui.rowMenu(tblAppointments, new String[]{"Edit", "Delete"}, new Runnable[]{this::editAppointment, this::deleteAppointment});
        Ui.rowMenu(tblFeedback, new String[]{"Edit", "Delete"}, new Runnable[]{this::editFeedback, this::deleteFeedback});
    }

    //  sidebar menu: which button opens which page 

    private void addPage(String key, utils.NavButton button, String title, String subtitle, Runnable refresh) {
        navButtons.put(key, button);
        pageText.put(key, new String[]{title, subtitle});
        refreshers.put(key, refresh);
    }

    private void showPage(String key) {
        for (java.util.Map.Entry<String, utils.NavButton> e : navButtons.entrySet()) { e.getValue().setActive(e.getKey().equals(key)); }
        lblPageTitle.setText(pageText.get(key)[0]);
        lblPageSubtitle.setText(pageText.get(key)[1]);
        refreshers.get(key).run();
        ((java.awt.CardLayout) pages.getLayout()).show(pages, key);
    }

    // show the name of the signed-in user in the sidebar
    private void refreshUserCard() {
        lblUserName.setText(user.getFullName());
        avatar.setName(user.getFullName());
    }

    private MainFrame frame() {
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
        return w instanceof MainFrame ? (MainFrame) w : null;
    }

    // back to the welcome page
    private void logout() {
        if (frame() != null) { frame().logout(); }
    }

    //  what the buttons do 

    private void refreshOverview() {
        Ui.run(this, () -> {
            cardStudents.setValue(String.valueOf(reportDAO.countStudents()));
            cardCounsellors.setValue(String.valueOf(reportDAO.countCounsellors()));
            cardAppointments.setValue(String.valueOf(reportDAO.countAppointments()));

            // appointments by status: look every status up by name so the bars always match their labels
            Map<String, Integer> status = reportDAO.appointmentsByStatus();
            int pending = statusCount(status, "PENDING"), approved = statusCount(status, "APPROVED");
            int completed = statusCount(status, "COMPLETED"), cancelled = statusCount(status, "CANCELLED");
            statusChart.setData(Arrays.asList("Pending", "Approved", "Completed", "Cancelled"), Arrays.asList(pending, approved, completed, cancelled),
                    Arrays.asList(Theme.GOLD, Theme.TEAL, Theme.SUCCESS, Theme.DANGER));
            int total = pending + approved + completed + cancelled;
            cardPending.setValue(String.valueOf(pending));
            cardCompletion.setValue(total == 0 ? "-" : Math.round(completed * 100.0 / total) + "%");

            // ratings: always show all five bars, even when a star level has no votes yet
            Map<Integer, Integer> spread = reportDAO.ratingSpread();
            List<Integer> stars = new ArrayList<>();
            int votes = 0, weighted = 0;
            for (int s = 1; s <= 5; s++) {
                int n = spread.getOrDefault(s, 0);
                stars.add(n);
                votes += n;
                weighted += s * n;
            }
            ratingChart.setData(Arrays.asList("1", "2", "3", "4", "5"), stars,
                    Arrays.asList(Theme.DANGER, Theme.DANGER, Theme.GOLD, Theme.TEAL, Theme.SUCCESS));
            cardRating.setValue(votes == 0 ? "-" : String.format("%.1f", weighted / (double) votes));

            // trend: appointments booked for each of the last six months
            allAppointments = appointmentDAO.findAll();
            java.time.YearMonth thisMonth = java.time.YearMonth.now();
            List<String> months = new ArrayList<>();
            List<Integer> perMonth = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                java.time.YearMonth m = thisMonth.minusMonths(i);
                int n = 0;
                for (Appointment a : allAppointments) {
                    if (a.getAppointmentDate() != null && java.time.YearMonth.from(a.getAppointmentDate()).equals(m)) { n++; }
                }
                months.add(m.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH));
                perMonth.add(n);
            }
            monthChart.setData(months, perMonth, new ArrayList<>(java.util.Collections.nCopies(6, Theme.TEAL)));

            Ui.setRows(tblWorkload, reportDAO.counsellorWorkload());
            Ui.widths(tblWorkload, 200, 70, 80, 80, 90, 90, 90);
        });
    }

    // how many appointments have this status (not case sensitive, 0 when none)
    private static int statusCount(Map<String, Integer> map, String name) {
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) { return e.getValue(); }
        }
        return 0;
    }

    private void refreshUsers() {
        Ui.run(this, () -> { allUsers = userDAO.findAll(); showUsers(); });
    }

    // search by name or email, and by role
    private void showUsers() {
        String q = txtSearch.getText().trim().toLowerCase();
        String role = (String) cmbRole.getSelectedItem();
        shownUsers = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();
        for (User u : allUsers) {
            boolean roleOk = role == null || role.equals("All roles") || role.equals(u.getRole());
            boolean textOk = q.isEmpty() || u.getFullName().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q);
            if (!roleOk || !textOk) { continue; }
            shownUsers.add(u);
            rows.add(new Object[]{u.getFullName(), u.getEmail(), u.getRole(), u.getPhone() == null ? "" : u.getPhone(), u.getCreatedAt() == null ? "" : u.getCreatedAt().toLocalDateTime().toLocalDate().format(Ui.DATE)});
        }
        Ui.setRows(tblUsers, rows);
        Ui.widths(tblUsers, 160, 210, 110, 130, 140);
    }

    private User selectedUser() {
        int i = Ui.selected(tblUsers);
        if (i < 0) { Ui.info(this, "Select a user in the table first."); return null; }
        return shownUsers.get(i);
    }

    private void editUser() {
        User u = selectedUser();
        if (u != null && UserForm.edit(this, u)) { refreshUsers(); }
    }

    private void resetPassword() {
        User u = selectedUser();
        if (u == null) { return; }
        JPasswordField pw = Ui.password(20);
        Ui.Form f = new Ui.Form();
        f.add("New password", pw);
        f.setPreferredSize(new Dimension(340, 50));
        boolean ok = Ui.ask(this, "Reset password for " + u.getFullName(), f, () -> {
            String p = new String(pw.getPassword());
            if (Validate.password(p) != null) { return Validate.password(p); }
            try { userDAO.updatePassword(u.getUserId(), p); return null; }
            catch (java.sql.SQLException ex) { return Ui.friendly(ex); }
        });
        if (ok) { Ui.info(this, "The password of " + u.getFullName() + " was changed."); }
    }

    private void deleteUser() {
        User u = selectedUser();
        if (u == null) { return; }
        if (u.getUserId() == user.getUserId()) { Ui.info(this, "You cannot delete the account you are signed in with."); return; }
        if (Ui.confirm(this, "Delete " + u.getFullName() + " (" + u.getRole().toLowerCase() + ")?\nTheir profile is deleted with the account.")) {
            Ui.run(this, () -> { userDAO.delete(u.getUserId()); refreshUsers(); });
        }
    }

    private void refreshAppointments() {
        Ui.run(this, () -> { allAppointments = appointmentDAO.findAll(); showAppointments(); });
    }

    private void showAppointments() {
        String f = (String) cmbStatus.getSelectedItem();
        shownAppointments = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();
        for (Appointment a : allAppointments) {
            if (f != null && !f.equals("All") && !f.equals(a.getStatus())) { continue; }
            shownAppointments.add(a);
            rows.add(new Object[]{a.getAppointmentDate().format(Ui.DATE), a.getAppointmentTime().format(Ui.TIME), a.getStudentName(), a.getCounsellorName(), a.getReason(), a.getStatus(), StarRating.stars(a.getRating())});
        }
        Ui.setRows(tblAppointments, rows);
        Ui.widths(tblAppointments, 140, 60, 120, 120, 170, 100, 90);
    }

    private Appointment selectedAppointment() {
        int i = Ui.selected(tblAppointments);
        if (i < 0) { Ui.info(this, "Select an appointment in the table first."); return null; }
        return shownAppointments.get(i);
    }

    private void editAppointment() {
        Appointment a = selectedAppointment();
        if (a != null && AppointmentForm.adminEdit(this, a)) { refreshAppointments(); }
    }

    private void deleteAppointment() {
        Appointment a = selectedAppointment();
        if (a == null) { return; }
        if (Ui.confirm(this, "Delete the appointment of " + a.getStudentName() + " on " + a.getAppointmentDate().format(Ui.DATE) + "?")) {
            Ui.run(this, () -> { appointmentDAO.delete(a.getAppointmentId()); refreshAppointments(); });
        }
    }

    private void refreshFeedback() {
        Ui.run(this, () -> {
            feedbackList = feedbackDAO.findAll();
            List<Object[]> rows = new ArrayList<>();
            for (Feedback f : feedbackList) { rows.add(new Object[]{f.getAppointmentDate().format(Ui.DATE), f.getStudentName(), f.getCounsellorName(), StarRating.stars(f.getRating()), f.getComments()}); }
            Ui.setRows(tblFeedback, rows);
            Ui.widths(tblFeedback, 140, 130, 130, 100, 280);
        });
    }

    private Feedback selectedFeedback() {
        int i = Ui.selected(tblFeedback);
        if (i < 0) { Ui.info(this, "Select a feedback row in the table first."); return null; }
        return feedbackList.get(i);
    }

    private void editFeedback() {
        Feedback f = selectedFeedback();
        if (f != null && FeedbackForm.show(this, f, false)) { refreshFeedback(); }
    }

    private void deleteFeedback() {
        Feedback f = selectedFeedback();
        if (f != null && Ui.confirm(this, "Delete this feedback from " + f.getStudentName() + "?")) {
            Ui.run(this, () -> { feedbackDAO.delete(f.getFeedbackId()); refreshFeedback(); });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pageOverview = new javax.swing.JPanel();
        statsPanel = new javax.swing.JPanel();
        cardStudents = new utils.StatCard();
        cardCounsellors = new utils.StatCard();
        cardAppointments = new utils.StatCard();
        cardRating = new utils.StatCard();
        cardCompletion = new utils.StatCard();
        cardPending = new utils.StatCard();
        chartsPanel = new javax.swing.JPanel();
        statusCard = new utils.Ui.RoundPanel();
        lblStatusTitle = new javax.swing.JLabel();
        statusChart = new utils.BarChart();
        ratingCard = new utils.Ui.RoundPanel();
        lblRatingTitle = new javax.swing.JLabel();
        ratingChart = new utils.BarChart();
        monthCard = new utils.Ui.RoundPanel();
        lblMonthTitle = new javax.swing.JLabel();
        monthChart = new utils.BarChart();
        workloadCard = new utils.Ui.RoundPanel();
        workloadHeader = new javax.swing.JPanel();
        lblWorkloadTitle = new javax.swing.JLabel();
        btnPrintReport = new utils.Ui.PillButton();
        scrollWorkload = new javax.swing.JScrollPane();
        tblWorkload = new javax.swing.JTable();
        sidebar = new utils.GradientPanel();
        lblLogo = new javax.swing.JLabel();
        lblPortal = new javax.swing.JLabel();
        navOverview = new utils.NavButton();
        navUsers = new utils.NavButton();
        navAppointments = new utils.NavButton();
        navFeedback = new utils.NavButton();
        avatar = new utils.Avatar();
        lblUserName = new javax.swing.JLabel();
        lblRole = new javax.swing.JLabel();
        btnLogout = new utils.Ui.PillButton();
        mainPanel = new javax.swing.JPanel();
        topBar = new utils.TopBar();
        titles = new javax.swing.JPanel();
        lblPageTitle = new javax.swing.JLabel();
        lblPageSubtitle = new javax.swing.JLabel();
        topRight = new javax.swing.JPanel();
        lblDate = new javax.swing.JLabel();
        windowButtons = new utils.WindowButtons();
        pages = new javax.swing.JPanel();
        pageUsers = new javax.swing.JPanel();
        toolbarPanelUsers = new javax.swing.JPanel();
        filterPanelUsers = new javax.swing.JPanel();
        txtSearch = new javax.swing.JTextField();
        cmbRole = new javax.swing.JComboBox<String>();
        buttonBarUsers = new javax.swing.JPanel();
        btnUserAdd = new utils.Ui.PillButton();
        btnUserEdit = new utils.Ui.PillButton();
        btnUserReset = new utils.Ui.PillButton();
        btnUserDelete = new utils.Ui.PillButton();
        btnUserPrint = new utils.Ui.PillButton();
        scrollUsers = new javax.swing.JScrollPane();
        tblUsers = new javax.swing.JTable();
        pageAppointments = new javax.swing.JPanel();
        toolbarPanelAppts = new javax.swing.JPanel();
        filterPanelAppts = new javax.swing.JPanel();
        lblStatus = new javax.swing.JLabel();
        cmbStatus = new javax.swing.JComboBox<String>();
        buttonBarAppts = new javax.swing.JPanel();
        btnApptEdit = new utils.Ui.PillButton();
        btnApptDelete = new utils.Ui.PillButton();
        btnApptPrint = new utils.Ui.PillButton();
        scrollAppts = new javax.swing.JScrollPane();
        tblAppointments = new javax.swing.JTable();
        lblTip = new javax.swing.JLabel();
        pageFeedback = new javax.swing.JPanel();
        toolbarPanelFeedback = new javax.swing.JPanel();
        buttonBarFeedback = new javax.swing.JPanel();
        btnFeedbackEdit = new utils.Ui.PillButton();
        btnFeedbackDelete = new utils.Ui.PillButton();
        btnFeedbackPrint = new utils.Ui.PillButton();
        scrollFeedback = new javax.swing.JScrollPane();
        tblFeedback = new javax.swing.JTable();

        pageOverview.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageOverview.setOpaque(false);
        pageOverview.setLayout(new java.awt.BorderLayout(0, 16));

        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new java.awt.Dimension(100, 100));
        statsPanel.setLayout(new java.awt.GridLayout(1, 6, 16, 0));

        cardStudents.setCaption("Students");
        statsPanel.add(cardStudents);

        cardCounsellors.setCaption("Counsellors");
        cardCounsellors.setAccent(new java.awt.Color(245, 179, 53));
        statsPanel.add(cardCounsellors);

        cardAppointments.setCaption("Appointments");
        cardAppointments.setAccent(new java.awt.Color(26, 75, 132));
        statsPanel.add(cardAppointments);

        cardRating.setCaption("Average rating");
        cardRating.setAccent(new java.awt.Color(168, 139, 255));
        statsPanel.add(cardRating);

        cardCompletion.setCaption("Completion rate");
        cardCompletion.setAccent(new java.awt.Color(46, 160, 110));
        statsPanel.add(cardCompletion);

        cardPending.setCaption("Pending requests");
        cardPending.setAccent(new java.awt.Color(245, 179, 53));
        statsPanel.add(cardPending);

        pageOverview.add(statsPanel, java.awt.BorderLayout.NORTH);

        chartsPanel.setOpaque(false);
        chartsPanel.setLayout(new java.awt.GridLayout(1, 3, 16, 0));

        statusCard.setLayout(new java.awt.BorderLayout(0, 8));

        lblStatusTitle.setForeground(new java.awt.Color(12, 42, 77));
        lblStatusTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblStatusTitle.setText("Appointments by status");
        statusCard.add(lblStatusTitle, java.awt.BorderLayout.NORTH);
        statusCard.add(statusChart, java.awt.BorderLayout.CENTER);

        chartsPanel.add(statusCard);

        ratingCard.setLayout(new java.awt.BorderLayout(0, 8));

        lblRatingTitle.setForeground(new java.awt.Color(12, 42, 77));
        lblRatingTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblRatingTitle.setText("Feedback ratings (1 to 5 stars)");
        ratingCard.add(lblRatingTitle, java.awt.BorderLayout.NORTH);
        ratingCard.add(ratingChart, java.awt.BorderLayout.CENTER);

        chartsPanel.add(ratingCard);

        monthCard.setLayout(new java.awt.BorderLayout(0, 8));

        lblMonthTitle.setForeground(new java.awt.Color(12, 42, 77));
        lblMonthTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblMonthTitle.setText("Appointments per month");
        monthCard.add(lblMonthTitle, java.awt.BorderLayout.NORTH);
        monthCard.add(monthChart, java.awt.BorderLayout.CENTER);

        chartsPanel.add(monthCard);

        pageOverview.add(chartsPanel, java.awt.BorderLayout.CENTER);

        workloadCard.setPreferredSize(new java.awt.Dimension(100, 190));
        workloadCard.setLayout(new java.awt.BorderLayout(0, 8));

        workloadHeader.setOpaque(false);
        workloadHeader.setLayout(new java.awt.BorderLayout());

        lblWorkloadTitle.setForeground(new java.awt.Color(12, 42, 77));
        lblWorkloadTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblWorkloadTitle.setText("Counsellor workload");
        workloadHeader.add(lblWorkloadTitle, java.awt.BorderLayout.WEST);

        btnPrintReport.setBackground(new java.awt.Color(232, 238, 246));
        btnPrintReport.setForeground(new java.awt.Color(12, 42, 77));
        btnPrintReport.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnPrintReport.setText("Print report");
        btnPrintReport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintReportActionPerformed(evt);
            }
        });
        workloadHeader.add(btnPrintReport, java.awt.BorderLayout.EAST);

        workloadCard.add(workloadHeader, java.awt.BorderLayout.NORTH);

        scrollWorkload.setViewportView(tblWorkload);

        workloadCard.add(scrollWorkload, java.awt.BorderLayout.CENTER);

        pageOverview.add(workloadCard, java.awt.BorderLayout.SOUTH);

        setPreferredSize(new java.awt.Dimension(1100, 700));
        setLayout(new java.awt.BorderLayout());

        sidebar.setPreferredSize(new java.awt.Dimension(250, 700));

        lblLogo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/usiu_logo.png"))); // NOI18N

        lblPortal.setForeground(new java.awt.Color(168, 139, 255));
        lblPortal.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblPortal.setText("ADMINISTRATOR");

        navOverview.setText("Overview");
        navOverview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navOverviewActionPerformed(evt);
            }
        });

        navUsers.setText("Users");
        navUsers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navUsersActionPerformed(evt);
            }
        });

        navAppointments.setText("Appointments");
        navAppointments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navAppointmentsActionPerformed(evt);
            }
        });

        navFeedback.setText("Feedback");
        navFeedback.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navFeedbackActionPerformed(evt);
            }
        });

        avatar.setBadgeColor(new java.awt.Color(168, 139, 255));

        lblUserName.setForeground(new java.awt.Color(255, 255, 255));
        lblUserName.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblUserName.setText("Name");

        lblRole.setForeground(new java.awt.Color(180, 195, 216));
        lblRole.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblRole.setText("Admin");

        btnLogout.setBackground(new java.awt.Color(30, 56, 92));
        btnLogout.setForeground(new java.awt.Color(255, 255, 255));
        btnLogout.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnLogout.setText("Log out");
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout sidebarLayout = new javax.swing.GroupLayout(sidebar);
        sidebar.setLayout(sidebarLayout);
        sidebarLayout.setHorizontalGroup(
            sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidebarLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblLogo)
                    .addComponent(lblPortal)
                    .addComponent(navOverview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(navUsers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(navAppointments, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                    .addComponent(navFeedback, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(sidebarLayout.createSequentialGroup()
                        .addComponent(avatar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addGroup(sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblUserName)
                            .addComponent(lblRole)))
                    .addComponent(btnLogout, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18))
        );
        sidebarLayout.setVerticalGroup(
            sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidebarLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(lblLogo)
                .addGap(14, 14, 14)
                .addComponent(lblPortal)
                .addGap(18, 18, 18)
                .addComponent(navOverview, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(navUsers, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(navAppointments, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(navFeedback, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(avatar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(sidebarLayout.createSequentialGroup()
                        .addComponent(lblUserName)
                        .addGap(0, 0, 0)
                        .addComponent(lblRole)))
                .addGap(14, 14, 14)
                .addComponent(btnLogout, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
        );

        add(sidebar, java.awt.BorderLayout.WEST);

        mainPanel.setBackground(new java.awt.Color(244, 247, 251));
        mainPanel.setLayout(new java.awt.BorderLayout());

        topBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 28, 8, 16));
        topBar.setPreferredSize(new java.awt.Dimension(100, 76));
        topBar.setLayout(new java.awt.BorderLayout());

        titles.setOpaque(false);
        titles.setLayout(new java.awt.GridLayout(2, 1));

        lblPageTitle.setForeground(new java.awt.Color(12, 42, 77));
        lblPageTitle.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        lblPageTitle.setText("Title");
        titles.add(lblPageTitle);

        lblPageSubtitle.setForeground(new java.awt.Color(91, 107, 130));
        lblPageSubtitle.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblPageSubtitle.setText("Subtitle");
        titles.add(lblPageSubtitle);

        topBar.add(titles, java.awt.BorderLayout.CENTER);

        topRight.setOpaque(false);
        topRight.setLayout(new java.awt.BorderLayout(14, 0));

        lblDate.setForeground(new java.awt.Color(91, 107, 130));
        lblDate.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblDate.setText("Today");
        topRight.add(lblDate, java.awt.BorderLayout.CENTER);
        topRight.add(windowButtons, java.awt.BorderLayout.EAST);

        topBar.add(topRight, java.awt.BorderLayout.EAST);

        mainPanel.add(topBar, java.awt.BorderLayout.NORTH);

        pages.setOpaque(false);
        pages.setLayout(new java.awt.CardLayout());

        pages.add(pageOverview, "OVERVIEW");

        pageUsers.setOpaque(false);
        pageUsers.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageUsers.setLayout(new java.awt.BorderLayout(0, 12));

        toolbarPanelUsers.setOpaque(false);
        toolbarPanelUsers.setLayout(new java.awt.BorderLayout(12, 0));

        filterPanelUsers.setOpaque(false);
        filterPanelUsers.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        txtSearch.setColumns(10);
        filterPanelUsers.add(txtSearch);

        cmbRole.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "All roles", "ADMIN", "COUNSELLOR", "STUDENT" }));

        cmbRole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbRoleActionPerformed(evt);
            }
        });
        filterPanelUsers.add(cmbRole);

        toolbarPanelUsers.add(filterPanelUsers, java.awt.BorderLayout.WEST);

        buttonBarUsers.setOpaque(false);
        buttonBarUsers.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        btnUserAdd.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnUserAdd.setText("Add user");
        btnUserAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUserAddActionPerformed(evt);
            }
        });
        buttonBarUsers.add(btnUserAdd);

        btnUserEdit.setBackground(new java.awt.Color(26, 75, 132));
        btnUserEdit.setForeground(new java.awt.Color(255, 255, 255));
        btnUserEdit.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnUserEdit.setText("Edit");
        btnUserEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUserEditActionPerformed(evt);
            }
        });
        buttonBarUsers.add(btnUserEdit);

        btnUserReset.setBackground(new java.awt.Color(232, 238, 246));
        btnUserReset.setForeground(new java.awt.Color(12, 42, 77));
        btnUserReset.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnUserReset.setText("Reset password");
        btnUserReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUserResetActionPerformed(evt);
            }
        });
        buttonBarUsers.add(btnUserReset);

        btnUserDelete.setBackground(new java.awt.Color(217, 69, 75));
        btnUserDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnUserDelete.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnUserDelete.setText("Delete");
        btnUserDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUserDeleteActionPerformed(evt);
            }
        });
        buttonBarUsers.add(btnUserDelete);

        btnUserPrint.setBackground(new java.awt.Color(232, 238, 246));
        btnUserPrint.setForeground(new java.awt.Color(12, 42, 77));
        btnUserPrint.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnUserPrint.setText("Print");
        btnUserPrint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUserPrintActionPerformed(evt);
            }
        });
        buttonBarUsers.add(btnUserPrint);

        toolbarPanelUsers.add(buttonBarUsers, java.awt.BorderLayout.EAST);

        pageUsers.add(toolbarPanelUsers, java.awt.BorderLayout.NORTH);

        scrollUsers.setViewportView(tblUsers);

        pageUsers.add(scrollUsers, java.awt.BorderLayout.CENTER);

        pages.add(pageUsers, "USERS");

        pageAppointments.setOpaque(false);
        pageAppointments.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageAppointments.setLayout(new java.awt.BorderLayout(0, 12));

        toolbarPanelAppts.setOpaque(false);
        toolbarPanelAppts.setLayout(new java.awt.BorderLayout(12, 0));

        filterPanelAppts.setOpaque(false);
        filterPanelAppts.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        lblStatus.setForeground(new java.awt.Color(12, 42, 77));
        lblStatus.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStatus.setText("Status");
        filterPanelAppts.add(lblStatus);

        cmbStatus.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "All", "PENDING", "APPROVED", "COMPLETED", "CANCELLED" }));

        cmbStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbStatusActionPerformed(evt);
            }
        });
        filterPanelAppts.add(cmbStatus);

        toolbarPanelAppts.add(filterPanelAppts, java.awt.BorderLayout.WEST);

        buttonBarAppts.setOpaque(false);
        buttonBarAppts.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        btnApptEdit.setBackground(new java.awt.Color(26, 75, 132));
        btnApptEdit.setForeground(new java.awt.Color(255, 255, 255));
        btnApptEdit.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnApptEdit.setText("Edit");
        btnApptEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnApptEditActionPerformed(evt);
            }
        });
        buttonBarAppts.add(btnApptEdit);

        btnApptDelete.setBackground(new java.awt.Color(217, 69, 75));
        btnApptDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnApptDelete.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnApptDelete.setText("Delete");
        btnApptDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnApptDeleteActionPerformed(evt);
            }
        });
        buttonBarAppts.add(btnApptDelete);

        btnApptPrint.setBackground(new java.awt.Color(232, 238, 246));
        btnApptPrint.setForeground(new java.awt.Color(12, 42, 77));
        btnApptPrint.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnApptPrint.setText("Print");
        btnApptPrint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnApptPrintActionPerformed(evt);
            }
        });
        buttonBarAppts.add(btnApptPrint);

        toolbarPanelAppts.add(buttonBarAppts, java.awt.BorderLayout.EAST);

        pageAppointments.add(toolbarPanelAppts, java.awt.BorderLayout.NORTH);

        scrollAppts.setViewportView(tblAppointments);

        pageAppointments.add(scrollAppts, java.awt.BorderLayout.CENTER);

        lblTip.setForeground(new java.awt.Color(91, 107, 130));
        lblTip.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblTip.setText("Deleting an appointment also deletes its session notes and feedback.");
        pageAppointments.add(lblTip, java.awt.BorderLayout.SOUTH);

        pages.add(pageAppointments, "APPOINTMENTS");

        pageFeedback.setOpaque(false);
        pageFeedback.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageFeedback.setLayout(new java.awt.BorderLayout(0, 12));

        toolbarPanelFeedback.setOpaque(false);
        toolbarPanelFeedback.setLayout(new java.awt.BorderLayout());

        buttonBarFeedback.setOpaque(false);
        buttonBarFeedback.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        btnFeedbackEdit.setBackground(new java.awt.Color(26, 75, 132));
        btnFeedbackEdit.setForeground(new java.awt.Color(255, 255, 255));
        btnFeedbackEdit.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnFeedbackEdit.setText("Edit");
        btnFeedbackEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFeedbackEditActionPerformed(evt);
            }
        });
        buttonBarFeedback.add(btnFeedbackEdit);

        btnFeedbackDelete.setBackground(new java.awt.Color(217, 69, 75));
        btnFeedbackDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnFeedbackDelete.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnFeedbackDelete.setText("Delete");
        btnFeedbackDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFeedbackDeleteActionPerformed(evt);
            }
        });
        buttonBarFeedback.add(btnFeedbackDelete);

        btnFeedbackPrint.setBackground(new java.awt.Color(232, 238, 246));
        btnFeedbackPrint.setForeground(new java.awt.Color(12, 42, 77));
        btnFeedbackPrint.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnFeedbackPrint.setText("Print");
        btnFeedbackPrint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFeedbackPrintActionPerformed(evt);
            }
        });
        buttonBarFeedback.add(btnFeedbackPrint);

        toolbarPanelFeedback.add(buttonBarFeedback, java.awt.BorderLayout.EAST);

        pageFeedback.add(toolbarPanelFeedback, java.awt.BorderLayout.NORTH);

        scrollFeedback.setViewportView(tblFeedback);

        pageFeedback.add(scrollFeedback, java.awt.BorderLayout.CENTER);

        pages.add(pageFeedback, "FEEDBACK");

        mainPanel.add(pages, java.awt.BorderLayout.CENTER);

        add(mainPanel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void navOverviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navOverviewActionPerformed
        showPage("OVERVIEW");
    }//GEN-LAST:event_navOverviewActionPerformed

    private void navUsersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navUsersActionPerformed
        showPage("USERS");
    }//GEN-LAST:event_navUsersActionPerformed

    private void navAppointmentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navAppointmentsActionPerformed
        showPage("APPOINTMENTS");
    }//GEN-LAST:event_navAppointmentsActionPerformed

    private void navFeedbackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navFeedbackActionPerformed
        showPage("FEEDBACK");
    }//GEN-LAST:event_navFeedbackActionPerformed

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        logout();
    }//GEN-LAST:event_btnLogoutActionPerformed

    private void btnPrintReportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintReportActionPerformed
        Ui.printTable(this, tblWorkload, "Counsellor workload report");
    }//GEN-LAST:event_btnPrintReportActionPerformed

    private void cmbRoleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbRoleActionPerformed
        showUsers();
    }//GEN-LAST:event_cmbRoleActionPerformed

    private void btnUserAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUserAddActionPerformed
        if (UserForm.create(this)) { refreshUsers(); }
    }//GEN-LAST:event_btnUserAddActionPerformed

    private void btnUserEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUserEditActionPerformed
        editUser();
    }//GEN-LAST:event_btnUserEditActionPerformed

    private void btnUserResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUserResetActionPerformed
        resetPassword();
    }//GEN-LAST:event_btnUserResetActionPerformed

    private void btnUserDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUserDeleteActionPerformed
        deleteUser();
    }//GEN-LAST:event_btnUserDeleteActionPerformed

    private void btnUserPrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUserPrintActionPerformed
        Ui.printTable(this, tblUsers, "Users");
    }//GEN-LAST:event_btnUserPrintActionPerformed

    private void cmbStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbStatusActionPerformed
        showAppointments();
    }//GEN-LAST:event_cmbStatusActionPerformed

    private void btnApptEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnApptEditActionPerformed
        editAppointment();
    }//GEN-LAST:event_btnApptEditActionPerformed

    private void btnApptDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnApptDeleteActionPerformed
        deleteAppointment();
    }//GEN-LAST:event_btnApptDeleteActionPerformed

    private void btnApptPrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnApptPrintActionPerformed
        Ui.printTable(this, tblAppointments, "Appointments");
    }//GEN-LAST:event_btnApptPrintActionPerformed

    private void btnFeedbackEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFeedbackEditActionPerformed
        editFeedback();
    }//GEN-LAST:event_btnFeedbackEditActionPerformed

    private void btnFeedbackDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFeedbackDeleteActionPerformed
        deleteFeedback();
    }//GEN-LAST:event_btnFeedbackDeleteActionPerformed

    private void btnFeedbackPrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFeedbackPrintActionPerformed
        Ui.printTable(this, tblFeedback, "Feedback");
    }//GEN-LAST:event_btnFeedbackPrintActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private utils.Avatar avatar;
    private utils.Ui.PillButton btnApptDelete;
    private utils.Ui.PillButton btnApptEdit;
    private utils.Ui.PillButton btnApptPrint;
    private utils.Ui.PillButton btnFeedbackDelete;
    private utils.Ui.PillButton btnFeedbackEdit;
    private utils.Ui.PillButton btnFeedbackPrint;
    private utils.Ui.PillButton btnLogout;
    private utils.Ui.PillButton btnPrintReport;
    private utils.Ui.PillButton btnUserAdd;
    private utils.Ui.PillButton btnUserDelete;
    private utils.Ui.PillButton btnUserEdit;
    private utils.Ui.PillButton btnUserPrint;
    private utils.Ui.PillButton btnUserReset;
    private javax.swing.JPanel buttonBarAppts;
    private javax.swing.JPanel buttonBarFeedback;
    private javax.swing.JPanel buttonBarUsers;
    private utils.StatCard cardAppointments;
    private utils.StatCard cardCounsellors;
    private utils.StatCard cardRating;
    private utils.StatCard cardStudents;
    private javax.swing.JPanel chartsPanel;
    private utils.StatCard cardCompletion;
    private utils.StatCard cardPending;
    private utils.BarChart monthChart;
    private utils.Ui.RoundPanel monthCard;
    private javax.swing.JLabel lblMonthTitle;
    private javax.swing.JComboBox<String> cmbRole;
    private javax.swing.JComboBox<String> cmbStatus;
    private javax.swing.JPanel filterPanelAppts;
    private javax.swing.JPanel filterPanelUsers;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblLogo;
    private javax.swing.JLabel lblPageSubtitle;
    private javax.swing.JLabel lblPageTitle;
    private javax.swing.JLabel lblPortal;
    private javax.swing.JLabel lblRatingTitle;
    private javax.swing.JLabel lblRole;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblStatusTitle;
    private javax.swing.JLabel lblTip;
    private javax.swing.JLabel lblUserName;
    private javax.swing.JLabel lblWorkloadTitle;
    private javax.swing.JPanel mainPanel;
    private utils.NavButton navAppointments;
    private utils.NavButton navFeedback;
    private utils.NavButton navOverview;
    private utils.NavButton navUsers;
    private javax.swing.JPanel pageAppointments;
    private javax.swing.JPanel pageFeedback;
    private javax.swing.JPanel pageOverview;
    private javax.swing.JPanel pageUsers;
    private javax.swing.JPanel pages;
    private utils.Ui.RoundPanel ratingCard;
    private utils.BarChart ratingChart;
    private javax.swing.JScrollPane scrollAppts;
    private javax.swing.JScrollPane scrollFeedback;
    private javax.swing.JScrollPane scrollUsers;
    private javax.swing.JScrollPane scrollWorkload;
    private utils.GradientPanel sidebar;
    private javax.swing.JPanel statsPanel;
    private utils.Ui.RoundPanel statusCard;
    private utils.BarChart statusChart;
    private javax.swing.JTable tblAppointments;
    private javax.swing.JTable tblFeedback;
    private javax.swing.JTable tblUsers;
    private javax.swing.JTable tblWorkload;
    private javax.swing.JPanel titles;
    private javax.swing.JPanel toolbarPanelAppts;
    private javax.swing.JPanel toolbarPanelFeedback;
    private javax.swing.JPanel toolbarPanelUsers;
    private utils.TopBar topBar;
    private javax.swing.JPanel topRight;
    private javax.swing.JTextField txtSearch;
    private utils.WindowButtons windowButtons;
    private utils.Ui.RoundPanel workloadCard;
    private javax.swing.JPanel workloadHeader;
    // End of variables declaration//GEN-END:variables
}
