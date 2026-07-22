package ar.edu.itba.paw.model.dto;


public class VerificationRequestDto {

    private String token;
    private String code;

    @ApiFieldAccess(ApiFieldAccess.Mode.WRITE_ONLY)
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.WRITE_ONLY)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
