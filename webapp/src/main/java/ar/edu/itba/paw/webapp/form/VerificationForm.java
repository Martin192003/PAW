package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotEmpty;

public class VerificationForm {
    @NotEmpty(message = "{verify.notempty}")
    private String token;
    public String getToken() {
        return token;
    }
    public void setToken(final String token) {
        this.token = token;
    }
}
