package ar.edu.itba.paw.model;

import java.time.OffsetDateTime;
import java.util.UUID;


public class User {

    private UUID userId;
    private String username;
    private String surname;
    private String email;
    private String passwordHash;
    private String cellphone;
    private Role role; // 'admin' | 'user' (validado por la DB)
    private OffsetDateTime createdAt;

    public User() {}

    public User(UUID userId, String userName, String surname, String email, String passwordHash,
                String cellphone, Role role, OffsetDateTime createdAt) {
        this.userId = userId;
        this.username = userName;
        this.surname = surname;
        this.email = email;
        this.passwordHash = passwordHash;
        this.cellphone = cellphone;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUserName(String userName) { this.username = userName; }

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
}
