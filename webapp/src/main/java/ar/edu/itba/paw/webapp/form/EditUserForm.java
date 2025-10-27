package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.*;
import ar.edu.itba.paw.webapp.form.validation.UnusedUsername;
import ar.edu.itba.paw.webapp.form.validation.UnusedEmail;


public class EditUserForm {

    @UnusedUsername
    @NotEmpty(message = "{register.username.notempty}")
    @Pattern(regexp = "^$|^[A-Za-z0-9_]+$", message = "{register.username.pattern}")
    private String username;

    @NotEmpty(message = "{register.name.notempty}")
    @Pattern(regexp = "^$|^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$", message = "{register.name.pattern}")
    private String name;

    @NotEmpty(message = "{register.surname.notempty}")
    @Pattern(regexp = "^$|^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$", message = "{register.surname.pattern}")
    private String surname;

    @UnusedEmail
    @NotBlank(message = "{register.email.notblank}")
    @Email(message = "{register.email.invalid}")
    private String email;

    @NotEmpty(message = "{register.phone.notempty}")
    @Pattern(regexp = "^[0-9\\-\\+ ]*$", message = "{register.phone.pattern}")
    private String cellphone;

    @NotEmpty(message = "{register.password.notempty}")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @NotEmpty(message = "{register.confirmpassword.notempty}")
    private String confirmPassword;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCellphone() { return cellphone; }
    public void setCellphone(String cellphone) { this.cellphone = cellphone; }

    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}

