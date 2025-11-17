package ar.edu.itba.paw.model;

import ar.edu.itba.paw.model.converter.RoleConverter;

import javax.persistence.*;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_name", nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String cellphone;

    @Convert(converter = RoleConverter.class)
    @Column(name="role", nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "is_verified")
    private boolean verified;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "locale")
    private String locale;

    @ManyToMany
    @JoinTable(
            name = "club_admins",
            joinColumns = @JoinColumn(name = "admin_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "club_id", referencedColumnName = "club_id")
    )
    private Set<Club> adminOfClubs = new HashSet<>();

    public User() {}

    public User(UUID userId, String userName, String name, String surname, String email,
                String passwordHash, String cellphone, Role role, OffsetDateTime createdAt, String locale) {
        this.id = userId;
        this.username = userName;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.passwordHash = passwordHash;
        this.cellphone = cellphone;
        this.role = role;
        this.createdAt = createdAt;
        this.locale = locale;
    }

    public User(UUID userId) {
        this.id = userId;
    }

    public UUID getId() { return id; }
    public void setUserId(UUID userId) { this.id = userId; }

    public String getUsername() { return username; }
    public void setUserName(String userName) { this.username = userName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getCellphone() { return cellphone; }
    public void setCellphone(String cellphone) { this.cellphone = cellphone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public String getResetPasswordToken() { return resetPasswordToken; }
    public void setResetPasswordToken(String resetPasswordToken) { this.resetPasswordToken = resetPasswordToken; }

    public Set<Club> getAdminOfClubs() { return adminOfClubs; }
    public void setAdminOfClubs(Set<Club> adminOfClubs) { this.adminOfClubs = adminOfClubs; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
}
