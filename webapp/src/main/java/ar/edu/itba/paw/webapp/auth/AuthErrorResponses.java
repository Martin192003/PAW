package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.model.dto.ErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

final class AuthErrorResponses {
    static final String APPLICATION_JSON = "application/json";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuthErrorResponses() {
    }

    static void write(HttpServletResponse response, int status, String error) throws IOException {
        response.setStatus(status);
        response.setContentType(APPLICATION_JSON);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        MAPPER.writeValue(response.getWriter(), new ErrorDto(error));
    }
}
