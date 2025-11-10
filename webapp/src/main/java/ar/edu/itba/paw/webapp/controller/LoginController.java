package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.form.UserForm;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.webapp.form.VerificationForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import ar.edu.itba.paw.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import ar.edu.itba.paw.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Controller
public class LoginController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    private final UserService userService;

    @Autowired
    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"", "/"})
    public String HelloWorldController() {
        return "redirect:/clubs";
    }

    @RequestMapping("/register")
    public ModelAndView register() {
        ModelAndView mav = new ModelAndView("register");
        mav.addObject("userForm", new UserForm());
        return mav;
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("userForm") final UserForm form,
            final BindingResult binding,
            final RedirectAttributes ra
    ) {
        if (binding.hasErrors()) {
            return "register";
        }

        try {
            userService.create(
                    form.getUsername(),
                    form.getName(),
                    form.getPassword(),
                    form.getSurname(),
                    form.getEmail(),
                    form.getCellphone(),
                    "USER",
                    form.getLocale()
            );
            ra.addFlashAttribute("success", "registration.success");
            return "redirect:/verifyMailNotice";
        } catch (DuplicateEntityException e) {
            if (e.getMessage().contains("email")) {
                binding.rejectValue("email", "duplicate.email", "error.mailAlreadyRegistered");
            } else {
                binding.rejectValue("username", "duplicate.username", "error.usernameAlreadyRegistered");
            }
            return "register";
        } catch (Exception e) {
            LOGGER.error("error.registration", e);
            binding.reject("registration.error", "error.during.registration");
            return "register";
        }
    }

    @GetMapping("/verifyMailNotice")
    public String showVerifyMailNotice() {
        return "verifyMailNotice";
    }

    @PostMapping("/verify")
    public String verifyToken(
            @Valid @ModelAttribute("verificationForm") VerificationForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "verify";
        }
        boolean success = userService.verifyUser(form.getToken());
        if (success) {
            model.addAttribute("success", "register.valid.token");
            return "verifyResult";
        } else {
            model.addAttribute("error", "register.invalid.token");
        }
        return "verify";
    }

    @GetMapping("/verify-account")
    public String verifyAccountFromEmail(@RequestParam(value = "token", required = false) String token,
                                         Model model) {
        if (token == null || token.trim().isEmpty()) {
            return "redirect:/unverified";
        }

        User userToVerify = userService.findByVerificationToken(token);
        if (userToVerify != null && !userToVerify.isVerified()) {
            boolean isValid = userService.verifyUser(token);
            if (isValid) {
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userToVerify.getUsername(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

                model.addAttribute("success", "register.valid.token");
                model.addAttribute("userAuthenticated", true);
                return "verifyResult";
            }
        }

        // Token inválido - redirigir a unverified
        return "redirect:/unverified";
    }
    
    @GetMapping("/unverified")
    public String showUnverifiedPage(@RequestParam(required = false) String username, Model model) {
        if (username != null) {
            model.addAttribute("username", username);
        }
        return "unverified";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam("usernameOrEmail") String usernameOrEmail,
                                     Model model) {
        boolean success = userService.resendVerificationToken(usernameOrEmail);
        if (success) {
            model.addAttribute("success", "verification.mail.sentSuccessfully");
            return "verifyMailNotice";
        } else {
            model.addAttribute("error", "error.userNotFound");
            model.addAttribute("username", usernameOrEmail);
            return "unverified";
        }
    }

    @RequestMapping("/login")
    public ModelAndView login(@RequestParam(value = "success", required = false) final boolean success,
                              @RequestParam(value = "usernameOrEmail", required = false) final String usernameOrEmail,
                              @RequestParam(value = "password", required = false) final String password,
                              final HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("login");
        mav.addObject("success", success);
        mav.addObject("usernameOrEmail", usernameOrEmail);

        if (usernameOrEmail != null && password != null) {
            try {
                User user = userService.authenticateAndGetUser(usernameOrEmail, password);
                if (user != null) {
                    String detectedLocale = request.getLocale().toLanguageTag();
                    userService.updateUserLocale(user.getId(), detectedLocale);

                    userService.sendReviewRequestsForPastBookings(user);

                    mav.setViewName("redirect:/clubs");
                } else {
                    mav.addObject("error", "error.invalidCredentials");
                }
            } catch (IllegalStateException e) {
                if (e.getMessage().contains("not verified")) {
                    mav.addObject("error", "notification.verifyAccount");
                    mav.addObject("showResendLink", true);
                } else {
                    mav.addObject("error", "error.authentication");
                }
            }
        }
        return mav;
    }

    @RequestMapping("/user")
    public ModelAndView index(@RequestParam(value = "id", required = true) final UUID id) {
        final ModelAndView mav = new ModelAndView("user");
        mav.addObject("user", userService.findById(id));
        return mav;
    }
}