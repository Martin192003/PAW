package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.*;
import java.util.UUID;

public class UserForm {

    @NotEmpty
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "{contact.username.pattern}")
    private String username;

    @NotEmpty
    @Pattern(regexp = "^$|^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$")
    private String surname;

    @NotBlank(message = "{contact.email.notblank}")
    @Email
    private String email;

    @NotEmpty
    @NotNull
    //@Pattern()
    @Size(min = 6)
    private String password;

    @NotEmpty
    @Pattern(regexp = "^[0-9\\-\\+ ]*$", message = "{contact.phone.pattern}")
    private String cellphone;

    @NotEmpty
    private String role;

    public String getUsername() {return username;}
    public void setUsername(String username) {this.username = username;}

    public String getSurname() {return surname;}
    public void setSurname(String surname) {this.surname = surname;}

    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}

    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}

    public String getCellphone() {return cellphone;}
    public void setCellphone(String cellphone) {this.cellphone = cellphone;}

    public String getRole() {return role;}
    public void setRole(String role) {this.role = role;}

}
