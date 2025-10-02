package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final UserService userService;

    @Autowired
    public CustomAuthenticationFailureHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String usernameOrEmail = request.getParameter("usernameOrEmail");

        // Verificar si el usuario existe pero no está verificado
        if (usernameOrEmail != null && !usernameOrEmail.trim().isEmpty()) {
            User user = userService.findByUserName(usernameOrEmail);
            if (user == null) {
                user = userService.findByEmail(usernameOrEmail);
            }

            if (user != null && !user.isVerified()) {
                String encodedUsername = URLEncoder.encode(usernameOrEmail, StandardCharsets.UTF_8.toString());
                response.sendRedirect(request.getContextPath() + "/unverified?username=" + encodedUsername);
                return;
            }
        }

        // Para otros errores de autenticación
        response.sendRedirect(request.getContextPath() + "/login?error=true");
    }
}