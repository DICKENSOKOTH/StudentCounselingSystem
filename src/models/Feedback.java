package models;

import java.time.LocalDate;

// a student's rating of a completed appointment
public class Feedback {

    private int feedbackId;
    private int appointmentId;
    private int rating;
    private String comments;
    private java.sql.Timestamp createdAt;
    private String studentName;
    private String counsellorName;
    private LocalDate appointmentDate;

    public int getFeedbackId() { return feedbackId; }
    public void setFeedbackId(int feedbackId) { this.feedbackId = feedbackId; }
    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getCounsellorName() { return counsellorName; }
    public void setCounsellorName(String counsellorName) { this.counsellorName = counsellorName; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
}
