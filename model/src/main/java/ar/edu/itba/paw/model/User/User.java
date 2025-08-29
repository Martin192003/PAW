package ar.edu.itba.paw.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    public String username;
    public UUID id;
    public String password;
    public String surname;
    public String email;
    public String cellphone;
    public Role role;
    public LocalDateTime createdAt;

    public User(UUID id, String username, String password, String surname, String email, String cellphone, Role role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.surname = surname;
        this.email = email;
        this.cellphone = cellphone;
        this.role = role;
    }

    public User(){

    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCellphone() {
        return cellphone;
    }

    public void setCellphone(String cellphone) {
        this.cellphone = cellphone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


