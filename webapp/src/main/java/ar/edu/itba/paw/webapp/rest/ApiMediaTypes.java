package ar.edu.itba.paw.webapp.rest;

import javax.ws.rs.core.MediaType;

public final class ApiMediaTypes {

    private ApiMediaTypes() {
    }

    public static final String APPLICATION_JSON = MediaType.APPLICATION_JSON;
    public static final String LINK_COLLECTION = "application/vnd.paw.link-collection.v1+json";
    public static final String API_ROOT = "application/vnd.paw.api-root.v1+json";
    public static final String USER = "application/vnd.paw.user.v1+json";
    public static final String CLUB = "application/vnd.paw.club.v1+json";
    public static final String BOOKING = "application/vnd.paw.booking.v1+json";
    public static final String AUTH_TOKEN = "application/vnd.paw.auth-token.v1+json";
    public static final String AUTH_CHALLENGE = "application/vnd.paw.auth-challenge.v1+json";
    public static final String REVIEW = "application/vnd.paw.review.v1+json";
    public static final String PRODUCT = "application/vnd.paw.product.v1+json";
    public static final String TABLE = "application/vnd.paw.table.v1+json";
    public static final String EVENT = "application/vnd.paw.event.v1+json";
}
