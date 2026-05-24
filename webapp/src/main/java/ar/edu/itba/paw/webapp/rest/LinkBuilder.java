package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.LinkDto;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

public final class LinkBuilder {

    private LinkBuilder() {
    }

    public static LinkDto link(String rel, URI uri, String method) {
        return new LinkDto(rel, uri.toString(), method);
    }

    public static LinkDto link(String rel, UriBuilder builder, String method) {
        return link(rel, builder.build(), method);
    }

    public static LinkDto link(String rel, String href, String method) {
        return new LinkDto(rel, href, method);
    }

    public static UriBuilder api(UriInfo uriInfo, String path) {
        return uriInfo.getBaseUriBuilder().path(path);
    }
}
