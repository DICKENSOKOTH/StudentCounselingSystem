package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import models.Appointment;

// appointments + the booking rules
public class AppointmentDAO implements CrudDAO<Appointment> {

    // working hours: Monday to Friday, one-hour slots from 08:00 to 16:00
    public static final LocalTime[] SLOTS = {
        LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0),
        LocalTime.of(13, 0), LocalTime.of(14, 0), LocalTime.of(15, 0), LocalTime.of(16, 0)
    };

    private static final String SELECT = "SELECT a.appointment_id, a.student_id, a.counsellor_id, a.appointment_date, a.appointment_time, "
            + "a.reason, a.status, su.full_name AS student_name, st.registration_number, cu.full_name AS counsellor_name, f.rating "
            + "FROM appointments a "
            + "JOIN students st ON st.student_id = a.student_id JOIN users su ON su.user_id = st.user_id "
            + "JOIN counsellors c ON c.counsellor_id = a.counsellor_id JOIN users cu ON cu.user_id = c.user_id "
            + "LEFT JOIN feedback f ON f.appointment_id = a.appointment_id ";
    private static final String ORDER = " ORDER BY a.appointment_date DESC, a.appointment_time DESC";

    private Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentId(rs.getInt("appointment_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setCounsellorId(rs.getInt("counsellor_id"));
        a.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
        a.setAppointmentTime(rs.getTime("appointment_time").toLocalTime());
        a.setReason(rs.getString("reason"));
        a.setStatus(rs.getString("status"));
        a.setStudentName(rs.getString("student_name"));
        a.setRegistrationNumber(rs.getString("registration_number"));
        a.setCounsellorName(rs.getString("counsellor_name"));
        int rating = rs.getInt("rating");
        a.setRating(rs.wasNull() ? null : rating);
        return a;
    }

    private List<Appointment> query(String where, Object... params) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + where + ORDER)) {
            for (int i = 0; i < params.length; i++) { ps.setObject(i + 1, params[i]); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { list.add(map(rs)); }
            }
        }
        return list;
    }

    //booking rules 

    // is this slot already held by an open booking of the counsellor? (ignore one appointment when rescheduling)
    private boolean counsellorBusy(Connection con, int counsellorId, LocalDate d, LocalTime t, int ignoreId) throws SQLException {
        return count(con, "counsellor_id", counsellorId, d, t, ignoreId) > 0;
    }

    private boolean studentBusy(Connection con, int studentId, LocalDate d, LocalTime t, int ignoreId) throws SQLException {
        return count(con, "student_id", studentId, d, t, ignoreId) > 0;
    }

    private int count(Connection con, String column, int id, LocalDate d, LocalTime t, int ignoreId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE " + column + " = ? AND appointment_date = ? AND appointment_time = ? "
                + "AND status IN ('PENDING','APPROVED') AND appointment_id <> ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setDate(2, Date.valueOf(d));
            ps.setTime(3, Time.valueOf(t));
            ps.setInt(4, ignoreId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    // throws IllegalArgumentException with a message the user can read
    private void validate(Connection con, int studentId, int counsellorId, LocalDate d, LocalTime t, int ignoreId) throws SQLException {
        if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("The counselling centre is open Monday to Friday only.");
        }
        boolean validSlot = false;
        for (LocalTime s : SLOTS) { if (s.equals(t)) { validSlot = true; } }
        if (!validSlot) { throw new IllegalArgumentException("Choose a time between 08:00 and 16:00."); }
        if (!d.atTime(t).isAfter(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Choose a date and time in the future.");
        }
        if (counsellorBusy(con, counsellorId, d, t, ignoreId)) {
            throw new IllegalArgumentException("That time is already booked with this counsellor.");
        }
        if (studentBusy(con, studentId, d, t, ignoreId)) {
            throw new IllegalArgumentException("You already have an appointment at that time.");
        }
    }

    // only these status changes are allowed: PENDING -> APPROVED/CANCELLED, APPROVED -> COMPLETED/CANCELLED
    public static boolean canMove(String from, String to) {
        if (from.equals(to)) { return true; }
        if (Appointment.PENDING.equals(from)) { return Appointment.APPROVED.equals(to) || Appointment.CANCELLED.equals(to); }
        if (Appointment.APPROVED.equals(from)) { return Appointment.COMPLETED.equals(to) || Appointment.CANCELLED.equals(to); }
        return false;
    }

    // Read: the free hours of one counsellor on one day
    public List<LocalTime> freeSlots(int counsellorId, LocalDate date) throws SQLException {
        List<LocalTime> free = new ArrayList<>();
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) { return free; }
        Set<LocalTime> taken = new HashSet<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT appointment_time FROM appointments WHERE counsellor_id = ? "
                     + "AND appointment_date = ? AND status IN ('PENDING','APPROVED')")) {
            ps.setInt(1, counsellorId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { taken.add(rs.getTime(1).toLocalTime()); }
            }
        }
        for (LocalTime s : SLOTS) {
            if (!taken.contains(s) && date.atTime(s).isAfter(java.time.LocalDateTime.now())) { free.add(s); }
        }
        return free;
    }

    //CRUD 

    // Create: book an appointment (starts as PENDING)
    @Override
    public boolean create(Appointment a) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            validate(con, a.getStudentId(), a.getCounsellorId(), a.getAppointmentDate(), a.getAppointmentTime(), 0);
            String sql = "INSERT INTO appointments (student_id, counsellor_id, appointment_date, appointment_time, reason, status) VALUES (?, ?, ?, ?, ?, 'PENDING')";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, a.getStudentId());
                ps.setInt(2, a.getCounsellorId());
                ps.setDate(3, Date.valueOf(a.getAppointmentDate()));
                ps.setTime(4, Time.valueOf(a.getAppointmentTime()));
                ps.setString(5, a.getReason());
                boolean ok = ps.executeUpdate() > 0;
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) { a.setAppointmentId(keys.getInt(1)); } }
                a.setStatus(Appointment.PENDING);
                return ok;
            }
        }
    }

    // Read: one / all / by student / by counsellor
    @Override
    public Appointment findById(int id) throws SQLException {
        List<Appointment> list = query("WHERE a.appointment_id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Appointment> findAll() throws SQLException { return query(""); }
    public List<Appointment> findByStudent(int studentId) throws SQLException { return query("WHERE a.student_id = ?", studentId); }
    public List<Appointment> findByCounsellor(int counsellorId) throws SQLException { return query("WHERE a.counsellor_id = ?", counsellorId); }

    // Update: reason, date, time and status (used by the admin)
    @Override
    public boolean update(Appointment a) throws SQLException {
        Appointment old = findById(a.getAppointmentId());
        if (old == null) { return false; }
        if (!canMove(old.getStatus(), a.getStatus())) {
            throw new IllegalArgumentException("A " + old.getStatus() + " appointment cannot become " + a.getStatus() + ".");
        }
        try (Connection con = DBConnection.getConnection()) {
            boolean moved = !old.getAppointmentDate().equals(a.getAppointmentDate()) || !old.getAppointmentTime().equals(a.getAppointmentTime());
            if (moved && a.isOpen()) {
                validate(con, a.getStudentId(), a.getCounsellorId(), a.getAppointmentDate(), a.getAppointmentTime(), a.getAppointmentId());
            }
            String sql = "UPDATE appointments SET appointment_date = ?, appointment_time = ?, reason = ?, status = ? WHERE appointment_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(a.getAppointmentDate()));
                ps.setTime(2, Time.valueOf(a.getAppointmentTime()));
                ps.setString(3, a.getReason());
                ps.setString(4, a.getStatus());
                ps.setInt(5, a.getAppointmentId());
                return ps.executeUpdate() > 0;
            }
        }
    }

    // Update: move to a new free slot (needs the counsellor to approve again)
    public boolean reschedule(Appointment a, LocalDate date, LocalTime time) throws SQLException {
        if (!a.isOpen()) { throw new IllegalArgumentException("Only pending or approved appointments can be rescheduled."); }
        try (Connection con = DBConnection.getConnection()) {
            validate(con, a.getStudentId(), a.getCounsellorId(), date, time, a.getAppointmentId());
            try (PreparedStatement ps = con.prepareStatement("UPDATE appointments SET appointment_date = ?, appointment_time = ?, status = 'PENDING' WHERE appointment_id = ?")) {
                ps.setDate(1, Date.valueOf(date));
                ps.setTime(2, Time.valueOf(time));
                ps.setInt(3, a.getAppointmentId());
                return ps.executeUpdate() > 0;
            }
        }
    }

    // Update: change only the status (approve, decline, cancel, complete)
    public boolean changeStatus(Appointment a, String newStatus) throws SQLException {
        if (!canMove(a.getStatus(), newStatus)) {
            throw new IllegalArgumentException("A " + a.getStatus() + " appointment cannot become " + newStatus + ".");
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE appointments SET status = ? WHERE appointment_id = ?")) {
            ps.setString(1, newStatus);
            ps.setInt(2, a.getAppointmentId());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) { a.setStatus(newStatus); }
            return ok;
        }
    }

    // Delete: the session and feedback of this appointment go with it (ON DELETE CASCADE)
    @Override
    public boolean delete(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM appointments WHERE appointment_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
