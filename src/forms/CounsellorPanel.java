package forms;

import dao.AppointmentDAO;
import dao.CounsellorDAO;
import dao.FeedbackDAO;
import dao.SessionDAO;
import dao.UserDAO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.text.JTextComponent;
import models.Appointment;
import models.Counsellor;
import models.CounsellingSession;
import models.Feedback;
import models.User;
import studentcounsellingsystem.MainFrame;
import utils.PasswordUtil;
import utils.Session;
import utils.StarRating;
import utils.Theme;
import utils.Ui;
import utils.Validate;

// counsellor portal: sidebar menu + 4 pages, all designed in the Design tab (dashboard, appointments + session notes, feedback, profile)
public class CounsellorPanel extends javax.swing.JPanel {

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final SessionDAO sessionDAO = new SessionDAO();
    private final CounsellorDAO counsellorDAO = new CounsellorDAO();
    private final UserDAO userDAO = new UserDAO();
    private Counsellor counsellor;
    private List<Appointment> all = new ArrayList<>();
    private List<Appointment> shown = new ArrayList<>();
    private List<Appointment> upcoming = new ArrayList<>();
    private final User user = Session.getUser();
    private final java.util.Map<String, utils.NavButton> navButtons = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, String[]> pageText = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Runnable> refreshers = new java.util.LinkedHashMap<>();

    public CounsellorPanel() {
        initComponents();
        Ui.run(this, () -> counsellor = counsellorDAO.findByUserId(user.getUserId()));
        if (counsellor == null) { Ui.error(this, "No counsellor profile was found for this account."); return; }
        applyTheme();
        refreshUserCard();
        lblRole.setText("Counsellor");
        lblDate.setText(LocalDate.now().format(Ui.DATE));

        // the menu: button, page title, subtitle and what to load when the page opens
        String first = user.getFullName().contains(" ") ? user.getFullName().substring(0, user.getFullName().indexOf(' ')) : user.getFullName();
        addPage("HOME", navHome, "Dashboard", "Welcome back, " + first, this::refreshHome);
        addPage("APPOINTMENTS", navAppointments, "Appointments", "Approve requests and record your sessions", this::refreshAppointments);
        addPage("FEEDBACK", navFeedback, "Feedback", "What students say about your sessions", this::refreshFeedback);
        addPage("PROFILE", navProfile, "My Profile", "Your details and password", this::refreshProfile);
        showPage("HOME");
    }

    //  look of the tables and fields (the layout itself is in the Design tab) 

    private void applyTheme() {
        Ui.styleTable(tblUpcoming, "Date", "Time", "Student", "Status");
        Ui.styleTable(tblAppointments, "Date", "Time", "Student", "Reg. no.", "Reason", "Status", "Rating");
        Ui.styleTable(tblFeedback, "Session date", "Student", "Rating", "Comments");
        Ui.styleScroll(scrollUpcoming);
        Ui.styleScroll(scrollAppointments);
        Ui.styleScroll(scrollFeedback);
        for (JTextComponent t : new JTextComponent[]{txtName, txtEmail, txtPhone, txtSpeciality, txtQualification, txtReligion, txtOld, txtNew, txtConfirm}) { Ui.style(t); }
        Ui.rowMenu(tblAppointments, new String[]{"Approve", "Decline / cancel", "Session notes"},
                new Runnable[]{this::approveSelected, this::declineSelected, this::notesForSelected});
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

    private void refreshHome() {
        Ui.run(this, () -> {
            all = appointmentDAO.findByCounsellor(counsellor.getCounsellorId());
            LocalDate today = LocalDate.now();
            int todayCount = 0, pending = 0, done = 0, cancelled = 0, approved = 0;
            upcoming = new ArrayList<>();
            for (Appointment a : all) {
                if (a.isOpen() && !a.getAppointmentDate().isBefore(today)) { upcoming.add(a); }
                if (a.isOpen() && a.getAppointmentDate().equals(today)) { todayCount++; }
                switch (a.getStatus()) {
                    case Appointment.PENDING: pending++; break;
                    case Appointment.APPROVED: approved++; break;
                    case Appointment.COMPLETED: done++; break;
                    default: cancelled++;
                }
            }
            Collections.reverse(upcoming);
            cardToday.setValue(String.valueOf(todayCount));
            cardPending.setValue(String.valueOf(pending));
            cardDone.setValue(String.valueOf(done));
            double sum = 0; int n = 0;
            for (Appointment a : all) { if (a.getRating() != null) { sum += a.getRating(); n++; } }
            cardRating.setValue(n == 0 ? "-" : String.format("%.1f", sum / n));
            List<Object[]> rows = new ArrayList<>();
            for (Appointment a : upcoming) { rows.add(new Object[]{a.getAppointmentDate().format(Ui.DATE), a.getAppointmentTime().format(Ui.TIME), a.getStudentName(), a.getStatus()}); }
            Ui.setRows(tblUpcoming, rows);
            Ui.widths(tblUpcoming, 140, 60, 130, 90);
            chart.setData(Arrays.asList("Pending", "Approved", "Done", "Cancelled"), Arrays.asList(pending, approved, done, cancelled),
                    Arrays.asList(Theme.GOLD, Theme.TEAL, Theme.SUCCESS, Theme.DANGER));
        });
    }

    private void refreshAppointments() {
        Ui.run(this, () -> {
            all = appointmentDAO.findByCounsellor(counsellor.getCounsellorId());
            showFiltered();
        });
    }

    private void showFiltered() {
        String f = (String) cmbFilter.getSelectedItem();
        shown = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();
        for (Appointment a : all) {
            if (f != null && !f.equals("All") && !f.equals(a.getStatus())) { continue; }
            shown.add(a);
            rows.add(new Object[]{a.getAppointmentDate().format(Ui.DATE), a.getAppointmentTime().format(Ui.TIME), a.getStudentName(), a.getRegistrationNumber(), a.getReason(), a.getStatus(), StarRating.stars(a.getRating())});
        }
        Ui.setRows(tblAppointments, rows);
        Ui.widths(tblAppointments, 140, 60, 130, 80, 190, 100, 90);
    }

    private Appointment selectedAppointment() {
        int i = Ui.selected(tblAppointments);
        if (i < 0) { Ui.info(this, "Select an appointment in the table first."); return null; }
        return shown.get(i);
    }

    private void approveSelected() {
        Appointment a = selectedAppointment();
        if (a == null) { return; }
        if (!Appointment.PENDING.equals(a.getStatus())) { Ui.info(this, "Only pending requests can be approved."); return; }
        Ui.run(this, () -> { appointmentDAO.changeStatus(a, Appointment.APPROVED); refreshAppointments(); });
    }

    private void declineSelected() {
        Appointment a = selectedAppointment();
        if (a == null) { return; }
        if (!a.isOpen()) { Ui.info(this, "This appointment is already " + a.getStatus().toLowerCase() + "."); return; }
        if (Ui.confirm(this, "Cancel the appointment with " + a.getStudentName() + " on " + a.getAppointmentDate().format(Ui.DATE) + "?")) {
            Ui.run(this, () -> { appointmentDAO.changeStatus(a, Appointment.CANCELLED); refreshAppointments(); });
        }
    }

    // approved -> write the notes; completed -> open, edit or delete them
    private void notesForSelected() {
        Appointment a = selectedAppointment();
        if (a == null) { return; }
        if (!Appointment.APPROVED.equals(a.getStatus()) && !Appointment.COMPLETED.equals(a.getStatus())) {
            Ui.info(this, "Approve the appointment first, then record the session after you meet.");
            return;
        }
        Ui.run(this, () -> {
            CounsellingSession existing = sessionDAO.findByAppointment(a.getAppointmentId());
            if (SessionForm.show(this, a, existing)) { refreshAppointments(); }
        });
    }

    private void refreshFeedback() {
        Ui.run(this, () -> {
            List<Feedback> list = new FeedbackDAO().findByCounsellor(counsellor.getCounsellorId());
            List<Object[]> rows = new ArrayList<>();
            double sum = 0;
            for (Feedback f : list) {
                sum += f.getRating();
                rows.add(new Object[]{f.getAppointmentDate().format(Ui.DATE), f.getStudentName(), StarRating.stars(f.getRating()), f.getComments()});
            }
            lblAverage.setText(list.isEmpty() ? "-" : String.format("%.1f", sum / list.size()));
            Ui.setRows(tblFeedback, rows);
            Ui.widths(tblFeedback, 140, 150, 110, 330);
        });
    }

    private void refreshProfile() {
        txtName.setText(counsellor.getFullName());
        txtEmail.setText(counsellor.getEmail());
        txtPhone.setText(counsellor.getPhone() == null ? "" : counsellor.getPhone());
        txtSpeciality.setText(counsellor.getSpecialization());
        txtQualification.setText(counsellor.getQualification());
        cmbGender.setSelectedItem(counsellor.getGender());
        txtReligion.setText(counsellor.getReligion());
    }

    private void saveProfile() {
        Ui.run(this, () -> {
            if (Validate.blank(txtName.getText())) { throw new IllegalArgumentException("Enter your full name."); }
            if (!Validate.phone(txtPhone.getText())) { throw new IllegalArgumentException("Enter a valid phone number (or leave it empty)."); }
            counsellor.setFullName(txtName.getText().trim());
            counsellor.setPhone(txtPhone.getText().trim());
            counsellor.setSpecialization(txtSpeciality.getText().trim());
            counsellor.setQualification(txtQualification.getText().trim());
            counsellor.setGender((String) cmbGender.getSelectedItem());
            counsellor.setReligion(txtReligion.getText().trim());
            counsellorDAO.update(counsellor);
            user.setFullName(counsellor.getFullName());
            refreshUserCard();
            Ui.info(this, "Your profile was updated.");
        });
    }

    private void changePassword() {
        Ui.run(this, () -> {
            String old = new String(txtOld.getPassword()), pw = new String(txtNew.getPassword());
            if (!PasswordUtil.verify(old, user.getPasswordHash())) { throw new IllegalArgumentException("Your current password is not correct."); }
            if (Validate.password(pw) != null) { throw new IllegalArgumentException(Validate.password(pw)); }
            if (!pw.equals(new String(txtConfirm.getPassword()))) { throw new IllegalArgumentException("The new passwords do not match."); }
            userDAO.updatePassword(user.getUserId(), pw);
            user.setPasswordHash(userDAO.findById(user.getUserId()).getPasswordHash());
            txtOld.setText(""); txtNew.setText(""); txtConfirm.setText("");
            Ui.info(this, "Your password was changed.");
        });
    }

    private void deleteAccount() {
        if (!Ui.confirm(this, "Delete your account?\nThis cannot be undone.")) { return; }
        Ui.run(this, () -> {
            userDAO.delete(user.getUserId());
            Ui.info(this, "Your account was deleted.");
            if (frame() != null) { frame().logout(); }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sidebar = new utils.GradientPanel();
        lblLogo = new javax.swing.JLabel();
        lblPortal = new javax.swing.JLabel();
        navHome = new utils.NavButton();
        navAppointments = new utils.NavButton();
        navFeedback = new utils.NavButton();
        navProfile = new utils.NavButton();
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
        pageHome = new javax.swing.JPanel();
        statsPanel = new javax.swing.JPanel();
        cardToday = new utils.StatCard();
        cardPending = new utils.StatCard();
        cardDone = new utils.StatCard();
        cardRating = new utils.StatCard();
        rowPanel = new javax.swing.JPanel();
        tableCard = new utils.Ui.RoundPanel();
        lblUpcomingTitle = new javax.swing.JLabel();
        scrollUpcoming = new javax.swing.JScrollPane();
        tblUpcoming = new javax.swing.JTable();
        chartCard = new utils.Ui.RoundPanel();
        lblChartTitle = new javax.swing.JLabel();
        chart = new utils.BarChart();
        pageAppointments = new javax.swing.JPanel();
        toolbarPanel = new javax.swing.JPanel();
        filterPanel = new javax.swing.JPanel();
        lblShow = new javax.swing.JLabel();
        cmbFilter = new javax.swing.JComboBox<String>();
        buttonBar = new javax.swing.JPanel();
        btnApprove = new utils.Ui.PillButton();
        btnDecline = new utils.Ui.PillButton();
        btnNotes = new utils.Ui.PillButton();
        scrollAppointments = new javax.swing.JScrollPane();
        tblAppointments = new javax.swing.JTable();
        lblTip = new javax.swing.JLabel();
        pageFeedback = new javax.swing.JPanel();
        averageCard = new utils.Ui.RoundPanel();
        lblAverage = new javax.swing.JLabel();
        lblAverageText = new javax.swing.JLabel();
        scrollFeedback = new javax.swing.JScrollPane();
        tblFeedback = new javax.swing.JTable();
        pageProfile = new javax.swing.JPanel();
        detailsCard = new utils.Ui.RoundPanel();
        lblDetails = new javax.swing.JLabel();
        detailsForm = new javax.swing.JPanel();
        lblFullName = new javax.swing.JLabel();
        lblEmail = new javax.swing.JLabel();
        lblPhone = new javax.swing.JLabel();
        lblSpeciality = new javax.swing.JLabel();
        lblQualification = new javax.swing.JLabel();
        lblGender = new javax.swing.JLabel();
        lblReligion = new javax.swing.JLabel();
        txtName = new javax.swing.JTextField();
        txtEmail = new javax.swing.JTextField();
        txtPhone = new javax.swing.JTextField();
        txtSpeciality = new javax.swing.JTextField();
        txtQualification = new javax.swing.JTextField();
        cmbGender = new javax.swing.JComboBox<String>();
        txtReligion = new javax.swing.JTextField();
        saveBar = new javax.swing.JPanel();
        btnSave = new utils.Ui.PillButton();
        securityCard = new utils.Ui.RoundPanel();
        lblPassword = new javax.swing.JLabel();
        passwordForm = new javax.swing.JPanel();
        lblOld = new javax.swing.JLabel();
        lblNew = new javax.swing.JLabel();
        lblConfirm = new javax.swing.JLabel();
        txtOld = new javax.swing.JPasswordField();
        txtNew = new javax.swing.JPasswordField();
        txtConfirm = new javax.swing.JPasswordField();
        securityBar = new javax.swing.JPanel();
        btnChange = new utils.Ui.PillButton();
        btnDeleteAccount = new utils.Ui.PillButton();

        setPreferredSize(new java.awt.Dimension(1100, 700));
        setLayout(new java.awt.BorderLayout());

        sidebar.setPreferredSize(new java.awt.Dimension(250, 700));

        lblLogo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/usiu_logo.png"))); // NOI18N

        lblPortal.setForeground(new java.awt.Color(245, 179, 53));
        lblPortal.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblPortal.setText("COUNSELLOR PORTAL");

        navHome.setText("Dashboard");
        navHome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navHomeActionPerformed(evt);
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

        navProfile.setText("My Profile");
        navProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navProfileActionPerformed(evt);
            }
        });

        avatar.setBadgeColor(new java.awt.Color(245, 179, 53));

        lblUserName.setForeground(new java.awt.Color(255, 255, 255));
        lblUserName.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblUserName.setText("Name");

        lblRole.setForeground(new java.awt.Color(180, 195, 216));
        lblRole.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblRole.setText("Counsellor");

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
                    .addComponent(navHome, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(navAppointments, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                    .addComponent(navFeedback, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(navProfile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                .addComponent(navHome, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(navAppointments, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(navFeedback, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(navProfile, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
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

        pageHome.setOpaque(false);
        pageHome.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageHome.setLayout(new java.awt.BorderLayout(0, 16));

        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new java.awt.Dimension(100, 100));
        statsPanel.setLayout(new java.awt.GridLayout(1, 4, 16, 0));

        cardToday.setCaption("Sessions today");
        statsPanel.add(cardToday);

        cardPending.setCaption("Pending requests");
        cardPending.setAccent(new java.awt.Color(245, 179, 53));
        statsPanel.add(cardPending);

        cardDone.setCaption("Completed");
        cardDone.setAccent(new java.awt.Color(27, 158, 104));
        statsPanel.add(cardDone);

        cardRating.setCaption("Average rating");
        cardRating.setAccent(new java.awt.Color(168, 139, 255));
        statsPanel.add(cardRating);

        pageHome.add(statsPanel, java.awt.BorderLayout.NORTH);

        rowPanel.setOpaque(false);
        rowPanel.setLayout(new java.awt.BorderLayout(16, 0));

        tableCard.setLayout(new java.awt.BorderLayout(0, 10));

        lblUpcomingTitle.setForeground(new java.awt.Color(12, 42, 77));
        lblUpcomingTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblUpcomingTitle.setText("Today and upcoming");
        tableCard.add(lblUpcomingTitle, java.awt.BorderLayout.NORTH);

        scrollUpcoming.setViewportView(tblUpcoming);

        tableCard.add(scrollUpcoming, java.awt.BorderLayout.CENTER);

        rowPanel.add(tableCard, java.awt.BorderLayout.CENTER);

        chartCard.setPreferredSize(new java.awt.Dimension(310, 100));
        chartCard.setLayout(new java.awt.BorderLayout(0, 10));

        lblChartTitle.setForeground(new java.awt.Color(12, 42, 77));
        lblChartTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblChartTitle.setText("My appointments by status");
        chartCard.add(lblChartTitle, java.awt.BorderLayout.NORTH);
        chartCard.add(chart, java.awt.BorderLayout.CENTER);

        rowPanel.add(chartCard, java.awt.BorderLayout.EAST);

        pageHome.add(rowPanel, java.awt.BorderLayout.CENTER);

        pages.add(pageHome, "HOME");

        pageAppointments.setOpaque(false);
        pageAppointments.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageAppointments.setLayout(new java.awt.BorderLayout(0, 12));

        toolbarPanel.setOpaque(false);
        toolbarPanel.setLayout(new java.awt.BorderLayout(12, 0));

        filterPanel.setOpaque(false);
        filterPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        lblShow.setForeground(new java.awt.Color(12, 42, 77));
        lblShow.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblShow.setText("Show");
        filterPanel.add(lblShow);

        cmbFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbFilterActionPerformed(evt);
            }
        });
        filterPanel.add(cmbFilter);

        toolbarPanel.add(filterPanel, java.awt.BorderLayout.WEST);

        buttonBar.setOpaque(false);
        buttonBar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        btnApprove.setBackground(new java.awt.Color(26, 75, 132));
        btnApprove.setForeground(new java.awt.Color(255, 255, 255));
        btnApprove.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnApprove.setText("Approve");
        btnApprove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnApproveActionPerformed(evt);
            }
        });
        buttonBar.add(btnApprove);

        btnDecline.setBackground(new java.awt.Color(217, 69, 75));
        btnDecline.setForeground(new java.awt.Color(255, 255, 255));
        btnDecline.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnDecline.setText("Decline / cancel");
        btnDecline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeclineActionPerformed(evt);
            }
        });
        buttonBar.add(btnDecline);

        btnNotes.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnNotes.setText("Session notes");
        btnNotes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNotesActionPerformed(evt);
            }
        });
        buttonBar.add(btnNotes);

        toolbarPanel.add(buttonBar, java.awt.BorderLayout.EAST);

        pageAppointments.add(toolbarPanel, java.awt.BorderLayout.NORTH);

        scrollAppointments.setViewportView(tblAppointments);

        pageAppointments.add(scrollAppointments, java.awt.BorderLayout.CENTER);

        lblTip.setForeground(new java.awt.Color(91, 107, 130));
        lblTip.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblTip.setText("Approve a pending request, then record the session after you meet. Right-click a row for the same actions.");
        pageAppointments.add(lblTip, java.awt.BorderLayout.SOUTH);

        pages.add(pageAppointments, "APPOINTMENTS");

        pageFeedback.setOpaque(false);
        pageFeedback.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageFeedback.setLayout(new java.awt.BorderLayout(0, 14));

        averageCard.setFill(new java.awt.Color(241, 236, 255));
        averageCard.setPreferredSize(new java.awt.Dimension(100, 80));
        averageCard.setLayout(new java.awt.BorderLayout(16, 0));

        lblAverage.setForeground(new java.awt.Color(12, 42, 77));
        lblAverage.setFont(new java.awt.Font("Segoe UI", 1, 34)); // NOI18N
        lblAverage.setText("-");
        averageCard.add(lblAverage, java.awt.BorderLayout.WEST);

        lblAverageText.setForeground(new java.awt.Color(91, 107, 130));
        lblAverageText.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblAverageText.setText("average rating from your students (out of 5)");
        averageCard.add(lblAverageText, java.awt.BorderLayout.CENTER);

        pageFeedback.add(averageCard, java.awt.BorderLayout.NORTH);

        scrollFeedback.setViewportView(tblFeedback);

        pageFeedback.add(scrollFeedback, java.awt.BorderLayout.CENTER);

        pages.add(pageFeedback, "FEEDBACK");

        pageProfile.setOpaque(false);
        pageProfile.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageProfile.setLayout(new java.awt.GridLayout(1, 2, 18, 0));

        detailsCard.setLayout(new java.awt.BorderLayout(0, 12));

        lblDetails.setForeground(new java.awt.Color(12, 42, 77));
        lblDetails.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblDetails.setText("Your details");
        detailsCard.add(lblDetails, java.awt.BorderLayout.NORTH);

        detailsForm.setOpaque(false);

        lblFullName.setForeground(new java.awt.Color(12, 42, 77));
        lblFullName.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblFullName.setText("Full name");

        lblEmail.setForeground(new java.awt.Color(12, 42, 77));
        lblEmail.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblEmail.setText("Email");

        lblPhone.setForeground(new java.awt.Color(12, 42, 77));
        lblPhone.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblPhone.setText("Phone");

        lblSpeciality.setForeground(new java.awt.Color(12, 42, 77));
        lblSpeciality.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblSpeciality.setText("Specialization");

        lblQualification.setForeground(new java.awt.Color(12, 42, 77));
        lblQualification.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblQualification.setText("Qualification");

        lblGender.setForeground(new java.awt.Color(12, 42, 77));
        lblGender.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblGender.setText("Gender");

        lblReligion.setForeground(new java.awt.Color(12, 42, 77));
        lblReligion.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblReligion.setText("Religion");

        txtEmail.setEditable(false);

        javax.swing.GroupLayout detailsFormLayout = new javax.swing.GroupLayout(detailsForm);
        detailsForm.setLayout(detailsFormLayout);
        detailsFormLayout.setHorizontalGroup(
            detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsFormLayout.createSequentialGroup()
                .addGroup(detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblFullName)
                    .addComponent(lblEmail)
                    .addComponent(lblPhone)
                    .addComponent(lblSpeciality)
                    .addComponent(lblQualification)
                    .addComponent(lblGender)
                    .addComponent(lblReligion))
                .addGap(14, 14, 14)
                .addGroup(detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtName)
                    .addComponent(txtEmail)
                    .addComponent(txtPhone)
                    .addComponent(txtSpeciality)
                    .addComponent(txtQualification)
                    .addComponent(cmbGender, 0, 197, Short.MAX_VALUE)
                    .addComponent(txtReligion)))
        );
        detailsFormLayout.setVerticalGroup(
            detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsFormLayout.createSequentialGroup()
                .addGroup(detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblFullName)
                    .addComponent(txtName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblEmail)
                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblPhone)
                    .addComponent(txtPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblSpeciality)
                    .addComponent(txtSpeciality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblQualification)
                    .addComponent(txtQualification, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblGender)
                    .addComponent(cmbGender, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(detailsFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblReligion)
                    .addComponent(txtReligion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        detailsCard.add(detailsForm, java.awt.BorderLayout.CENTER);

        saveBar.setOpaque(false);
        saveBar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        btnSave.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnSave.setText("Save changes");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        saveBar.add(btnSave);

        detailsCard.add(saveBar, java.awt.BorderLayout.SOUTH);

        pageProfile.add(detailsCard);

        securityCard.setLayout(new java.awt.BorderLayout(0, 12));

        lblPassword.setForeground(new java.awt.Color(12, 42, 77));
        lblPassword.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblPassword.setText("Password");
        securityCard.add(lblPassword, java.awt.BorderLayout.NORTH);

        passwordForm.setOpaque(false);

        lblOld.setForeground(new java.awt.Color(12, 42, 77));
        lblOld.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblOld.setText("Current password");

        lblNew.setForeground(new java.awt.Color(12, 42, 77));
        lblNew.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblNew.setText("New password");

        lblConfirm.setForeground(new java.awt.Color(12, 42, 77));
        lblConfirm.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblConfirm.setText("Repeat new");

        javax.swing.GroupLayout passwordFormLayout = new javax.swing.GroupLayout(passwordForm);
        passwordForm.setLayout(passwordFormLayout);
        passwordFormLayout.setHorizontalGroup(
            passwordFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(passwordFormLayout.createSequentialGroup()
                .addGroup(passwordFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblOld)
                    .addComponent(lblNew)
                    .addComponent(lblConfirm))
                .addGap(14, 14, 14)
                .addGroup(passwordFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtOld, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                    .addComponent(txtNew)
                    .addComponent(txtConfirm)))
        );
        passwordFormLayout.setVerticalGroup(
            passwordFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(passwordFormLayout.createSequentialGroup()
                .addGroup(passwordFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblOld)
                    .addComponent(txtOld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(passwordFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblNew)
                    .addComponent(txtNew, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(passwordFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblConfirm)
                    .addComponent(txtConfirm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        securityCard.add(passwordForm, java.awt.BorderLayout.CENTER);

        securityBar.setOpaque(false);
        securityBar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        btnChange.setBackground(new java.awt.Color(26, 75, 132));
        btnChange.setForeground(new java.awt.Color(255, 255, 255));
        btnChange.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnChange.setText("Update password");
        btnChange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChangeActionPerformed(evt);
            }
        });
        securityBar.add(btnChange);

        btnDeleteAccount.setBackground(new java.awt.Color(217, 69, 75));
        btnDeleteAccount.setForeground(new java.awt.Color(255, 255, 255));
        btnDeleteAccount.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnDeleteAccount.setText("Delete my account");
        btnDeleteAccount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteAccountActionPerformed(evt);
            }
        });
        securityBar.add(btnDeleteAccount);

        securityCard.add(securityBar, java.awt.BorderLayout.SOUTH);

        pageProfile.add(securityCard);

        pages.add(pageProfile, "PROFILE");

        mainPanel.add(pages, java.awt.BorderLayout.CENTER);

        add(mainPanel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void navHomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navHomeActionPerformed
        showPage("HOME");
    }//GEN-LAST:event_navHomeActionPerformed

    private void navAppointmentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navAppointmentsActionPerformed
        showPage("APPOINTMENTS");
    }//GEN-LAST:event_navAppointmentsActionPerformed

    private void navFeedbackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navFeedbackActionPerformed
        showPage("FEEDBACK");
    }//GEN-LAST:event_navFeedbackActionPerformed

    private void navProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navProfileActionPerformed
        showPage("PROFILE");
    }//GEN-LAST:event_navProfileActionPerformed

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        logout();
    }//GEN-LAST:event_btnLogoutActionPerformed

    private void cmbFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbFilterActionPerformed
        showFiltered();
    }//GEN-LAST:event_cmbFilterActionPerformed

    private void btnApproveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnApproveActionPerformed
        approveSelected();
    }//GEN-LAST:event_btnApproveActionPerformed

    private void btnDeclineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeclineActionPerformed
        declineSelected();
    }//GEN-LAST:event_btnDeclineActionPerformed

    private void btnNotesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNotesActionPerformed
        notesForSelected();
    }//GEN-LAST:event_btnNotesActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        saveProfile();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnChangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChangeActionPerformed
        changePassword();
    }//GEN-LAST:event_btnChangeActionPerformed

    private void btnDeleteAccountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteAccountActionPerformed
        deleteAccount();
    }//GEN-LAST:event_btnDeleteAccountActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private utils.Avatar avatar;
    private utils.Ui.RoundPanel averageCard;
    private utils.Ui.PillButton btnApprove;
    private utils.Ui.PillButton btnChange;
    private utils.Ui.PillButton btnDecline;
    private utils.Ui.PillButton btnDeleteAccount;
    private utils.Ui.PillButton btnLogout;
    private utils.Ui.PillButton btnNotes;
    private utils.Ui.PillButton btnSave;
    private javax.swing.JPanel buttonBar;
    private utils.StatCard cardDone;
    private utils.StatCard cardPending;
    private utils.StatCard cardRating;
    private utils.StatCard cardToday;
    private utils.BarChart chart;
    private utils.Ui.RoundPanel chartCard;
    private javax.swing.JComboBox<String> cmbFilter;
    private javax.swing.JComboBox<String> cmbGender;
    private utils.Ui.RoundPanel detailsCard;
    private javax.swing.JPanel detailsForm;
    private javax.swing.JPanel filterPanel;
    private javax.swing.JLabel lblAverage;
    private javax.swing.JLabel lblAverageText;
    private javax.swing.JLabel lblChartTitle;
    private javax.swing.JLabel lblConfirm;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblDetails;
    private javax.swing.JLabel lblEmail;
    private javax.swing.JLabel lblFullName;
    private javax.swing.JLabel lblGender;
    private javax.swing.JLabel lblLogo;
    private javax.swing.JLabel lblNew;
    private javax.swing.JLabel lblOld;
    private javax.swing.JLabel lblPageSubtitle;
    private javax.swing.JLabel lblPageTitle;
    private javax.swing.JLabel lblPassword;
    private javax.swing.JLabel lblPhone;
    private javax.swing.JLabel lblPortal;
    private javax.swing.JLabel lblQualification;
    private javax.swing.JLabel lblReligion;
    private javax.swing.JLabel lblRole;
    private javax.swing.JLabel lblShow;
    private javax.swing.JLabel lblSpeciality;
    private javax.swing.JLabel lblTip;
    private javax.swing.JLabel lblUpcomingTitle;
    private javax.swing.JLabel lblUserName;
    private javax.swing.JPanel mainPanel;
    private utils.NavButton navAppointments;
    private utils.NavButton navFeedback;
    private utils.NavButton navHome;
    private utils.NavButton navProfile;
    private javax.swing.JPanel pageAppointments;
    private javax.swing.JPanel pageFeedback;
    private javax.swing.JPanel pageHome;
    private javax.swing.JPanel pageProfile;
    private javax.swing.JPanel pages;
    private javax.swing.JPanel passwordForm;
    private javax.swing.JPanel rowPanel;
    private javax.swing.JPanel saveBar;
    private javax.swing.JScrollPane scrollAppointments;
    private javax.swing.JScrollPane scrollFeedback;
    private javax.swing.JScrollPane scrollUpcoming;
    private javax.swing.JPanel securityBar;
    private utils.Ui.RoundPanel securityCard;
    private utils.GradientPanel sidebar;
    private javax.swing.JPanel statsPanel;
    private utils.Ui.RoundPanel tableCard;
    private javax.swing.JTable tblAppointments;
    private javax.swing.JTable tblFeedback;
    private javax.swing.JTable tblUpcoming;
    private javax.swing.JPanel titles;
    private javax.swing.JPanel toolbarPanel;
    private utils.TopBar topBar;
    private javax.swing.JPanel topRight;
    private javax.swing.JPasswordField txtConfirm;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtName;
    private javax.swing.JPasswordField txtNew;
    private javax.swing.JPasswordField txtOld;
    private javax.swing.JTextField txtPhone;
    private javax.swing.JTextField txtQualification;
    private javax.swing.JTextField txtReligion;
    private javax.swing.JTextField txtSpeciality;
    private utils.WindowButtons windowButtons;
    // End of variables declaration//GEN-END:variables
}
