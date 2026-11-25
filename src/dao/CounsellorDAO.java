package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import models.Counsellor;

// counsellor profiles (name, email, phone come from the users table)
public class CounsellorDAO {

    private static final String SELECT = "SELECT c.counsellor_id, c.user_id, c.specialization, c.qualification, c.gender, c.religion, "
            + "u.full_name, u.email, u.phone FROM counsellors c JOIN users u ON u.user_id = c.user_id ";

    private Counsellor map(ResultSet rs) throws SQLException {
        Counsellor c = new Counsellor();
        c.setCounsellorId(rs.getInt("counsellor_id"));
        c.setUserId(rs.getInt("user_id"));
        c.setSpecialization(rs.getString("specialization"));
        c.setQualification(rs.getString("qualification"));
        c.setGender(rs.getString("gender"));
        c.setReligion(rs.getString("religion"));
        c.setFullName(rs.getString("full_name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        return c;
    }

    public Counsellor findByUserId(int userId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE c.user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<Counsellor> findAll() throws SQLException {
        List<Counsellor> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "ORDER BY u.full_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { list.add(map(rs)); }
        }
        return list;
    }

    // Update: profile + name/phone, saved together
    public boolean update(Counsellor c) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement u = con.prepareStatement("UPDATE users SET full_name = ?, phone = ? WHERE user_id = ?");
                 PreparedStatement p = con.prepareStatement("UPDATE counsellors SET specialization = ?, qualification = ?, gender = ?, religion = ? WHERE counsellor_id = ?")) {
                u.setString(1, c.getFullName());
                u.setString(2, c.getPhone());
                u.setInt(3, c.getUserId());
                u.executeUpdate();
                p.setString(1, c.getSpecialization());
                p.setString(2, c.getQualification());
                p.setString(3, c.getGender());
                p.setString(4, c.getReligion());
                p.setInt(5, c.getCounsellorId());
                p.executeUpdate();
                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }
}
