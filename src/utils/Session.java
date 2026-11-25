package utils;

import dao.CounsellorDAO;
import dao.StudentDAO;
import java.sql.SQLException;
import models.Counsellor;
import models.Student;
import models.User;

// who is logged in right now (the profile is loaded once, when a page first needs it)
public final class Session {

    private static User currentUser;
    private static Student student;
    private static Counsellor counsellor;

    private Session() { }

    public static User getUser() { return currentUser; }

    public static void setUser(User user) {
        currentUser = user;
        student = null;
        counsellor = null;
    }

    public static void clear() { setUser(null); }

    public static Student getStudent() throws SQLException {
        if (student == null && currentUser != null) { student = new StudentDAO().findByUserId(currentUser.getUserId()); }
        return student;
    }

    public static Counsellor getCounsellor() throws SQLException {
        if (counsellor == null && currentUser != null) { counsellor = new CounsellorDAO().findByUserId(currentUser.getUserId()); }
        return counsellor;
    }
}
