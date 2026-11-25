package models;

import java.time.LocalDate;
import java.time.LocalTime;

// a booking between a student and a counsellor
public class Appointment {

    private int appointmentId;
    private int studentId;
    private int counsellorId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String reason;
    private String status;
    private String studentName;
    private String registrationNumber;
    private String counsellorName;
    private Integer rating;

    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public int getCounsellorId() { return counsellorId; }
    public void setCounsellorId(int counsellorId) { this.counsellorId = counsellorId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalTime appointmentTime) { this.appointmentTime = appointmentTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getCounsellorName() { return counsellorName; }
    public void setCounsellorName(String counsellorName) { this.counsellorName = counsellorName; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    // statuses
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    // an appointment is still open while it is waiting or approved
    public boolean isOpen() { return PENDING.equals(status) || APPROVED.equals(status); }
    public boolean hasFeedback() { return rating != null; }
}
