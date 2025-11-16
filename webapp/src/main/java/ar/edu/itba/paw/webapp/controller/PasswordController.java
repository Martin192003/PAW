package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.form.ForgotPasswordForm;
import ar.edu.itba.paw.webapp.form.ResetPasswordForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ar.edu.itba.paw.model.User;
import java.util.Optional;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.context.SecurityContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javax.validation.Valid;

@Controller
public class PasswordController {

    private final UserService userService;
    private final UserDetailsService userDetailsService;

    @Autowired
    public PasswordController(UserService userService, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @PreAuthorize("isAnonymous()")
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
        userService.requestPasswordReset(form.getEmail());
        model.addAttribute("success", "forgot.password.sent");
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
                                       Model model,
                                       HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("token", token);
            return "resetPasswordResult";
        }

        Optional<User> optUser = userService.resetPassword(token, form.getNewPassword());
        if (optUser.isEmpty()) {
            model.addAttribute("error", "verify.error");
            model.addAttribute("token", token);
            return "resetPasswordResult";
        }
        User user = optUser.get();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        model.addAttribute("success", "password.reset.success");
        return "passwordResetSuccess";
    }


}