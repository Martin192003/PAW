package ar.edu.itba.paw.model.dto;


public class LinkDto {
    private String rel;
    private String href;
    private String method;

    public LinkDto() {
    }

    public LinkDto(String rel, String href, String method) {
        this.rel = rel;
        this.href = href;
        this.method = method;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
