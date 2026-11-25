package forms;

import dao.UserDAO;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import models.Counsellor;
import models.Student;
import models.User;
import studentcounsellingsystem.MainFrame;
import utils.PasswordUtil;
import utils.Theme;
import utils.Ui;
import utils.Validate;

// create a student or counsellor account (administrators are added by an administrator)
public class RegisterPanel extends javax.swing.JPanel {

    private String role = User.STUDENT;

    public RegisterPanel() {
        initComponents();
        for (JTextComponent t : new JTextComponent[]{txtName, txtEmail, txtPhone, txtPassword, txtConfirm, txtReg, txtCourse, txtSpeciality, txtQualification, txtReligion}) { Ui.style(t); }
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) {
                MainFrame f = frame();
                selectRole(f != null && User.COUNSELLOR.equals(f.getSelectedRole()) ? User.COUNSELLOR : User.STUDENT);
            }
        });
        selectRole(User.STUDENT);
    }

    // highlight the chosen tab and show the extra fields of that role
    private void selectRole(String newRole) {
        role = newRole;
        boolean student = User.STUDENT.equals(role);
        btnTabStudent.setBackground(student ? Theme.NAVY : new Color(0xE8, 0xEE, 0xF6));
        btnTabStudent.setForeground(student ? Color.WHITE : Theme.NAVY);
        btnTabCounsellor.setBackground(student ? new Color(0xE8, 0xEE, 0xF6) : Theme.NAVY);
        btnTabCounsellor.setForeground(student ? Theme.NAVY : Color.WHITE);
        ((CardLayout) extras.getLayout()).show(extras, role);
    }

    private MainFrame frame() {
        java.awt.Window w = SwingUtilities.getWindowAncestor(this);
        return w instanceof MainFrame ? (MainFrame) w : null;
    }

    private void goTo(String screen) {
        MainFrame f = frame();
        if (f != null) { f.setSelectedRole(role); f.showScreen(screen); }
    }

    private void register() {
        Ui.run(this, () -> {
            String pw = new String(txtPassword.getPassword());
            if (Validate.blank(txtName.getText())) { throw new IllegalArgumentException("Enter your full name."); }
            if (!Validate.email(txtEmail.getText())) { throw new IllegalArgumentException("Enter a valid email address."); }
            if (!Validate.phone(txtPhone.getText())) { throw new IllegalArgumentException("Enter a valid phone number (or leave it empty)."); }
            if (Validate.password(pw) != null) { throw new IllegalArgumentException(Validate.password(pw)); }
            if (!pw.equals(new String(txtConfirm.getPassword()))) { throw new IllegalArgumentException("The two passwords do not match."); }

            User u = new User();
            u.setFullName(txtName.getText().trim());
            u.setEmail(txtEmail.getText().trim());
            u.setPhone(txtPhone.getText().trim());
            u.setPasswordHash(PasswordUtil.hash(pw));
            UserDAO users = new UserDAO();
            if (User.STUDENT.equals(role)) {
                if (Validate.blank(txtReg.getText()) || Validate.blank(txtCourse.getText())) { throw new IllegalArgumentException("Enter your registration number and course."); }
                Student s = new Student();
                s.setRegistrationNumber(txtReg.getText().trim());
                s.setCourse(txtCourse.getText().trim());
                s.setYearOfStudy(Integer.parseInt((String) cmbYear.getSelectedItem()));
                users.createStudent(u, s);
            } else {
                if (Validate.blank(txtSpeciality.getText())) { throw new IllegalArgumentException("Enter your specialization."); }
                Counsellor c = new Counsellor();
                c.setSpecialization(txtSpeciality.getText().trim());
                c.setQualification(txtQualification.getText().trim());
                c.setGender((String) cmbGender.getSelectedItem());
                c.setReligion(txtReligion.getText().trim());
                users.createCounsellor(u, c);
            }
            Ui.info(this, "Your account was created.\nYou can sign in now.");
            clear();
            MainFrame f = frame();
            if (f != null) {
                f.setSelectedRole(role);
                f.showScreen("LOGIN");
                for (java.awt.Component comp : f.getContentPane().getComponents()) {
                    if (comp instanceof LoginPanel) { ((LoginPanel) comp).prefill(u.getEmail()); }
                }
            }
        });
    }

    private void clear() {
        for (JTextComponent t : new JTextComponent[]{txtName, txtEmail, txtPhone, txtReg, txtCourse, txtSpeciality, txtQualification, txtReligion, txtPassword, txtConfirm}) { t.setText(""); }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        background = new utils.AnimatedBackground();
        topPanel = new javax.swing.JPanel();
        lblLogo = new javax.swing.JLabel();
        windowButtons = new utils.WindowButtons();
        middlePanel = new javax.swing.JPanel();
        cardPanel = new utils.Ui.RoundPanel();
        headPanel = new javax.swing.JPanel();
        tabRow = new javax.swing.JPanel();
        btnTabStudent = new utils.Ui.PillButton();
        btnTabCounsellor = new utils.Ui.PillButton();
        lblTitle = new javax.swing.JLabel();
        bodyPanel = new javax.swing.JPanel();
        grid = new javax.swing.JPanel();
        cellName = new javax.swing.JPanel();
        lblName = new javax.swing.JLabel();
        txtName = new javax.swing.JTextField();
        cellEmail = new javax.swing.JPanel();
        lblEmail = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        cellPhone = new javax.swing.JPanel();
        lblPhone = new javax.swing.JLabel();
        txtPhone = new javax.swing.JTextField();
        cellPassword = new javax.swing.JPanel();
        lblPassword = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        cellConfirm = new javax.swing.JPanel();
        lblConfirm = new javax.swing.JLabel();
        txtConfirm = new javax.swing.JPasswordField();
        spacer = new javax.swing.JPanel();
        extrasTop = new javax.swing.JPanel();
        extras = new javax.swing.JPanel();
        studentGrid = new javax.swing.JPanel();
        cellReg = new javax.swing.JPanel();
        lblReg = new javax.swing.JLabel();
        txtReg = new javax.swing.JTextField();
        cellCourse = new javax.swing.JPanel();
        lblCourse = new javax.swing.JLabel();
        txtCourse = new javax.swing.JTextField();
        cellYear = new javax.swing.JPanel();
        lblYear = new javax.swing.JLabel();
        cmbYear = new javax.swing.JComboBox<String>();
        counsellorGrid = new javax.swing.JPanel();
        cellSpeciality = new javax.swing.JPanel();
        lblSpeciality = new javax.swing.JLabel();
        txtSpeciality = new javax.swing.JTextField();
        cellQualification = new javax.swing.JPanel();
        lblQualification = new javax.swing.JLabel();
        txtQualification = new javax.swing.JTextField();
        cellGender = new javax.swing.JPanel();
        lblGender = new javax.swing.JLabel();
        cmbGender = new javax.swing.JComboBox<String>();
        cellReligion = new javax.swing.JPanel();
        lblReligion = new javax.swing.JLabel();
        txtReligion = new javax.swing.JTextField();
        footPanel = new javax.swing.JPanel();
        btnCreate = new utils.Ui.PillButton();
        lnkSignIn = new javax.swing.JLabel();

        setPreferredSize(new java.awt.Dimension(1100, 700));
        setLayout(new java.awt.BorderLayout());

        background.setLayout(new java.awt.BorderLayout());

        topPanel.setOpaque(false);
        topPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 36, 0, 20));
        topPanel.setLayout(new java.awt.BorderLayout());

        lblLogo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/usiu_logo.png"))); // NOI18N
        topPanel.add(lblLogo, java.awt.BorderLayout.WEST);

        windowButtons.setSymbolColor(new java.awt.Color(255, 255, 255));
        topPanel.add(windowButtons, java.awt.BorderLayout.EAST);

        background.add(topPanel, java.awt.BorderLayout.NORTH);

        middlePanel.setOpaque(false);

        cardPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        cardPanel.setLayout(new java.awt.BorderLayout(0, 14));

        headPanel.setOpaque(false);
        headPanel.setPreferredSize(new java.awt.Dimension(100, 116));

        tabRow.setOpaque(false);
        tabRow.setLayout(new java.awt.GridLayout(1, 2, 8, 0));

        btnTabStudent.setBackground(new java.awt.Color(232, 238, 246));
        btnTabStudent.setForeground(new java.awt.Color(12, 42, 77));
        btnTabStudent.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnTabStudent.setText("I am a student");
        btnTabStudent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTabStudentActionPerformed(evt);
            }
        });
        tabRow.add(btnTabStudent);

        btnTabCounsellor.setBackground(new java.awt.Color(232, 238, 246));
        btnTabCounsellor.setForeground(new java.awt.Color(12, 42, 77));
        btnTabCounsellor.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnTabCounsellor.setText("I am a counsellor");
        btnTabCounsellor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTabCounsellorActionPerformed(evt);
            }
        });
        tabRow.add(btnTabCounsellor);

        lblTitle.setForeground(new java.awt.Color(12, 42, 77));
        lblTitle.setFont(new java.awt.Font("Segoe UI", 1, 26)); // NOI18N
        lblTitle.setText("Create your account");

        javax.swing.GroupLayout headPanelLayout = new javax.swing.GroupLayout(headPanel);
        headPanel.setLayout(headPanelLayout);
        headPanelLayout.setHorizontalGroup(
            headPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headPanelLayout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(headPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tabRow, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTitle)))
        );
        headPanelLayout.setVerticalGroup(
            headPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headPanelLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(tabRow, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16)
                .addComponent(lblTitle))
        );

        cardPanel.add(headPanel, java.awt.BorderLayout.NORTH);

        bodyPanel.setOpaque(false);
        bodyPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 32, 0, 32));
        bodyPanel.setLayout(new java.awt.BorderLayout(0, 8));

        grid.setOpaque(false);
        grid.setLayout(new java.awt.GridLayout(0, 2, 18, 8));

        cellName.setOpaque(false);
        cellName.setLayout(new java.awt.BorderLayout(0, 3));

        lblName.setForeground(new java.awt.Color(12, 42, 77));
        lblName.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblName.setText("Full name");
        cellName.add(lblName, java.awt.BorderLayout.NORTH);
        cellName.add(txtName, java.awt.BorderLayout.CENTER);

        grid.add(cellName);

        cellEmail.setOpaque(false);
        cellEmail.setLayout(new java.awt.BorderLayout(0, 3));

        lblEmail.setForeground(new java.awt.Color(12, 42, 77));
        lblEmail.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblEmail.setText("Email");
        cellEmail.add(lblEmail, java.awt.BorderLayout.NORTH);
        cellEmail.add(txtEmail, java.awt.BorderLayout.CENTER);

        grid.add(cellEmail);

        cellPhone.setOpaque(false);
        cellPhone.setLayout(new java.awt.BorderLayout(0, 3));

        lblPhone.setForeground(new java.awt.Color(12, 42, 77));
        lblPhone.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblPhone.setText("Phone (optional)");
        cellPhone.add(lblPhone, java.awt.BorderLayout.NORTH);
        cellPhone.add(txtPhone, java.awt.BorderLayout.CENTER);

        grid.add(cellPhone);

        cellPassword.setOpaque(false);
        cellPassword.setLayout(new java.awt.BorderLayout(0, 3));

        lblPassword.setForeground(new java.awt.Color(12, 42, 77));
        lblPassword.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblPassword.setText("Password (min. 6 characters)");
        cellPassword.add(lblPassword, java.awt.BorderLayout.NORTH);
        cellPassword.add(txtPassword, java.awt.BorderLayout.CENTER);

        grid.add(cellPassword);

        cellConfirm.setOpaque(false);
        cellConfirm.setLayout(new java.awt.BorderLayout(0, 3));

        lblConfirm.setForeground(new java.awt.Color(12, 42, 77));
        lblConfirm.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblConfirm.setText("Repeat password");
        cellConfirm.add(lblConfirm, java.awt.BorderLayout.NORTH);
        cellConfirm.add(txtConfirm, java.awt.BorderLayout.CENTER);

        grid.add(cellConfirm);

        spacer.setOpaque(false);
        spacer.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        grid.add(spacer);

        bodyPanel.add(grid, java.awt.BorderLayout.NORTH);

        extrasTop.setOpaque(false);
        extrasTop.setLayout(new java.awt.BorderLayout());

        extras.setOpaque(false);
        extras.setLayout(new java.awt.CardLayout());

        studentGrid.setOpaque(false);
        studentGrid.setLayout(new java.awt.GridLayout(0, 2, 18, 8));

        cellReg.setOpaque(false);
        cellReg.setLayout(new java.awt.BorderLayout(0, 3));

        lblReg.setForeground(new java.awt.Color(12, 42, 77));
        lblReg.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblReg.setText("Registration number");
        cellReg.add(lblReg, java.awt.BorderLayout.NORTH);
        cellReg.add(txtReg, java.awt.BorderLayout.CENTER);

        studentGrid.add(cellReg);

        cellCourse.setOpaque(false);
        cellCourse.setLayout(new java.awt.BorderLayout(0, 3));

        lblCourse.setForeground(new java.awt.Color(12, 42, 77));
        lblCourse.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblCourse.setText("Course");
        cellCourse.add(lblCourse, java.awt.BorderLayout.NORTH);
        cellCourse.add(txtCourse, java.awt.BorderLayout.CENTER);

        studentGrid.add(cellCourse);

        cellYear.setOpaque(false);
        cellYear.setLayout(new java.awt.BorderLayout(0, 3));

        lblYear.setForeground(new java.awt.Color(12, 42, 77));
        lblYear.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblYear.setText("Year of study");
        cellYear.add(lblYear, java.awt.BorderLayout.NORTH);
        cmbYear.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "1", "2", "3", "4", "5", "6" }));
        cellYear.add(cmbYear, java.awt.BorderLayout.CENTER);

        studentGrid.add(cellYear);

        extras.add(studentGrid, "STUDENT");

        counsellorGrid.setOpaque(false);
        counsellorGrid.setLayout(new java.awt.GridLayout(0, 2, 18, 8));

        cellSpeciality.setOpaque(false);
        cellSpeciality.setLayout(new java.awt.BorderLayout(0, 3));

        lblSpeciality.setForeground(new java.awt.Color(12, 42, 77));
        lblSpeciality.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblSpeciality.setText("Specialization");
        cellSpeciality.add(lblSpeciality, java.awt.BorderLayout.NORTH);
        cellSpeciality.add(txtSpeciality, java.awt.BorderLayout.CENTER);

        counsellorGrid.add(cellSpeciality);

        cellQualification.setOpaque(false);
        cellQualification.setLayout(new java.awt.BorderLayout(0, 3));

        lblQualification.setForeground(new java.awt.Color(12, 42, 77));
        lblQualification.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblQualification.setText("Qualification");
        cellQualification.add(lblQualification, java.awt.BorderLayout.NORTH);
        cellQualification.add(txtQualification, java.awt.BorderLayout.CENTER);

        counsellorGrid.add(cellQualification);

        cellGender.setOpaque(false);
        cellGender.setLayout(new java.awt.BorderLayout(0, 3));

        lblGender.setForeground(new java.awt.Color(12, 42, 77));
        lblGender.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblGender.setText("Gender");
        cellGender.add(lblGender, java.awt.BorderLayout.NORTH);
        cmbGender.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "Male", "Female", "Other" }));
        cellGender.add(cmbGender, java.awt.BorderLayout.CENTER);

        counsellorGrid.add(cellGender);

        cellReligion.setOpaque(false);
        cellReligion.setLayout(new java.awt.BorderLayout(0, 3));

        lblReligion.setForeground(new java.awt.Color(12, 42, 77));
        lblReligion.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblReligion.setText("Religion");
        cellReligion.add(lblReligion, java.awt.BorderLayout.NORTH);
        cellReligion.add(txtReligion, java.awt.BorderLayout.CENTER);

        counsellorGrid.add(cellReligion);

        extras.add(counsellorGrid, "COUNSELLOR");

        extrasTop.add(extras, java.awt.BorderLayout.NORTH);

        bodyPanel.add(extrasTop, java.awt.BorderLayout.CENTER);

        cardPanel.add(bodyPanel, java.awt.BorderLayout.CENTER);

        footPanel.setOpaque(false);
        footPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 32, 26, 32));
        footPanel.setPreferredSize(new java.awt.Dimension(100, 70));
        footPanel.setLayout(new java.awt.BorderLayout());

        btnCreate.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnCreate.setText("Create account");
        btnCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateActionPerformed(evt);
            }
        });
        footPanel.add(btnCreate, java.awt.BorderLayout.WEST);

        lnkSignIn.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lnkSignIn.setForeground(new java.awt.Color(26, 75, 132));
        lnkSignIn.setText("Already have an account?  Sign in");
        lnkSignIn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lnkSignInMouseClicked(evt);
            }
        });
        footPanel.add(lnkSignIn, java.awt.BorderLayout.EAST);

        cardPanel.add(footPanel, java.awt.BorderLayout.SOUTH);

        javax.swing.GroupLayout middlePanelLayout = new javax.swing.GroupLayout(middlePanel);
        middlePanel.setLayout(middlePanelLayout);
        middlePanelLayout.setHorizontalGroup(
            middlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(middlePanelLayout.createSequentialGroup()
                .addContainerGap(37, Short.MAX_VALUE)
                .addComponent(cardPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 700, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(37, Short.MAX_VALUE))
        );
        middlePanelLayout.setVerticalGroup(
            middlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(middlePanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cardPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 560, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        background.add(middlePanel, java.awt.BorderLayout.CENTER);

        add(background, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btnTabStudentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTabStudentActionPerformed
        selectRole(User.STUDENT);
    }//GEN-LAST:event_btnTabStudentActionPerformed

    private void btnTabCounsellorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTabCounsellorActionPerformed
        selectRole(User.COUNSELLOR);
    }//GEN-LAST:event_btnTabCounsellorActionPerformed

    private void btnCreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateActionPerformed
        register();
    }//GEN-LAST:event_btnCreateActionPerformed

    private void lnkSignInMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lnkSignInMouseClicked
        goTo("LOGIN");
    }//GEN-LAST:event_lnkSignInMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private utils.AnimatedBackground background;
    private javax.swing.JPanel bodyPanel;
    private utils.Ui.PillButton btnCreate;
    private utils.Ui.PillButton btnTabCounsellor;
    private utils.Ui.PillButton btnTabStudent;
    private utils.Ui.RoundPanel cardPanel;
    private javax.swing.JPanel cellConfirm;
    private javax.swing.JPanel cellCourse;
    private javax.swing.JPanel cellEmail;
    private javax.swing.JPanel cellGender;
    private javax.swing.JPanel cellName;
    private javax.swing.JPanel cellPassword;
    private javax.swing.JPanel cellPhone;
    private javax.swing.JPanel cellQualification;
    private javax.swing.JPanel cellReg;
    private javax.swing.JPanel cellReligion;
    private javax.swing.JPanel cellSpeciality;
    private javax.swing.JPanel cellYear;
    private javax.swing.JComboBox<String> cmbGender;
    private javax.swing.JComboBox<String> cmbYear;
    private javax.swing.JPanel counsellorGrid;
    private javax.swing.JPanel extras;
    private javax.swing.JPanel extrasTop;
    private javax.swing.JPanel footPanel;
    private javax.swing.JPanel grid;
    private javax.swing.JPanel headPanel;
    private javax.swing.JLabel lblConfirm;
    private javax.swing.JLabel lblCourse;
    private javax.swing.JLabel lblEmail;
    private javax.swing.JLabel lblGender;
    private javax.swing.JLabel lblLogo;
    private javax.swing.JLabel lblName;
    private javax.swing.JLabel lblPassword;
    private javax.swing.JLabel lblPhone;
    private javax.swing.JLabel lblQualification;
    private javax.swing.JLabel lblReg;
    private javax.swing.JLabel lblReligion;
    private javax.swing.JLabel lblSpeciality;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblYear;
    private javax.swing.JLabel lnkSignIn;
    private javax.swing.JPanel middlePanel;
    private javax.swing.JPanel spacer;
    private javax.swing.JPanel studentGrid;
    private javax.swing.JPanel tabRow;
    private javax.swing.JPanel topPanel;
    private javax.swing.JPasswordField txtConfirm;
    private javax.swing.JTextField txtCourse;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtName;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtPhone;
    private javax.swing.JTextField txtQualification;
    private javax.swing.JTextField txtReg;
    private javax.swing.JTextField txtReligion;
    private javax.swing.JTextField txtSpeciality;
    private utils.WindowButtons windowButtons;
    // End of variables declaration//GEN-END:variables
}
