package models;

// common data of every person in the system (users are students or counsellors)
public abstract class Person {

    private int userId;
    private String fullName;
    private String email;
    private String phone;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // each kind of person tells which role it plays
    public abstract String getRole();

    @Override
    public String toString() { return fullName; }
}
