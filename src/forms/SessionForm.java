package forms;

import dao.SessionDAO;
import java.awt.Component;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import models.Appointment;
import models.CounsellingSession;
import utils.Ui;

// the counsellor's notes for one session: write, edit and delete
public final class SessionForm {

    private SessionForm() { }

    // returns true when something changed (saved or deleted)
    public static boolean show(Component parent, Appointment a, CounsellingSession existing) {
        boolean isNew = existing == null;
        CounsellingSession s = isNew ? new CounsellingSession() : existing;
        s.setAppointmentId(a.getAppointmentId());

        JTextArea concern = Ui.area(2, 30), goals = Ui.area(2, 30), notes = Ui.area(4, 30), plan = Ui.area(2, 30);
        concern.setText(nz(s.getPresentingConcern())); goals.setText(nz(s.getCounsellingGoals()));
        notes.setText(nz(s.getSessionNotes())); plan.setText(nz(s.getActionPlan()));
        JSpinner minutes = new JSpinner(new SpinnerNumberModel(s.getDurationMinutes() == 0 ? 45 : s.getDurationMinutes(), 5, 240, 5));
        JTextField followUp = Ui.field(10);
        followUp.setText(s.getFollowUpDate() == null ? "" : s.getFollowUpDate().toString());
        followUp.setToolTipText("yyyy-mm-dd, leave empty when no follow-up is needed");

        Ui.Form form = new Ui.Form();
        form.add("Student", Ui.label(a.getStudentName() + "  (" + a.getAppointmentDate().format(Ui.DATE) + ")", java.awt.Font.BOLD, 14f, utils.Theme.NAVY));
        form.add("Presenting concern", Ui.scrolled(concern));
        form.add("Counselling goals", Ui.scrolled(goals));
        form.add("Session notes", Ui.scrolled(notes));
        form.add("Action plan", Ui.scrolled(plan));
        form.add("Duration (minutes)", minutes);
        form.add("Follow-up date", followUp);

        // save, delete (only for existing notes) or cancel
        java.util.function.Supplier<String> save = () -> {
            if (utils.Validate.blank(notes.getText())) { return "Write the session notes."; }
            LocalDate follow = null;
            if (!followUp.getText().trim().isEmpty()) {
                try { follow = LocalDate.parse(followUp.getText().trim()); }
                catch (DateTimeParseException ex) { return "Write the follow-up date like 2026-12-01, or leave it empty."; }
            }
            s.setPresentingConcern(concern.getText().trim());
            s.setCounsellingGoals(goals.getText().trim());
            s.setSessionNotes(notes.getText().trim());
            s.setActionPlan(plan.getText().trim());
            s.setDurationMinutes((Integer) minutes.getValue());
            s.setFollowUpDate(follow);
            try {
                if (isNew) { new SessionDAO().create(s); } else { new SessionDAO().update(s); }
                return null;
            } catch (SQLException ex) {
                return Ui.friendly(ex);
            } catch (IllegalArgumentException ex) {
                return ex.getMessage();
            }
        };
        Object[] options = isNew ? new Object[]{"Save session", "Cancel"} : new Object[]{"Save changes", "Delete notes", "Cancel"};
        while (true) {
            int choice = JOptionPane.showOptionDialog(parent, form, isNew ? "Record session" : "Session notes", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                String problem = save.get();
                if (problem == null) { return true; }
                Ui.error(parent, problem);
            } else if (!isNew && choice == 1) {
                if (Ui.confirm(parent, "Delete the session notes of " + a.getStudentName() + "?")) {
                    try { new SessionDAO().delete(s.getSessionId()); return true; }
                    catch (SQLException ex) { Ui.error(parent, Ui.friendly(ex)); }
                }
            } else {
                return false;
            }
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
