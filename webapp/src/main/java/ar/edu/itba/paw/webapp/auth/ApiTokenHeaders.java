package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.model.User;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

public final class ApiTokenHeaders {

    public static final String AUTH_TOKEN = "Paw-Auth-Token";
    public static final String TOKEN_TYPE = "Paw-Token-Type";
    public static final String TOKEN_EXPIRES_IN = "Paw-Token-Expires-In";
    private static final String AUTH_TOKEN_ATTRIBUTE = ApiTokenHeaders.class.getName() + ".authToken";
    private static final String TOKEN_EXPIRES_IN_ATTRIBUTE = ApiTokenHeaders.class.getName() + ".expiresIn";
    private static final String BEARER = "Bearer";

    private ApiTokenHeaders() {
    }

    public static String tokenFor(JwtUtil jwtUtil, User user) {
        return jwtUtil.generateToken(user.getId().toString());
    }

    public static void remember(HttpServletRequest request, JwtUtil jwtUtil, User user) {
        request.setAttribute(AUTH_TOKEN_ATTRIBUTE, tokenFor(jwtUtil, user));
        request.setAttribute(TOKEN_EXPIRES_IN_ATTRIBUTE, jwtUtil.getExpirationSeconds());
    }

    public static void addTo(HttpServletResponse response, JwtUtil jwtUtil, User user) {
        response.setHeader(AUTH_TOKEN, tokenFor(jwtUtil, user));
        response.setHeader(TOKEN_TYPE, BEARER);
        response.setHeader(TOKEN_EXPIRES_IN, String.valueOf(jwtUtil.getExpirationSeconds()));
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        mergeVary(response);
    }

    public static void addTo(HttpServletResponse response, HttpServletRequest request) {
        String token = tokenFrom(request);
        Long expiresIn = expiresInFrom(request);
        if (!StringUtils.hasText(token) || expiresIn == null) {
            return;
        }

        response.setHeader(AUTH_TOKEN, token);
        response.setHeader(TOKEN_TYPE, BEARER);
        response.setHeader(TOKEN_EXPIRES_IN, String.valueOf(expiresIn));
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        mergeVary(response);
    }

    public static void addTo(MultivaluedMap<String, Object> headers, HttpServletRequest request) {
        String token = tokenFrom(request);
        Long expiresIn = expiresInFrom(request);
        if (!StringUtils.hasText(token) || expiresIn == null) {
            return;
        }

        headers.putSingle(AUTH_TOKEN, token);
        headers.putSingle(TOKEN_TYPE, BEARER);
        headers.putSingle(TOKEN_EXPIRES_IN, expiresIn);
        headers.putSingle("Cache-Control", "no-store");
        headers.putSingle("Pragma", "no-cache");
        mergeVary(headers);
    }

    public static Response.ResponseBuilder addTo(Response.ResponseBuilder response, JwtUtil jwtUtil, User user) {
        return addTo(response, tokenFor(jwtUtil, user), jwtUtil.getExpirationSeconds());
    }

    public static Response.ResponseBuilder addTo(Response.ResponseBuilder response, String token, long expiresIn) {
        return response
                .header(AUTH_TOKEN, token)
                .header(TOKEN_TYPE, BEARER)
                .header(TOKEN_EXPIRES_IN, expiresIn)
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .header("Vary", "Authorization");
    }

    private static void mergeVary(HttpServletResponse response) {
        String vary = response.getHeader("Vary");
        if (!StringUtils.hasText(vary)) {
            response.setHeader("Vary", "Authorization");
            return;
        }

        boolean alreadyPresent = Arrays.stream(vary.split(","))
                .map(String::trim)
                .anyMatch(value -> "Authorization".equalsIgnoreCase(value));
        if (!alreadyPresent) {
            response.setHeader("Vary", vary + ", Authorization");
        }
    }

    private static void mergeVary(MultivaluedMap<String, Object> headers) {
        List<Object> values = headers.get("Vary");
        boolean alreadyPresent = values != null && values.stream()
                .map(String::valueOf)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .anyMatch(value -> "Authorization".equalsIgnoreCase(value));
        if (!alreadyPresent) {
            headers.add("Vary", "Authorization");
        }
    }

    private static String tokenFrom(HttpServletRequest request) {
        Object value = request.getAttribute(AUTH_TOKEN_ATTRIBUTE);
        return value instanceof String ? (String) value : null;
    }

    private static Long expiresInFrom(HttpServletRequest request) {
        Object value = request.getAttribute(TOKEN_EXPIRES_IN_ATTRIBUTE);
        return value instanceof Long ? (Long) value : null;
    }
}
