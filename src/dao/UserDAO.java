package dao;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import models.Counsellor;
import models.Student;
import models.User;
import utils.PasswordUtil;

// accounts: register, log in, CRUD for the admin
public class UserDAO implements CrudDAO<User> {

    private static final String COLS = "user_id, full_name, email, phone, password_hash, role, created_at";

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }

    // insert one account row and remember its new id
    private void insertUser(Connection con, User u) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, phone, password_hash, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail().trim().toLowerCase());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getPasswordHash());
            ps.setString(5, u.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) { u.setUserId(keys.getInt(1)); }
            }
        }
    }

    // Create: a plain account (used by the admin)
    @Override
    public boolean create(User u) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            insertUser(con, u);
            return u.getUserId() > 0;
        }
    }

    // Create: account + student profile in one transaction
    public boolean createStudent(User u, Student s) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                u.setRole(User.STUDENT);
                insertUser(con, u);
                String sql = "INSERT INTO students (user_id, registration_number, course, year_of_study) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, u.getUserId());
                    ps.setString(2, s.getRegistrationNumber());
                    ps.setString(3, s.getCourse());
                    ps.setInt(4, s.getYearOfStudy());
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    // Create: account + counsellor profile in one transaction
    public boolean createCounsellor(User u, Counsellor c) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                u.setRole(User.COUNSELLOR);
                insertUser(con, u);
                String sql = "INSERT INTO counsellors (user_id, specialization, qualification, gender, religion) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, u.getUserId());
                    ps.setString(2, c.getSpecialization());
                    ps.setString(3, c.getQualification());
                    ps.setString(4, c.getGender());
                    ps.setString(5, c.getReligion());
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    // Read: one account
    @Override
    public User findById(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT " + COLS + " FROM users WHERE user_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    // Read: all accounts
    @Override
    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT " + COLS + " FROM users ORDER BY created_at DESC, user_id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { list.add(map(rs)); }
        }
        return list;
    }

    // Read: log in with email + password (null when wrong)
    public User authenticate(String email, String password) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT " + COLS + " FROM users WHERE email = ?")) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = map(rs);
                    if (PasswordUtil.verify(password, u.getPasswordHash())) { return u; }
                }
            }
        }
        return null;
    }

    // Update: name, email, phone
    @Override
    public boolean update(User u) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE users SET full_name = ?, email = ?, phone = ? WHERE user_id = ?")) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail().trim().toLowerCase());
            ps.setString(3, u.getPhone());
            ps.setInt(4, u.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    // Update: new password
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE users SET password_hash = ? WHERE user_id = ?")) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // Delete: the profile row goes with it (ON DELETE CASCADE)
    @Override
    public boolean delete(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
