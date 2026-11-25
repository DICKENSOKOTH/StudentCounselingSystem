package models;

// a login account (password is stored as a PBKDF2 hash)
public class User {

    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String role;
    private java.sql.Timestamp createdAt;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }

    // roles
    public static final String ADMIN = "ADMIN";
    public static final String COUNSELLOR = "COUNSELLOR";
    public static final String STUDENT = "STUDENT";

    public boolean isAdmin() { return ADMIN.equals(role); }
    public boolean isCounsellor() { return COUNSELLOR.equals(role); }
    public boolean isStudent() { return STUDENT.equals(role); }
}
