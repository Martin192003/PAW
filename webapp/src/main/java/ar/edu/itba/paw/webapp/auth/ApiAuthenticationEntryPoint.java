package ar.edu.itba.paw.webapp.auth;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Usuario existente pero no verificado → 403 para que el frontend
        // redirija al flow de verificación de email en lugar de mostrar
        // "credenciales inválidas".
        if (authException instanceof DisabledException) {
            AuthErrorResponses.write(response, HttpServletResponse.SC_FORBIDDEN, "account_disabled");
            return;
        }

        String authorization = request.getHeader("Authorization");
        String error = authorization != null && authorization.regionMatches(true, 0, "Basic ", 0, 6)
                ? "invalid_credentials"
                : "invalid_token";
        AuthErrorResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, error);
    }
}
