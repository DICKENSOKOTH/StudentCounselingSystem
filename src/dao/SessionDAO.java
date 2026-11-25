package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import models.CounsellingSession;

// session notes written by the counsellor
public class SessionDAO implements CrudDAO<CounsellingSession> {

    private static final String COLS = "session_id, appointment_id, presenting_concern, counselling_goals, session_notes, action_plan, duration_minutes, follow_up_date";

    private CounsellingSession map(ResultSet rs) throws SQLException {
        CounsellingSession s = new CounsellingSession();
        s.setSessionId(rs.getInt("session_id"));
        s.setAppointmentId(rs.getInt("appointment_id"));
        s.setPresentingConcern(rs.getString("presenting_concern"));
        s.setCounsellingGoals(rs.getString("counselling_goals"));
        s.setSessionNotes(rs.getString("session_notes"));
        s.setActionPlan(rs.getString("action_plan"));
        s.setDurationMinutes(rs.getInt("duration_minutes"));
        Date follow = rs.getDate("follow_up_date");
        s.setFollowUpDate(follow == null ? null : follow.toLocalDate());
        return s;
    }

    // Create: save the notes and complete the appointment in one transaction
    @Override
    public boolean create(CounsellingSession s) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                String sql = "INSERT INTO counselling_sessions (appointment_id, presenting_concern, counselling_goals, session_notes, action_plan, duration_minutes, follow_up_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, s);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) { s.setSessionId(keys.getInt(1)); } }
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE appointments SET status = 'COMPLETED' WHERE appointment_id = ? AND status = 'APPROVED'")) {
                    ps.setInt(1, s.getAppointmentId());
                    if (ps.executeUpdate() == 0) { throw new IllegalArgumentException("Only an approved appointment can be completed."); }
                }
                con.commit();
                return true;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    private void bind(PreparedStatement ps, CounsellingSession s) throws SQLException {
        ps.setInt(1, s.getAppointmentId());
        ps.setString(2, s.getPresentingConcern());
        ps.setString(3, s.getCounsellingGoals());
        ps.setString(4, s.getSessionNotes());
        ps.setString(5, s.getActionPlan());
        ps.setInt(6, s.getDurationMinutes());
        ps.setDate(7, s.getFollowUpDate() == null ? null : Date.valueOf(s.getFollowUpDate()));
    }

    @Override
    public CounsellingSession findById(int id) throws SQLException { return one("WHERE session_id = ?", id); }

    // Read: the notes of one appointment (null when none yet)
    public CounsellingSession findByAppointment(int appointmentId) throws SQLException { return one("WHERE appointment_id = ?", appointmentId); }

    private CounsellingSession one(String where, int id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT " + COLS + " FROM counselling_sessions " + where)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    @Override
    public List<CounsellingSession> findAll() throws SQLException {
        List<CounsellingSession> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT " + COLS + " FROM counselling_sessions ORDER BY session_date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { list.add(map(rs)); }
        }
        return list;
    }

    @Override
    public boolean update(CounsellingSession s) throws SQLException {
        String sql = "UPDATE counselling_sessions SET presenting_concern = ?, counselling_goals = ?, session_notes = ?, action_plan = ?, duration_minutes = ?, follow_up_date = ? WHERE session_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getPresentingConcern());
            ps.setString(2, s.getCounsellingGoals());
            ps.setString(3, s.getSessionNotes());
            ps.setString(4, s.getActionPlan());
            ps.setInt(5, s.getDurationMinutes());
            ps.setDate(6, s.getFollowUpDate() == null ? null : Date.valueOf(s.getFollowUpDate()));
            ps.setInt(7, s.getSessionId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM counselling_sessions WHERE session_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
