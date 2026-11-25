package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import models.Feedback;

// student feedback on completed appointments
public class FeedbackDAO implements CrudDAO<Feedback> {

    private static final String SELECT = "SELECT f.feedback_id, f.appointment_id, f.rating, f.comments, f.created_at, "
            + "su.full_name AS student_name, cu.full_name AS counsellor_name, a.appointment_date, a.counsellor_id "
            + "FROM feedback f JOIN appointments a ON a.appointment_id = f.appointment_id "
            + "JOIN students st ON st.student_id = a.student_id JOIN users su ON su.user_id = st.user_id "
            + "JOIN counsellors c ON c.counsellor_id = a.counsellor_id JOIN users cu ON cu.user_id = c.user_id ";

    private Feedback map(ResultSet rs) throws SQLException {
        Feedback f = new Feedback();
        f.setFeedbackId(rs.getInt("feedback_id"));
        f.setAppointmentId(rs.getInt("appointment_id"));
        f.setRating(rs.getInt("rating"));
        f.setComments(rs.getString("comments"));
        f.setCreatedAt(rs.getTimestamp("created_at"));
        f.setStudentName(rs.getString("student_name"));
        f.setCounsellorName(rs.getString("counsellor_name"));
        f.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
        return f;
    }

    private List<Feedback> query(String where, Object... params) throws SQLException {
        List<Feedback> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + where + " ORDER BY f.created_at DESC")) {
            for (int i = 0; i < params.length; i++) { ps.setObject(i + 1, params[i]); }
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { list.add(map(rs)); } }
        }
        return list;
    }

    // Create: only for a completed appointment, one per appointment
    @Override
    public boolean create(Feedback f) throws SQLException {
        if (f.getRating() < 1 || f.getRating() > 5) { throw new IllegalArgumentException("Choose a rating from 1 to 5."); }
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT status FROM appointments WHERE appointment_id = ?")) {
                ps.setInt(1, f.getAppointmentId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !"COMPLETED".equals(rs.getString(1))) {
                        throw new IllegalArgumentException("Feedback is only possible after the session is completed.");
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO feedback (appointment_id, rating, comments) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, f.getAppointmentId());
                ps.setInt(2, f.getRating());
                ps.setString(3, f.getComments());
                boolean ok = ps.executeUpdate() > 0;
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) { f.setFeedbackId(keys.getInt(1)); } }
                return ok;
            }
        }
    }

    @Override
    public Feedback findById(int id) throws SQLException {
        List<Feedback> list = query("WHERE f.feedback_id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Feedback findByAppointment(int appointmentId) throws SQLException {
        List<Feedback> list = query("WHERE f.appointment_id = ?", appointmentId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Feedback> findAll() throws SQLException { return query(""); }
    public List<Feedback> findByCounsellor(int counsellorId) throws SQLException { return query("WHERE a.counsellor_id = ?", counsellorId); }

    @Override
    public boolean update(Feedback f) throws SQLException {
        if (f.getRating() < 1 || f.getRating() > 5) { throw new IllegalArgumentException("Choose a rating from 1 to 5."); }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE feedback SET rating = ?, comments = ? WHERE feedback_id = ?")) {
            ps.setInt(1, f.getRating());
            ps.setString(2, f.getComments());
            ps.setInt(3, f.getFeedbackId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM feedback WHERE feedback_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
