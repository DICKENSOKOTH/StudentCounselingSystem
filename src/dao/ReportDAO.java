package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// numbers for the admin overview and the charts
public class ReportDAO {

    private int number(String sql) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countStudents() throws SQLException { return number("SELECT COUNT(*) FROM students"); }
    public int countCounsellors() throws SQLException { return number("SELECT COUNT(*) FROM counsellors"); }
    public int countAppointments() throws SQLException { return number("SELECT COUNT(*) FROM appointments"); }

    // appointments per status (all four statuses are always present)
    public Map<String, Integer> appointmentsByStatus() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String s : new String[]{"PENDING", "APPROVED", "COMPLETED", "CANCELLED"}) { map.put(s, 0); }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT status, COUNT(*) FROM appointments GROUP BY status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { map.put(rs.getString(1), rs.getInt(2)); }
        }
        return map;
    }

    // how many feedback ratings of each value 1..5
    public Map<Integer, Integer> ratingSpread() throws SQLException {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) { map.put(i, 0); }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT rating, COUNT(*) FROM feedback GROUP BY rating");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { map.put(rs.getInt(1), rs.getInt(2)); }
        }
        return map;
    }

    // one row per counsellor: name, total, pending, approved, completed, cancelled, average rating
    public List<Object[]> counsellorWorkload() throws SQLException {
        String sql = "SELECT u.full_name, COUNT(a.appointment_id), COALESCE(SUM(a.status = 'PENDING'), 0), COALESCE(SUM(a.status = 'APPROVED'), 0), "
                + "COALESCE(SUM(a.status = 'COMPLETED'), 0), COALESCE(SUM(a.status = 'CANCELLED'), 0), ROUND(AVG(f.rating), 1) "
                + "FROM counsellors c JOIN users u ON u.user_id = c.user_id "
                + "LEFT JOIN appointments a ON a.counsellor_id = c.counsellor_id "
                + "LEFT JOIN feedback f ON f.appointment_id = a.appointment_id "
                + "GROUP BY c.counsellor_id, u.full_name ORDER BY u.full_name";
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double avg = rs.getDouble(7);
                boolean noRatings = rs.wasNull();   // must be checked right after reading the column
                rows.add(new Object[]{rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), noRatings ? "-" : String.valueOf(avg)});
            }
        }
        return rows;
    }
}
