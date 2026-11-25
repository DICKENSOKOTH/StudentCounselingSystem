package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import models.Student;

// student profiles (name, email, phone come from the users table)
public class StudentDAO {

    private static final String SELECT = "SELECT s.student_id, s.user_id, s.registration_number, s.course, s.year_of_study, "
            + "u.full_name, u.email, u.phone FROM students s JOIN users u ON u.user_id = s.user_id ";

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setUserId(rs.getInt("user_id"));
        s.setRegistrationNumber(rs.getString("registration_number"));
        s.setCourse(rs.getString("course"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        return s;
    }

    public Student findByUserId(int userId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE s.user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<Student> findAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "ORDER BY u.full_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { list.add(map(rs)); }
        }
        return list;
    }

    // Update: profile + name/phone, saved together
    public boolean update(Student s) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement u = con.prepareStatement("UPDATE users SET full_name = ?, phone = ? WHERE user_id = ?");
                 PreparedStatement p = con.prepareStatement("UPDATE students SET registration_number = ?, course = ?, year_of_study = ? WHERE student_id = ?")) {
                u.setString(1, s.getFullName());
                u.setString(2, s.getPhone());
                u.setInt(3, s.getUserId());
                u.executeUpdate();
                p.setString(1, s.getRegistrationNumber());
                p.setString(2, s.getCourse());
                p.setInt(3, s.getYearOfStudy());
                p.setInt(4, s.getStudentId());
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
