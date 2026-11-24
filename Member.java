package model;

/**
 * Member entity representing a registered library patron.
 */
public class Member extends BaseEntity {
    private String fullName;
    private String email;
    private String phone;
    private String registrationDate;
    private String status; // ACTIVE, SUSPENDED, INACTIVE

    public Member() {
        super();
        this.status = "ACTIVE";
    }

    public Member(int id, String fullName, String email, String phone, String registrationDate, String status) {
        super(id);
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.registrationDate = registrationDate;
        this.status = (status != null && !status.isEmpty()) ? status : "ACTIVE";
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Member[id=" + id + ", name='" + fullName + "', email='" + email + "', status='" + status + "']";
    }
}
