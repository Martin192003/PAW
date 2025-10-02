package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.UUID;

public class ClubForm {
    private UUID clubId;

    @NotBlank(message = "{contact.name.notblank}")
    @Pattern(regexp = "^$|^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$", message = "{contact.name.pattern}")
    private String name;

    @NotBlank(message = "{contact.email.notblank}")
    @Email(message = "{contact.email.invalid}")
    private String email;

    @Pattern(regexp = "^[0-9\\-\\+ ]*$", message = "{contact.phone.pattern}")
    private String phone;

    @NotBlank(message = "{contact.message.notblank}")
    private String message;

    public String getName() {
        return name;
    }

    public UUID getClubId() {
        return clubId;
    }

    public void setClubId(UUID clubId) {
        this.clubId = clubId;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
