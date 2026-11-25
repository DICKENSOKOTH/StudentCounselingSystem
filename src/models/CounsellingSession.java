package models;

import java.time.LocalDate;

// what happened in a completed appointment
public class CounsellingSession {

    private int sessionId;
    private int appointmentId;
    private String presentingConcern;
    private String counsellingGoals;
    private String sessionNotes;
    private String actionPlan;
    private int durationMinutes;
    private LocalDate followUpDate;

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }
    public String getPresentingConcern() { return presentingConcern; }
    public void setPresentingConcern(String presentingConcern) { this.presentingConcern = presentingConcern; }
    public String getCounsellingGoals() { return counsellingGoals; }
    public void setCounsellingGoals(String counsellingGoals) { this.counsellingGoals = counsellingGoals; }
    public String getSessionNotes() { return sessionNotes; }
    public void setSessionNotes(String sessionNotes) { this.sessionNotes = sessionNotes; }
    public String getActionPlan() { return actionPlan; }
    public void setActionPlan(String actionPlan) { this.actionPlan = actionPlan; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }
}
