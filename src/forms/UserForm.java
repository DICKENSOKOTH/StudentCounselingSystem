package forms;

import dao.CounsellorDAO;
import dao.StudentDAO;
import dao.UserDAO;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.SQLException;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import models.Counsellor;
import models.Student;
import models.User;
import utils.PasswordUtil;
import utils.Theme;
import utils.Ui;
import utils.Validate;

// admin: add a user or edit one (the extra fields depend on the role)
public final class UserForm {

    private UserForm() { }

    public static boolean create(Component parent) { return show(parent, null); }
    public static boolean edit(Component parent, User user) { return show(parent, user); }

    private static boolean show(Component parent, User existing) {
        boolean isNew = existing == null;
        JTextField name = Ui.field(20), email = Ui.field(20), phone = Ui.field(20);
        JPasswordField password = Ui.password(20);
        JComboBox<String> role = Ui.combo(new String[]{User.STUDENT, User.COUNSELLOR, User.ADMIN});

        // student extras
        JTextField reg = Ui.field(20), course = Ui.field(20);
        JComboBox<Integer> year = Ui.combo(new Integer[]{1, 2, 3, 4, 5, 6});
        Ui.Form studentForm = new Ui.Form();
        studentForm.add("Registration no.", reg);
        studentForm.add("Course", course);
        studentForm.add("Year of study", year);
        // counsellor extras
        JTextField speciality = Ui.field(20), qualification = Ui.field(20), religion = Ui.field(20);
        JComboBox<String> gender = Ui.combo(new String[]{"Female", "Male", "Other"});
        Ui.Form counsellorForm = new Ui.Form();
        counsellorForm.add("Specialization", speciality);
        counsellorForm.add("Qualification", qualification);
        counsellorForm.add("Gender", gender);
        counsellorForm.add("Religion", religion);

        CardLayout cards = new CardLayout();
        JPanel extras = new JPanel(cards);
        extras.setOpaque(false);
        extras.add(studentForm, User.STUDENT);
        extras.add(counsellorForm, User.COUNSELLOR);
        extras.add(new JPanel(), User.ADMIN);
        role.addActionListener(e -> cards.show(extras, (String) role.getSelectedItem()));

        Student student = null;
        Counsellor counsellor = null;
        try {
            if (!isNew) {
                name.setText(existing.getFullName());
                email.setText(existing.getEmail());
                phone.setText(existing.getPhone() == null ? "" : existing.getPhone());
                role.setSelectedItem(existing.getRole());
                role.setEnabled(false);   // the role cannot change after the account exists
                if (existing.isStudent()) {
                    student = new StudentDAO().findByUserId(existing.getUserId());
                    reg.setText(student.getRegistrationNumber());
                    course.setText(student.getCourse());
                    year.setSelectedItem(student.getYearOfStudy());
                } else if (existing.isCounsellor()) {
                    counsellor = new CounsellorDAO().findByUserId(existing.getUserId());
                    speciality.setText(counsellor.getSpecialization());
                    qualification.setText(counsellor.getQualification());
                    gender.setSelectedItem(counsellor.getGender());
                    religion.setText(counsellor.getReligion());
                }
            }
        } catch (SQLException ex) {
            Ui.error(parent, Ui.friendly(ex));
            return false;
        }
        cards.show(extras, (String) role.getSelectedItem());

        Ui.Form form = new Ui.Form();
        form.add("Role", role);
        form.add("Full name", name);
        form.add("Email", email);
        form.add("Phone", phone);
        if (isNew) { form.add("Password", password); }
        JPanel body = new JPanel(new java.awt.BorderLayout(0, 8));
        body.add(form, java.awt.BorderLayout.NORTH);
        body.add(extras, java.awt.BorderLayout.CENTER);
        body.setPreferredSize(new Dimension(430, 400));

        final Student editStudent = student;
        final Counsellor editCounsellor = counsellor;
        return Ui.ask(parent, isNew ? "Add user" : "Edit user", body, () -> {
            String selected = (String) role.getSelectedItem();
            if (Validate.blank(name.getText())) { return "Enter the full name."; }
            if (!Validate.email(email.getText())) { return "Enter a valid email address."; }
            if (!Validate.phone(phone.getText())) { return "Enter a valid phone number (or leave it empty)."; }
            if (isNew && Validate.password(new String(password.getPassword())) != null) { return Validate.password(new String(password.getPassword())); }
            if (User.STUDENT.equals(selected) && (Validate.blank(reg.getText()) || Validate.blank(course.getText()))) { return "Enter the registration number and course."; }
            try {
                User u = isNew ? new User() : existing;
                u.setFullName(name.getText().trim());
                u.setEmail(email.getText().trim());
                u.setPhone(phone.getText().trim());
                UserDAO users = new UserDAO();
                if (isNew) {
                    u.setPasswordHash(PasswordUtil.hash(new String(password.getPassword())));
                    u.setRole(selected);
                    if (User.STUDENT.equals(selected)) {
                        Student s = new Student();
                        s.setRegistrationNumber(reg.getText().trim());
                        s.setCourse(course.getText().trim());
                        s.setYearOfStudy((Integer) year.getSelectedItem());
                        users.createStudent(u, s);
                    } else if (User.COUNSELLOR.equals(selected)) {
                        Counsellor c = new Counsellor();
                        c.setSpecialization(speciality.getText().trim());
                        c.setQualification(qualification.getText().trim());
                        c.setGender((String) gender.getSelectedItem());
                        c.setReligion(religion.getText().trim());
                        users.createCounsellor(u, c);
                    } else {
                        users.create(u);
                    }
                } else {
                    users.update(u);
                    if (editStudent != null) {
                        editStudent.setFullName(u.getFullName());
                        editStudent.setPhone(u.getPhone());
                        editStudent.setRegistrationNumber(reg.getText().trim());
                        editStudent.setCourse(course.getText().trim());
                        editStudent.setYearOfStudy((Integer) year.getSelectedItem());
                        new StudentDAO().update(editStudent);
                    } else if (editCounsellor != null) {
                        editCounsellor.setFullName(u.getFullName());
                        editCounsellor.setPhone(u.getPhone());
                        editCounsellor.setSpecialization(speciality.getText().trim());
                        editCounsellor.setQualification(qualification.getText().trim());
                        editCounsellor.setGender((String) gender.getSelectedItem());
                        editCounsellor.setReligion(religion.getText().trim());
                        new CounsellorDAO().update(editCounsellor);
                    }
                }
                return null;
            } catch (SQLException ex) {
                return Ui.friendly(ex);
            }
        });
    }
}
