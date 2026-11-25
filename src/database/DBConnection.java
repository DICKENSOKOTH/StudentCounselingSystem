package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// one place for the database connection (XAMPP MySQL / MariaDB)
// usage: try (Connection con = DBConnection.getConnection()) { ... }
public class DBConnection {

    // database settings (XAMPP defaults)
    private static final String URL = "jdbc:mariadb://localhost:3306/student_counselling_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private DBConnection() { }

    // open a new connection (the caller closes it)
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // test database
    public static void main(String[] args) {
        try (Connection con = getConnection()) {
            System.out.println("Connected to database: " + con.getCatalog());
        } catch (SQLException ex) {
            System.err.println("Connection failed: " + ex.getMessage());
        }
    }
}
