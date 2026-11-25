package models;

// a counsellor profile (inherits the person data)
public class Counsellor extends Person {

    private int counsellorId;
    private String specialization;
    private String qualification;
    private String gender;
    private String religion;

    public int getCounsellorId() { return counsellorId; }
    public void setCounsellorId(int counsellorId) { this.counsellorId = counsellorId; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    @Override
    public String getRole() { return User.COUNSELLOR; }

    // shown in the booking list
    @Override
    public String toString() { return getFullName() + (specialization == null || specialization.isEmpty() ? "" : "  -  " + specialization); }
}
