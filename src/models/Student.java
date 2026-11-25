package models;

// a student profile (inherits the person data)
public class Student extends Person {

    private int studentId;
    private String registrationNumber;
    private String course;
    private int yearOfStudy;

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    @Override
    public String getRole() { return User.STUDENT; }
}
