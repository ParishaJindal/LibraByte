package model;

/**
 * User entity representing an authenticated account in LibraByte.
 * Extends BaseEntity to demonstrate Object-Oriented inheritance.
 */
public class User extends BaseEntity {
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String salt;
    private String role; // "ADMIN" or "MEMBER"
    private Integer memberId; // Linked member catalog ID (nullable for standalone admins)
    private String profileImage = ""; // Profile photo URL, base64 data URI, or asset path

    public User() {
        super();
        this.role = "MEMBER";
        this.profileImage = "";
    }

    public User(int id, String fullName, String email, String phone, String passwordHash, String salt, String role, Integer memberId, String createdAt) {
        this(id, fullName, email, phone, passwordHash, salt, role, memberId, createdAt, "");
    }

    public User(int id, String fullName, String email, String phone, String passwordHash, String salt, String role, Integer memberId, String createdAt, String profileImage) {
        super(id, createdAt);
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = (role != null && !role.isEmpty()) ? role : "MEMBER";
        this.memberId = memberId;
        this.profileImage = (profileImage != null) ? profileImage : "";
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = (profileImage != null) ? profileImage : ""; }

    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }
    public boolean isMember() { return "MEMBER".equalsIgnoreCase(role); }

    @Override
    public String toString() {
        return "User[id=" + id + ", name='" + fullName + "', email='" + email + "', role=" + role + "]";
    }
}
