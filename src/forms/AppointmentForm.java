package forms;

import dao.AppointmentDAO;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.sql.SQLException;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import models.Appointment;
import utils.Ui;

// pop-up forms that change an existing appointment
public final class AppointmentForm {

    private AppointmentForm() { }

    // student: move to another free hour (needs approval again)
    public static boolean reschedule(Component parent, Appointment a) {
        SlotPicker picker = new SlotPicker();
        picker.setCounsellor(a.getCounsellorId());
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(Ui.label("Choose a new day and time with " + a.getCounsellorName(), java.awt.Font.BOLD, 14f, utils.Theme.NAVY), BorderLayout.NORTH);
        body.add(picker, BorderLayout.CENTER);
        body.setPreferredSize(new Dimension(360, 150));
        return Ui.ask(parent, "Reschedule appointment", body, () -> {
            if (picker.getTime() == null) { return "Choose a day that has a free hour."; }
            try {
                new AppointmentDAO().reschedule(a, picker.getDate(), picker.getTime());
                return null;
            } catch (SQLException ex) {
                return Ui.friendly(ex);
            } catch (IllegalArgumentException ex) {
                return ex.getMessage();
            }
        });
    }

    // admin: change time, reason and status
    public static boolean adminEdit(Component parent, Appointment a) {
        SlotPicker picker = new SlotPicker();
        picker.setCounsellor(a.getCounsellorId());
        JComboBox<String> status = Ui.combo(new String[]{Appointment.PENDING, Appointment.APPROVED, Appointment.COMPLETED, Appointment.CANCELLED});
        status.setSelectedItem(a.getStatus());
        JTextArea reason = Ui.area(3, 24);
        reason.setText(a.getReason() == null ? "" : a.getReason());

        Ui.Form form = new Ui.Form();
        form.add("Student", Ui.label(a.getStudentName(), java.awt.Font.PLAIN, 14f, utils.Theme.NAVY));
        form.add("Counsellor", Ui.label(a.getCounsellorName(), java.awt.Font.PLAIN, 14f, utils.Theme.NAVY));
        form.add("Now", Ui.label(a.getAppointmentDate().format(Ui.DATE) + " at " + a.getAppointmentTime().format(Ui.TIME), java.awt.Font.PLAIN, 14f, utils.Theme.INK_MUTED));
        form.add("New time", picker);
        form.add("Status", status);
        form.add("Reason", Ui.scrolled(reason));
        form.setPreferredSize(new Dimension(420, 330));

        return Ui.ask(parent, "Edit appointment", form, () -> {
            Appointment copy = new Appointment();
            copy.setAppointmentId(a.getAppointmentId());
            copy.setStudentId(a.getStudentId());
            copy.setCounsellorId(a.getCounsellorId());
            copy.setReason(reason.getText().trim());
            copy.setStatus((String) status.getSelectedItem());
            // keep the old time when the admin did not pick a new free hour
            copy.setAppointmentDate(picker.getTime() == null ? a.getAppointmentDate() : picker.getDate());
            copy.setAppointmentTime(picker.getTime() == null ? a.getAppointmentTime() : picker.getTime());
            try {
                new AppointmentDAO().update(copy);
                return null;
            } catch (SQLException ex) {
                return Ui.friendly(ex);
            } catch (IllegalArgumentException ex) {
                return ex.getMessage();
            }
        });
    }
}
