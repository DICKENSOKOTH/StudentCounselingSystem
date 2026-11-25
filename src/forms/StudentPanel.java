package forms;

import dao.AppointmentDAO;
import dao.CounsellorDAO;
import dao.FeedbackDAO;
import dao.StudentDAO;
import dao.UserDAO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.text.JTextComponent;
import models.Appointment;
import models.Counsellor;
import models.Feedback;
import models.Student;
import models.User;
import studentcounsellingsystem.MainFrame;
import utils.PasswordUtil;
import utils.Session;
import utils.StarRating;
import utils.Ui;
import utils.Validate;

// student portal: sidebar menu + 4 pages, all designed in the Design tab (dashboard, book, my appointments, profile)
public class StudentPanel extends javax.swing.JPanel {

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final UserDAO userDAO = new UserDAO();
    private Student student;
    private List<Appointment> all = new ArrayList<>();
    private List<Appointment> shown = new ArrayList<>();
    private List<Appointment> upcoming = new ArrayList<>();
    private final User user = Session.getUser();
    private final java.util.Map<String, utils.NavButton> navButtons = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, String[]> pageText = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Runnable> refreshers = new java.util.LinkedHashMap<>();

    public StudentPanel() {
        initComponents();
        Ui.run(this, () -> student = studentDAO.findByUserId(user.getUserId()));
        if (student == null) { Ui.error(this, "No student profile was found for this account."); return; }
        applyTheme();
        refreshUserCard();
        lblRole.setText("Student");
        lblDate.setText(LocalDate.now().format(Ui.DATE));

        // the menu: button, page title, subtitle and what to load when the page opens
        addPage("HOME", navHome, "Dashboard", "Your counselling activity at a glance", this::refreshHome);
        addPage("BOOK", navBook, "Book Appointment", "Pick a counsellor, a day and a free hour", this::refreshBook);
        addPage("APPOINTMENTS", navAppointments, "My Appointments", "Reschedule, cancel or rate your sessions", this::refreshAppointments);
        addPage("PROFILE", navProfile, "My Profile", "Your details and password", this::refreshProfile);
        showPage("HOME");
    }

    //  look of the tables and fields (the layout itself is in the Design tab) 

    private void applyTheme() {
        Ui.styleTable(tblUpcoming, "Date", "Time", "Counsellor", "Reason", "Status");
        Ui.styleTable(tblAppointments, "Date", "Time", "Counsellor", "Reason", "Status", "Feedback");
        Ui.styleScroll(scrollUpcoming);
        Ui.styleScroll(scrollAppointments);
        scrollReason.setBorder(javax.swing.BorderFactory.createLineBorder(utils.Theme.LINE));
        // white cards get a thin outline
        for (javax.swing.JPanel p : new javax.swing.JPanel[]{counsellorCard, bookCard, cardDetails, cardPassword}) {
            p.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(utils.Theme.LINE),
                    javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        }
        for (JTextComponent t : new JTextComponent[]{txtReason, txtName, txtReg, txtEmail, txtPhone, txtCourse, txtOld, txtNew, txtConfirm}) { Ui.style(t); }
        cmbCounsellor.addActionListener(e -> counsellorChanged());
        // right-click menu on my appointments
        Ui.rowMenu(tblAppointments,
                new String[]{"Reschedule", "Cancel appointment", "Give / edit feedback", "Remove my feedback", "Delete (cancelled only)"},
                new Runnable[]{this::reschedule, this::cancelSelected, this::feedbackForSelected, this::removeFeedback, this::deleteSelected});
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
            all = appointmentDAO.findByStudent(student.getStudentId());
            LocalDate today = LocalDate.now();
            int up = 0, pending = 0, done = 0;
            upcoming = new ArrayList<>();
            for (Appointment a : all) {
                if (a.isOpen() && !a.getAppointmentDate().isBefore(today)) { upcoming.add(a); }
                if (Appointment.APPROVED.equals(a.getStatus()) && !a.getAppointmentDate().isBefore(today)) { up++; }
                if (Appointment.PENDING.equals(a.getStatus())) { pending++; }
                if (Appointment.COMPLETED.equals(a.getStatus())) { done++; }
            }
            Collections.reverse(upcoming);   // soonest first
            cardUpcoming.setValue(String.valueOf(up));
            cardPending.setValue(String.valueOf(pending));
            cardCompleted.setValue(String.valueOf(done));
            if (upcoming.isEmpty()) {
                lblNext.setText("No upcoming sessions yet. Book one when you are ready.");
            } else {
                Appointment n = upcoming.get(0);
                lblNext.setText("Your next session: " + n.getAppointmentDate().format(Ui.DATE) + " at " + n.getAppointmentTime().format(Ui.TIME)
                        + " with " + n.getCounsellorName() + " (" + n.getStatus().toLowerCase() + ")");
            }
            List<Object[]> rows = new ArrayList<>();
            for (Appointment a : upcoming) { rows.add(new Object[]{a.getAppointmentDate().format(Ui.DATE), a.getAppointmentTime().format(Ui.TIME), a.getCounsellorName(), a.getReason(), a.getStatus()}); }
            Ui.setRows(tblUpcoming, rows);
            Ui.widths(tblUpcoming, 150, 70, 150, 220, 100);
        });
    }

    private void refreshBook() {
        Ui.run(this, () -> {
            Object keep = cmbCounsellor.getSelectedItem();
            List<Counsellor> list = new CounsellorDAO().findAll();
            cmbCounsellor.setModel(new javax.swing.DefaultComboBoxModel<>(list.toArray(new Counsellor[0])));
            if (keep != null) { cmbCounsellor.setSelectedItem(keep); }
            counsellorChanged();
        });
    }

    private void bookAppointment() {
        Ui.run(this, () -> {
            Counsellor c = (Counsellor) cmbCounsellor.getSelectedItem();
            if (c == null) { throw new IllegalArgumentException("Choose a counsellor."); }
            if (slots.getTime() == null) { throw new IllegalArgumentException("Choose a day that has a free hour."); }
            if (Validate.blank(txtReason.getText()) || txtReason.getText().trim().length() < 3) { throw new IllegalArgumentException("Tell your counsellor briefly what you would like to talk about."); }
            Appointment a = new Appointment();
            a.setStudentId(student.getStudentId());
            a.setCounsellorId(c.getCounsellorId());
            a.setAppointmentDate(slots.getDate());
            a.setAppointmentTime(slots.getTime());
            a.setReason(txtReason.getText().trim());
            appointmentDAO.create(a);
            txtReason.setText("");
            Ui.info(this, "Your request was sent to " + c.getFullName() + ".\nYou will see it as APPROVED once the counsellor confirms.");
            showPage("APPOINTMENTS");
        });
    }

    private void refreshAppointments() {
        Ui.run(this, () -> {
            all = appointmentDAO.findByStudent(student.getStudentId());
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
            rows.add(new Object[]{a.getAppointmentDate().format(Ui.DATE), a.getAppointmentTime().format(Ui.TIME), a.getCounsellorName(), a.getReason(), a.getStatus(), StarRating.stars(a.getRating())});
        }
        Ui.setRows(tblAppointments, rows);
        Ui.widths(tblAppointments, 150, 70, 140, 200, 100, 100);
    }

    private Appointment selectedAppointment() {
        int i = Ui.selected(tblAppointments);
        if (i < 0) { Ui.info(this, "Select an appointment in the table first."); return null; }
        return shown.get(i);
    }

    private void reschedule() {
        Appointment a = selectedAppointment();
        if (a == null) { return; }
        if (!a.isOpen()) { Ui.info(this, "Only pending or approved appointments can be rescheduled."); return; }
        if (AppointmentForm.reschedule(this, a)) { Ui.info(this, "Moved. Your counsellor needs to approve the new time."); refreshAppointments(); }
    }

    private void cancelSelected() {
        Appointment a = selectedAppointment();
        if (a == null) { return; }
        if (!a.isOpen()) { Ui.info(this, "This appointment is already " + a.getStatus().toLowerCase() + "."); return; }
        if (Ui.confirm(this, "Cancel your appointment on " + a.getAppointmentDate().format(Ui.DATE) + " at " + a.getAppointmentTime().format(Ui.TIME) + "?")) {
            Ui.run(this, () -> { appointmentDAO.changeStatus(a, Appointment.CANCELLED); refreshAppointments(); });
        }
    }

    private void feedbackForSelected() {
        Appointment a = selectedAppointment();
        if (a == null) { return; }
        if (!Appointment.COMPLETED.equals(a.getStatus())) { Ui.info(this, "You can give feedback after the session is completed."); return; }
        Ui.run(this, () -> {
            Feedback existing = new FeedbackDAO().findByAppointment(a.getAppointmentId());
            boolean isNew = existing == null;
            Feedback f = isNew ? new Feedback() : existing;
            f.setAppointmentId(a.getAppointmentId());
            if (FeedbackForm.show(this, f, isNew)) { refreshAppointments(); }
        });
    }

    private void removeFeedback() {
        Appointment a = selectedAppointment();
        if (a == null) { return; }
        Ui.run(this, () -> {
            Feedback f = new FeedbackDAO().findByAppointment(a.getAppointmentId());
            if (f == null) { Ui.info(this, "There is no feedback to remove."); return; }
            if (Ui.confirm(this, "Remove your feedback for this session?")) { new FeedbackDAO().delete(f.getFeedbackId()); refreshAppointments(); }
        });
    }

    private void deleteSelected() {
        Appointment a = selectedAppointment();
        if (a == null) { return; }
        if (!Appointment.CANCELLED.equals(a.getStatus())) { Ui.info(this, "Only cancelled appointments can be deleted. Cancel it first."); return; }
        if (Ui.confirm(this, "Delete this cancelled appointment from your list?")) {
            Ui.run(this, () -> { appointmentDAO.delete(a.getAppointmentId()); refreshAppointments(); });
        }
    }

    // show the counsellor's details and their free hours
    private void counsellorChanged() {
        Counsellor c = (Counsellor) cmbCounsellor.getSelectedItem();
        if (c == null) { return; }
        lblCName.setText(c.getFullName());
        lblValSpecialization.setText(dash(c.getSpecialization()));
        lblValQualification.setText(dash(c.getQualification()));
        lblValGender.setText(dash(c.getGender()));
        lblValReligion.setText(dash(c.getReligion()));
        lblValEmail.setText(dash(c.getEmail()));
        slots.setCounsellor(c.getCounsellorId());
    }

    private static String dash(String s) { return s == null || s.isEmpty() ? "-" : s; }

    // fill the profile form with the signed-in student
    private void refreshProfile() {
        txtName.setText(student.getFullName());
        txtReg.setText(student.getRegistrationNumber());
        txtEmail.setText(student.getEmail());
        txtPhone.setText(student.getPhone() == null ? "" : student.getPhone());
        txtCourse.setText(student.getCourse());
        cmbYear.setSelectedItem(String.valueOf(student.getYearOfStudy()));
    }

    private void saveProfile() {
        Ui.run(this, () -> {
            if (Validate.blank(txtName.getText())) { throw new IllegalArgumentException("Enter your full name."); }
            if (!Validate.phone(txtPhone.getText())) { throw new IllegalArgumentException("Enter a valid phone number (or leave it empty)."); }
            if (Validate.blank(txtCourse.getText())) { throw new IllegalArgumentException("Enter your course."); }
            student.setFullName(txtName.getText().trim());
            student.setPhone(txtPhone.getText().trim());
            student.setCourse(txtCourse.getText().trim());
            student.setYearOfStudy(Integer.parseInt((String) cmbYear.getSelectedItem()));
            studentDAO.update(student);
            user.setFullName(student.getFullName());
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

        sidebar = new javax.swing.JPanel();
        userCard = new javax.swing.JPanel();
        avatar = new utils.Avatar();
        userText = new javax.swing.JPanel();
        lblUserName = new javax.swing.JLabel();
        lblRole = new javax.swing.JLabel();
        navWrap = new javax.swing.JPanel();
        navPanel = new javax.swing.JPanel();
        navHome = new utils.NavButton();
        navBook = new utils.NavButton();
        navAppointments = new utils.NavButton();
        navProfile = new utils.NavButton();
        logoutPanel = new javax.swing.JPanel();
        btnLogout = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        headerPanel = new javax.swing.JPanel();
        titlePanel = new javax.swing.JPanel();
        lblPageTitle = new javax.swing.JLabel();
        lblPageSubtitle = new javax.swing.JLabel();
        lblDate = new javax.swing.JLabel();
        pages = new javax.swing.JPanel();
        pageHome = new javax.swing.JPanel();
        topPanel = new javax.swing.JPanel();
        statsPanel = new javax.swing.JPanel();
        pnlStat1 = new javax.swing.JPanel();
        lblStat1 = new javax.swing.JLabel();
        cardUpcoming = new utils.StatCard();
        pnlStat2 = new javax.swing.JPanel();
        lblStat2 = new javax.swing.JLabel();
        cardPending = new utils.StatCard();
        pnlStat3 = new javax.swing.JPanel();
        lblStat3 = new javax.swing.JLabel();
        cardCompleted = new utils.StatCard();
        lblNext = new javax.swing.JLabel();
        scrollUpcoming = new javax.swing.JScrollPane();
        tblUpcoming = new javax.swing.JTable();
        actionsPanel = new javax.swing.JPanel();
        btnBook = new javax.swing.JButton();
        btnMine = new javax.swing.JButton();
        pageBook = new javax.swing.JPanel();
        counsellorCard = new javax.swing.JPanel();
        pnlPick = new javax.swing.JPanel();
        lblPick = new javax.swing.JLabel();
        cmbCounsellor = new javax.swing.JComboBox<models.Counsellor>();
        pnlDetails = new javax.swing.JPanel();
        lblCName = new javax.swing.JLabel();
        pnlGridWrap = new javax.swing.JPanel();
        detailsGrid = new javax.swing.JPanel();
        lblSpecialization = new javax.swing.JLabel();
        lblValSpecialization = new javax.swing.JLabel();
        lblQualification = new javax.swing.JLabel();
        lblValQualification = new javax.swing.JLabel();
        lblGender = new javax.swing.JLabel();
        lblValGender = new javax.swing.JLabel();
        lblReligion = new javax.swing.JLabel();
        lblValReligion = new javax.swing.JLabel();
        lblEmail = new javax.swing.JLabel();
        lblValEmail = new javax.swing.JLabel();
        bookCard = new javax.swing.JPanel();
        lblSlotsTitle = new javax.swing.JLabel();
        slots = new forms.SlotPicker();
        pnlReason = new javax.swing.JPanel();
        lblReason = new javax.swing.JLabel();
        scrollReason = new javax.swing.JScrollPane();
        txtReason = new javax.swing.JTextArea();
        pnlRequest = new javax.swing.JPanel();
        btnRequest = new javax.swing.JButton();
        pageAppointments = new javax.swing.JPanel();
        toolbarPanel = new javax.swing.JPanel();
        filterPanel = new javax.swing.JPanel();
        lblShow = new javax.swing.JLabel();
        cmbFilter = new javax.swing.JComboBox<String>();
        buttonBar = new javax.swing.JPanel();
        btnReschedule = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        btnFeedback = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        scrollAppointments = new javax.swing.JScrollPane();
        tblAppointments = new javax.swing.JTable();
        lblTip = new javax.swing.JLabel();
        pageProfile = new javax.swing.JPanel();
        cardDetails = new javax.swing.JPanel();
        lblDetailsTitle = new javax.swing.JLabel();
        pnlDetailsWrap = new javax.swing.JPanel();
        formDetails = new javax.swing.JPanel();
        lblName = new javax.swing.JLabel();
        txtName = new javax.swing.JTextField();
        lblReg = new javax.swing.JLabel();
        txtReg = new javax.swing.JTextField();
        lblEmailP = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        lblPhone = new javax.swing.JLabel();
        txtPhone = new javax.swing.JTextField();
        lblCourse = new javax.swing.JLabel();
        txtCourse = new javax.swing.JTextField();
        lblYear = new javax.swing.JLabel();
        cmbYear = new javax.swing.JComboBox<String>();
        pnlSave = new javax.swing.JPanel();
        btnSave = new javax.swing.JButton();
        cardPassword = new javax.swing.JPanel();
        lblPwTitle = new javax.swing.JLabel();
        pnlPwWrap = new javax.swing.JPanel();
        formPw = new javax.swing.JPanel();
        lblOld = new javax.swing.JLabel();
        txtOld = new javax.swing.JPasswordField();
        lblNew = new javax.swing.JLabel();
        txtNew = new javax.swing.JPasswordField();
        lblConfirm = new javax.swing.JLabel();
        txtConfirm = new javax.swing.JPasswordField();
        pnlPw = new javax.swing.JPanel();
        btnChange = new javax.swing.JButton();
        btnDeleteAccount = new javax.swing.JButton();

        this.setPreferredSize(new java.awt.Dimension(1100, 700));
        setLayout(new java.awt.BorderLayout());
        sidebar.setOpaque(true);
        sidebar.setBackground(new java.awt.Color(12, 42, 77));
        sidebar.setPreferredSize(new java.awt.Dimension(230, 100));
        sidebar.setLayout(new java.awt.BorderLayout());
        userCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 18, 18, 12));
        userCard.setOpaque(false);
        userCard.setLayout(new java.awt.BorderLayout(12, 0));
        avatar.setPreferredSize(new java.awt.Dimension(44, 44));

        userCard.add(avatar, java.awt.BorderLayout.WEST);
        userText.setOpaque(false);
        userText.setLayout(new java.awt.GridLayout(2, 1, 0, 2));
        lblUserName.setText("User");
        lblUserName.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblUserName.setForeground(new java.awt.Color(255, 255, 255));

        userText.add(lblUserName);
        lblRole.setText("Student");
        lblRole.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblRole.setForeground(new java.awt.Color(190, 205, 225));

        userText.add(lblRole);

        userCard.add(userText, java.awt.BorderLayout.CENTER);

        sidebar.add(userCard, java.awt.BorderLayout.NORTH);
        navWrap.setOpaque(false);
        navWrap.setLayout(new java.awt.BorderLayout());
        navPanel.setPreferredSize(new java.awt.Dimension(100, 200));
        navPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));
        navPanel.setOpaque(false);
        navPanel.setLayout(new java.awt.GridLayout(4, 1, 0, 6));
        navHome.setText("Dashboard");
        navHome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navHomeActionPerformed(evt);
            }
        });

        navPanel.add(navHome);
        navBook.setText("Book Appointment");
        navBook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navBookActionPerformed(evt);
            }
        });

        navPanel.add(navBook);
        navAppointments.setText("My Appointments");
        navAppointments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navAppointmentsActionPerformed(evt);
            }
        });

        navPanel.add(navAppointments);
        navProfile.setText("My Profile");
        navProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navProfileActionPerformed(evt);
            }
        });

        navPanel.add(navProfile);

        navWrap.add(navPanel, java.awt.BorderLayout.NORTH);

        sidebar.add(navWrap, java.awt.BorderLayout.CENTER);
        logoutPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 20, 16));
        logoutPanel.setOpaque(false);
        logoutPanel.setLayout(new java.awt.BorderLayout());
        btnLogout.setText("Log out");
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });

        logoutPanel.add(btnLogout, java.awt.BorderLayout.CENTER);

        sidebar.add(logoutPanel, java.awt.BorderLayout.SOUTH);

        add(sidebar, java.awt.BorderLayout.WEST);
        mainPanel.setOpaque(true);
        mainPanel.setBackground(new java.awt.Color(244, 247, 251));
        mainPanel.setLayout(new java.awt.BorderLayout());
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 28, 0, 28));
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new java.awt.BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new java.awt.GridLayout(2, 1, 0, 2));
        lblPageTitle.setText("Dashboard");
        lblPageTitle.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        lblPageTitle.setForeground(new java.awt.Color(12, 42, 77));

        titlePanel.add(lblPageTitle);
        lblPageSubtitle.setText("");
        lblPageSubtitle.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblPageSubtitle.setForeground(new java.awt.Color(91, 107, 130));

        titlePanel.add(lblPageSubtitle);

        headerPanel.add(titlePanel, java.awt.BorderLayout.WEST);
        lblDate.setHorizontalAlignment(4);
        lblDate.setText("");
        lblDate.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblDate.setForeground(new java.awt.Color(91, 107, 130));

        headerPanel.add(lblDate, java.awt.BorderLayout.EAST);

        mainPanel.add(headerPanel, java.awt.BorderLayout.NORTH);
        pages.setOpaque(false);
        pages.setLayout(new java.awt.CardLayout());
        pageHome.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageHome.setOpaque(false);
        pageHome.setLayout(new java.awt.BorderLayout(0, 16));
        topPanel.setPreferredSize(new java.awt.Dimension(100, 184));
        topPanel.setOpaque(false);
        topPanel.setLayout(new java.awt.BorderLayout(0, 14));
        statsPanel.setPreferredSize(new java.awt.Dimension(100, 104));
        statsPanel.setOpaque(false);
        statsPanel.setLayout(new java.awt.GridLayout(1, 3, 16, 0));
        pnlStat1.setOpaque(false);
        pnlStat1.setLayout(new java.awt.BorderLayout(0, 6));
        lblStat1.setText("Upcoming sessions");
        lblStat1.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStat1.setForeground(new java.awt.Color(91, 107, 130));

        pnlStat1.add(lblStat1, java.awt.BorderLayout.NORTH);

        pnlStat1.add(cardUpcoming, java.awt.BorderLayout.CENTER);

        statsPanel.add(pnlStat1);
        pnlStat2.setOpaque(false);
        pnlStat2.setLayout(new java.awt.BorderLayout(0, 6));
        lblStat2.setText("Pending requests");
        lblStat2.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStat2.setForeground(new java.awt.Color(91, 107, 130));

        pnlStat2.add(lblStat2, java.awt.BorderLayout.NORTH);

        pnlStat2.add(cardPending, java.awt.BorderLayout.CENTER);

        statsPanel.add(pnlStat2);
        pnlStat3.setOpaque(false);
        pnlStat3.setLayout(new java.awt.BorderLayout(0, 6));
        lblStat3.setText("Completed sessions");
        lblStat3.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblStat3.setForeground(new java.awt.Color(91, 107, 130));

        pnlStat3.add(lblStat3, java.awt.BorderLayout.NORTH);

        pnlStat3.add(cardCompleted, java.awt.BorderLayout.CENTER);

        statsPanel.add(pnlStat3);

        topPanel.add(statsPanel, java.awt.BorderLayout.NORTH);
        lblNext.setText("");
        lblNext.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblNext.setForeground(new java.awt.Color(12, 42, 77));

        topPanel.add(lblNext, java.awt.BorderLayout.CENTER);

        pageHome.add(topPanel, java.awt.BorderLayout.NORTH);

        scrollUpcoming.setViewportView(tblUpcoming);

        pageHome.add(scrollUpcoming, java.awt.BorderLayout.CENTER);
        actionsPanel.setOpaque(false);
        actionsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));
        btnBook.setText("Book an appointment");
        btnBook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBookActionPerformed(evt);
            }
        });

        actionsPanel.add(btnBook);
        btnMine.setText("View my appointments");
        btnMine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMineActionPerformed(evt);
            }
        });

        actionsPanel.add(btnMine);

        pageHome.add(actionsPanel, java.awt.BorderLayout.SOUTH);

        pages.add(pageHome, "HOME");
        pageBook.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageBook.setOpaque(false);
        pageBook.setLayout(new java.awt.BorderLayout(18, 0));
        counsellorCard.setPreferredSize(new java.awt.Dimension(380, 100));
        counsellorCard.setOpaque(true);
        counsellorCard.setBackground(new java.awt.Color(255, 255, 255));
        counsellorCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        counsellorCard.setLayout(new java.awt.BorderLayout(0, 12));
        pnlPick.setOpaque(false);
        pnlPick.setLayout(new java.awt.BorderLayout(0, 6));
        lblPick.setText("Choose a counsellor");
        lblPick.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblPick.setForeground(new java.awt.Color(12, 42, 77));

        pnlPick.add(lblPick, java.awt.BorderLayout.NORTH);

        pnlPick.add(cmbCounsellor, java.awt.BorderLayout.CENTER);

        counsellorCard.add(pnlPick, java.awt.BorderLayout.NORTH);
        pnlDetails.setOpaque(false);
        pnlDetails.setLayout(new java.awt.BorderLayout(0, 10));
        lblCName.setText("");
        lblCName.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblCName.setForeground(new java.awt.Color(12, 42, 77));

        pnlDetails.add(lblCName, java.awt.BorderLayout.NORTH);
        pnlGridWrap.setOpaque(false);
        pnlGridWrap.setLayout(new java.awt.BorderLayout());
        detailsGrid.setOpaque(false);
        detailsGrid.setLayout(new java.awt.GridLayout(5, 2, 10, 10));
        lblSpecialization.setText("Specialization");
        lblSpecialization.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblSpecialization.setForeground(new java.awt.Color(91, 107, 130));

        detailsGrid.add(lblSpecialization);
        lblValSpecialization.setText("-");
        lblValSpecialization.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblValSpecialization.setForeground(new java.awt.Color(12, 42, 77));

        detailsGrid.add(lblValSpecialization);
        lblQualification.setText("Qualification");
        lblQualification.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblQualification.setForeground(new java.awt.Color(91, 107, 130));

        detailsGrid.add(lblQualification);
        lblValQualification.setText("-");
        lblValQualification.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblValQualification.setForeground(new java.awt.Color(12, 42, 77));

        detailsGrid.add(lblValQualification);
        lblGender.setText("Gender");
        lblGender.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblGender.setForeground(new java.awt.Color(91, 107, 130));

        detailsGrid.add(lblGender);
        lblValGender.setText("-");
        lblValGender.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblValGender.setForeground(new java.awt.Color(12, 42, 77));

        detailsGrid.add(lblValGender);
        lblReligion.setText("Religion");
        lblReligion.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblReligion.setForeground(new java.awt.Color(91, 107, 130));

        detailsGrid.add(lblReligion);
        lblValReligion.setText("-");
        lblValReligion.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblValReligion.setForeground(new java.awt.Color(12, 42, 77));

        detailsGrid.add(lblValReligion);
        lblEmail.setText("Email");
        lblEmail.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblEmail.setForeground(new java.awt.Color(91, 107, 130));

        detailsGrid.add(lblEmail);
        lblValEmail.setText("-");
        lblValEmail.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblValEmail.setForeground(new java.awt.Color(12, 42, 77));

        detailsGrid.add(lblValEmail);

        pnlGridWrap.add(detailsGrid, java.awt.BorderLayout.NORTH);

        pnlDetails.add(pnlGridWrap, java.awt.BorderLayout.CENTER);

        counsellorCard.add(pnlDetails, java.awt.BorderLayout.CENTER);

        pageBook.add(counsellorCard, java.awt.BorderLayout.WEST);
        bookCard.setOpaque(true);
        bookCard.setBackground(new java.awt.Color(255, 255, 255));
        bookCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        bookCard.setLayout(new java.awt.BorderLayout(0, 10));
        lblSlotsTitle.setText("Pick a day and a free hour");
        lblSlotsTitle.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblSlotsTitle.setForeground(new java.awt.Color(12, 42, 77));

        bookCard.add(lblSlotsTitle, java.awt.BorderLayout.NORTH);

        bookCard.add(slots, java.awt.BorderLayout.CENTER);
        pnlReason.setOpaque(false);
        pnlReason.setLayout(new java.awt.BorderLayout(0, 8));
        lblReason.setText("What would you like to talk about?");
        lblReason.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblReason.setForeground(new java.awt.Color(12, 42, 77));

        pnlReason.add(lblReason, java.awt.BorderLayout.NORTH);
        scrollReason.setPreferredSize(new java.awt.Dimension(100, 90));
        txtReason.setColumns(20);
        txtReason.setRows(4);
        txtReason.setLineWrap(true);
        txtReason.setWrapStyleWord(true);

        scrollReason.setViewportView(txtReason);

        pnlReason.add(scrollReason, java.awt.BorderLayout.CENTER);
        pnlRequest.setOpaque(false);
        pnlRequest.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 5));
        btnRequest.setText("Request appointment");
        btnRequest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRequestActionPerformed(evt);
            }
        });

        pnlRequest.add(btnRequest);

        pnlReason.add(pnlRequest, java.awt.BorderLayout.SOUTH);

        bookCard.add(pnlReason, java.awt.BorderLayout.SOUTH);

        pageBook.add(bookCard, java.awt.BorderLayout.CENTER);

        pages.add(pageBook, "BOOK");
        pageAppointments.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageAppointments.setOpaque(false);
        pageAppointments.setLayout(new java.awt.BorderLayout(0, 12));
        toolbarPanel.setOpaque(false);
        toolbarPanel.setLayout(new java.awt.BorderLayout(12, 0));
        filterPanel.setOpaque(false);
        filterPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));
        lblShow.setText("Show");
        lblShow.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblShow.setForeground(new java.awt.Color(12, 42, 77));

        filterPanel.add(lblShow);
        cmbFilter.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "All", "PENDING", "APPROVED", "COMPLETED", "CANCELLED" }));
        cmbFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbFilterActionPerformed(evt);
            }
        });

        filterPanel.add(cmbFilter);

        toolbarPanel.add(filterPanel, java.awt.BorderLayout.WEST);
        buttonBar.setOpaque(false);
        buttonBar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));
        btnReschedule.setText("Reschedule");
        btnReschedule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRescheduleActionPerformed(evt);
            }
        });

        buttonBar.add(btnReschedule);
        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        buttonBar.add(btnCancel);
        btnFeedback.setText("Feedback");
        btnFeedback.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFeedbackActionPerformed(evt);
            }
        });

        buttonBar.add(btnFeedback);
        btnDelete.setText("Delete");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        buttonBar.add(btnDelete);

        toolbarPanel.add(buttonBar, java.awt.BorderLayout.EAST);

        pageAppointments.add(toolbarPanel, java.awt.BorderLayout.NORTH);

        scrollAppointments.setViewportView(tblAppointments);

        pageAppointments.add(scrollAppointments, java.awt.BorderLayout.CENTER);
        lblTip.setText("Tip: right-click an appointment for more actions.");
        lblTip.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblTip.setForeground(new java.awt.Color(91, 107, 130));

        pageAppointments.add(lblTip, java.awt.BorderLayout.SOUTH);

        pages.add(pageAppointments, "APPOINTMENTS");
        pageProfile.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 28, 22, 28));
        pageProfile.setOpaque(false);
        pageProfile.setLayout(new java.awt.GridLayout(1, 2, 18, 0));
        cardDetails.setOpaque(true);
        cardDetails.setBackground(new java.awt.Color(255, 255, 255));
        cardDetails.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        cardDetails.setLayout(new java.awt.BorderLayout(0, 14));
        lblDetailsTitle.setText("My details");
        lblDetailsTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblDetailsTitle.setForeground(new java.awt.Color(12, 42, 77));

        cardDetails.add(lblDetailsTitle, java.awt.BorderLayout.NORTH);
        pnlDetailsWrap.setOpaque(false);
        pnlDetailsWrap.setLayout(new java.awt.BorderLayout());
        formDetails.setOpaque(false);
        formDetails.setLayout(new java.awt.GridLayout(6, 2, 12, 12));
        lblName.setText("Full name");
        lblName.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblName.setForeground(new java.awt.Color(91, 107, 130));

        formDetails.add(lblName);

        formDetails.add(txtName);
        lblReg.setText("Registration number");
        lblReg.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblReg.setForeground(new java.awt.Color(91, 107, 130));

        formDetails.add(lblReg);
        txtReg.setEditable(false);

        formDetails.add(txtReg);
        lblEmailP.setText("Email");
        lblEmailP.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblEmailP.setForeground(new java.awt.Color(91, 107, 130));

        formDetails.add(lblEmailP);
        txtEmail.setEditable(false);

        formDetails.add(txtEmail);
        lblPhone.setText("Phone");
        lblPhone.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblPhone.setForeground(new java.awt.Color(91, 107, 130));

        formDetails.add(lblPhone);

        formDetails.add(txtPhone);
        lblCourse.setText("Course");
        lblCourse.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblCourse.setForeground(new java.awt.Color(91, 107, 130));

        formDetails.add(lblCourse);

        formDetails.add(txtCourse);
        lblYear.setText("Year of study");
        lblYear.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblYear.setForeground(new java.awt.Color(91, 107, 130));

        formDetails.add(lblYear);
        cmbYear.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "1", "2", "3", "4", "5", "6" }));

        formDetails.add(cmbYear);

        pnlDetailsWrap.add(formDetails, java.awt.BorderLayout.NORTH);

        cardDetails.add(pnlDetailsWrap, java.awt.BorderLayout.CENTER);
        pnlSave.setOpaque(false);
        pnlSave.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 5));
        btnSave.setText("Save changes");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        pnlSave.add(btnSave);

        cardDetails.add(pnlSave, java.awt.BorderLayout.SOUTH);

        pageProfile.add(cardDetails);
        cardPassword.setOpaque(true);
        cardPassword.setBackground(new java.awt.Color(255, 255, 255));
        cardPassword.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        cardPassword.setLayout(new java.awt.BorderLayout(0, 14));
        lblPwTitle.setText("Change password");
        lblPwTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblPwTitle.setForeground(new java.awt.Color(12, 42, 77));

        cardPassword.add(lblPwTitle, java.awt.BorderLayout.NORTH);
        pnlPwWrap.setOpaque(false);
        pnlPwWrap.setLayout(new java.awt.BorderLayout());
        formPw.setOpaque(false);
        formPw.setLayout(new java.awt.GridLayout(3, 2, 12, 12));
        lblOld.setText("Current password");
        lblOld.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblOld.setForeground(new java.awt.Color(91, 107, 130));

        formPw.add(lblOld);

        formPw.add(txtOld);
        lblNew.setText("New password");
        lblNew.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblNew.setForeground(new java.awt.Color(91, 107, 130));

        formPw.add(lblNew);

        formPw.add(txtNew);
        lblConfirm.setText("Confirm new password");
        lblConfirm.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblConfirm.setForeground(new java.awt.Color(91, 107, 130));

        formPw.add(lblConfirm);

        formPw.add(txtConfirm);

        pnlPwWrap.add(formPw, java.awt.BorderLayout.NORTH);

        cardPassword.add(pnlPwWrap, java.awt.BorderLayout.CENTER);
        pnlPw.setOpaque(false);
        pnlPw.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));
        btnChange.setText("Change password");
        btnChange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChangeActionPerformed(evt);
            }
        });

        pnlPw.add(btnChange);
        btnDeleteAccount.setText("Delete my account");
        btnDeleteAccount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteAccountActionPerformed(evt);
            }
        });

        pnlPw.add(btnDeleteAccount);

        cardPassword.add(pnlPw, java.awt.BorderLayout.SOUTH);

        pageProfile.add(cardPassword);

        pages.add(pageProfile, "PROFILE");

        mainPanel.add(pages, java.awt.BorderLayout.CENTER);

        add(mainPanel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void navHomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navHomeActionPerformed
        showPage("HOME");
    }//GEN-LAST:event_navHomeActionPerformed

    private void navBookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navBookActionPerformed
        showPage("BOOK");
    }//GEN-LAST:event_navBookActionPerformed

    private void navAppointmentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navAppointmentsActionPerformed
        showPage("APPOINTMENTS");
    }//GEN-LAST:event_navAppointmentsActionPerformed

    private void navProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navProfileActionPerformed
        showPage("PROFILE");
    }//GEN-LAST:event_navProfileActionPerformed

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        logout();
    }//GEN-LAST:event_btnLogoutActionPerformed

    private void btnBookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBookActionPerformed
        showPage("BOOK");
    }//GEN-LAST:event_btnBookActionPerformed

    private void btnMineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMineActionPerformed
        showPage("APPOINTMENTS");
    }//GEN-LAST:event_btnMineActionPerformed

    private void btnRequestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRequestActionPerformed
        bookAppointment();
    }//GEN-LAST:event_btnRequestActionPerformed

    private void cmbFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbFilterActionPerformed
        showFiltered();
    }//GEN-LAST:event_cmbFilterActionPerformed

    private void btnRescheduleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRescheduleActionPerformed
        reschedule();
    }//GEN-LAST:event_btnRescheduleActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        cancelSelected();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnFeedbackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFeedbackActionPerformed
        feedbackForSelected();
    }//GEN-LAST:event_btnFeedbackActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        deleteSelected();
    }//GEN-LAST:event_btnDeleteActionPerformed

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
    private javax.swing.JPanel actionsPanel;
    private utils.Avatar avatar;
    private javax.swing.JPanel bookCard;
    private javax.swing.JButton btnBook;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnChange;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnDeleteAccount;
    private javax.swing.JButton btnFeedback;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnMine;
    private javax.swing.JButton btnRequest;
    private javax.swing.JButton btnReschedule;
    private javax.swing.JButton btnSave;
    private javax.swing.JPanel buttonBar;
    private utils.StatCard cardCompleted;
    private javax.swing.JPanel cardDetails;
    private javax.swing.JPanel cardPassword;
    private utils.StatCard cardPending;
    private utils.StatCard cardUpcoming;
    private javax.swing.JComboBox<models.Counsellor> cmbCounsellor;
    private javax.swing.JComboBox<String> cmbFilter;
    private javax.swing.JComboBox<String> cmbYear;
    private javax.swing.JPanel counsellorCard;
    private javax.swing.JPanel detailsGrid;
    private javax.swing.JPanel filterPanel;
    private javax.swing.JPanel formDetails;
    private javax.swing.JPanel formPw;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JLabel lblCName;
    private javax.swing.JLabel lblConfirm;
    private javax.swing.JLabel lblCourse;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblDetailsTitle;
    private javax.swing.JLabel lblEmail;
    private javax.swing.JLabel lblEmailP;
    private javax.swing.JLabel lblGender;
    private javax.swing.JLabel lblName;
    private javax.swing.JLabel lblNew;
    private javax.swing.JLabel lblNext;
    private javax.swing.JLabel lblOld;
    private javax.swing.JLabel lblPageSubtitle;
    private javax.swing.JLabel lblPageTitle;
    private javax.swing.JLabel lblPhone;
    private javax.swing.JLabel lblPick;
    private javax.swing.JLabel lblPwTitle;
    private javax.swing.JLabel lblQualification;
    private javax.swing.JLabel lblReason;
    private javax.swing.JLabel lblReg;
    private javax.swing.JLabel lblReligion;
    private javax.swing.JLabel lblRole;
    private javax.swing.JLabel lblShow;
    private javax.swing.JLabel lblSlotsTitle;
    private javax.swing.JLabel lblSpecialization;
    private javax.swing.JLabel lblStat1;
    private javax.swing.JLabel lblStat2;
    private javax.swing.JLabel lblStat3;
    private javax.swing.JLabel lblTip;
    private javax.swing.JLabel lblUserName;
    private javax.swing.JLabel lblValEmail;
    private javax.swing.JLabel lblValGender;
    private javax.swing.JLabel lblValQualification;
    private javax.swing.JLabel lblValReligion;
    private javax.swing.JLabel lblValSpecialization;
    private javax.swing.JLabel lblYear;
    private javax.swing.JPanel logoutPanel;
    private javax.swing.JPanel mainPanel;
    private utils.NavButton navAppointments;
    private utils.NavButton navBook;
    private utils.NavButton navHome;
    private javax.swing.JPanel navPanel;
    private utils.NavButton navProfile;
    private javax.swing.JPanel navWrap;
    private javax.swing.JPanel pageAppointments;
    private javax.swing.JPanel pageBook;
    private javax.swing.JPanel pageHome;
    private javax.swing.JPanel pageProfile;
    private javax.swing.JPanel pages;
    private javax.swing.JPanel pnlDetails;
    private javax.swing.JPanel pnlDetailsWrap;
    private javax.swing.JPanel pnlGridWrap;
    private javax.swing.JPanel pnlPick;
    private javax.swing.JPanel pnlPw;
    private javax.swing.JPanel pnlPwWrap;
    private javax.swing.JPanel pnlReason;
    private javax.swing.JPanel pnlRequest;
    private javax.swing.JPanel pnlSave;
    private javax.swing.JPanel pnlStat1;
    private javax.swing.JPanel pnlStat2;
    private javax.swing.JPanel pnlStat3;
    private javax.swing.JScrollPane scrollAppointments;
    private javax.swing.JScrollPane scrollReason;
    private javax.swing.JScrollPane scrollUpcoming;
    private javax.swing.JPanel sidebar;
    private forms.SlotPicker slots;
    private javax.swing.JPanel statsPanel;
    private javax.swing.JTable tblAppointments;
    private javax.swing.JTable tblUpcoming;
    private javax.swing.JPanel titlePanel;
    private javax.swing.JPanel toolbarPanel;
    private javax.swing.JPanel topPanel;
    private javax.swing.JPasswordField txtConfirm;
    private javax.swing.JTextField txtCourse;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtName;
    private javax.swing.JPasswordField txtNew;
    private javax.swing.JPasswordField txtOld;
    private javax.swing.JTextField txtPhone;
    private javax.swing.JTextArea txtReason;
    private javax.swing.JTextField txtReg;
    private javax.swing.JPanel userCard;
    private javax.swing.JPanel userText;
    // End of variables declaration//GEN-END:variables
}
