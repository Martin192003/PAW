package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.form.UserForm;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import ar.edu.itba.paw.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import ar.edu.itba.paw.service.UserService;

import javax.validation.Valid;
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
                    "USER"
            );
            ra.addFlashAttribute("success", "Usuario registrado correctamente. Revisa tu email para verificar tu cuenta.");
            return "redirect:/verify";
        } catch (DuplicateEntityException e) {
            if (e.getMessage().contains("email")) {
                binding.rejectValue("email", "duplicate.email", "Este email ya está registrado");
            } else {
                binding.rejectValue("username", "duplicate.username", "Este nombre de usuario ya está registrado");
            }
            return "register";
        } catch (Exception e) {
            LOGGER.error("Error during registration", e);
            binding.reject("registration.error", "Error durante el registro. Intenta nuevamente.");
            return "register";
        }
    }

    @GetMapping("/verify")
    public String showVerificationPage() {
        return "verify";
    }

    @PostMapping("/verify")
    public String verifyToken(@RequestParam("token") final String token, Model model) {
        boolean success = userService.verifyUser(token);
        if (success) {
            model.addAttribute("success", "Account verified successfully.");
        } else {
            model.addAttribute("error", "Invalid verification token.");
        }
        return "verifyResult";
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
            model.addAttribute("success", "Verification email sent successfully.");
            return "verifyMailNotice";
        } else {
            model.addAttribute("error", "User not found or already verified.");
            model.addAttribute("username", usernameOrEmail);
            return "unverified";
        }
    }

    @RequestMapping("/login")
    public ModelAndView login(@RequestParam(value = "success", required = false) final boolean success,
                              @RequestParam(value = "usernameOrEmail", required = false) final String usernameOrEmail,
                              @RequestParam(value = "password", required = false) final String password) {
        ModelAndView mav = new ModelAndView("login");
        mav.addObject("success", success);
        mav.addObject("usernameOrEmail", usernameOrEmail);

        if (usernameOrEmail != null && password != null) {
            try {
                User user = userService.authenticateAndGetUser(usernameOrEmail, password);
                if (user != null) {
                    mav.setViewName("redirect:/clubs");
                } else {
                    mav.addObject("error", "Invalid credentials");
                }
            } catch (IllegalStateException e) {
                if (e.getMessage().contains("not verified")) {
                    mav.addObject("error", "Please verify your account first");
                    mav.addObject("showResendLink", true);
                } else {
                    mav.addObject("error", "Authentication error");
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