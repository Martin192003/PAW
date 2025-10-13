package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.form.ForgotPasswordForm;
import ar.edu.itba.paw.webapp.form.ResetPasswordForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import ar.edu.itba.paw.model.User;
import java.util.Collections;

import javax.validation.Valid;

@Controller
public class PasswordController {

    private final UserService userService;

    @Autowired
    public PasswordController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/forgot-password")
    public String showForgotForm(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "forgotPasswordResult";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "forgotPasswordResult";
        }
        boolean success = userService.requestPasswordReset(form.getEmail());
        if (success) {
            model.addAttribute("success", "forgot.password.sent");
        } else {
            model.addAttribute("success", "forgot.password.sent");
        }
        return "forgotPasswordResult";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        if (!model.containsAttribute("resetPasswordForm")) {
            model.addAttribute("resetPasswordForm", new ResetPasswordForm());
        }
        model.addAttribute("token", token);
        return "resetPasswordResult";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
                                       BindingResult bindingResult,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("token", token);
            return "resetPasswordResult";
        }

        User user = userService.findByResetPasswordToken(token);
        if (user == null) {
            model.addAttribute("error", "Token inválido o expirado.");
            model.addAttribute("token", token);
            return "resetPasswordResult";
        }

        boolean success = userService.resetPassword(token, form.getNewPassword());
        if (!success) {
            model.addAttribute("error", "Token inválido o expirado.");
            model.addAttribute("token", token);
            return "resetPasswordResult";
        }

        // Autenticar automáticamente al usuario después del reset exitoso
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Ir a página de éxito en lugar de redirigir directamente
        model.addAttribute("success", "password.reset.success");
        return "passwordResetSuccess";
    }
}