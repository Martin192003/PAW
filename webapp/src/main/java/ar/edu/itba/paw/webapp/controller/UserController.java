package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.webapp.form.EditUserForm;
import ar.edu.itba.paw.webapp.form.UserForm;
import ar.edu.itba.paw.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.UUID;

@Controller
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
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

    private UUID getCurrentUserId() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userService.findByUserName(username);
        return user.getId();
    }

    private void prepareProfileModel(Model model) {
        User user = userService.findById(getCurrentUserId());
        model.addAttribute("user", user);

        EditUserForm editUserForm = new EditUserForm();
        editUserForm.setUsername(user.getUsername());
        editUserForm.setName(user.getName());
        editUserForm.setSurname(user.getSurname());
        editUserForm.setEmail(user.getEmail());
        editUserForm.setCellphone(user.getCellphone());
        model.addAttribute("editUserForm", editUserForm);
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model) {
        prepareProfileModel(model);
        return "profile";
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        prepareProfileModel(model);
        return "profile";
    }

    private String handleFieldUpdate(String field, String value, String fieldName,
                                     BindingResult errors, Model model,
                                     RedirectAttributes ra, boolean requiresAuthUpdate) {
        if (errors.hasFieldErrors(field)) {
            return handleValidationError(field, model);
        }

        try {
            User updatedUser = userService.updateUserField(getCurrentUserId(), field, value);

            if (requiresAuthUpdate) {
                updateAuthenticationContext(updatedUser);
            }

            ra.addFlashAttribute("success", fieldName + " actualizado correctamente.");
            return "redirect:/profile";
        } catch (DuplicateEntityException e) {
            return handleDuplicateError(field, model);
        }
    }

    private String handleValidationError(String field, Model model) {
        User user = userService.findById(getCurrentUserId());
        model.addAttribute("user", user);
        model.addAttribute("openModal", true);
        model.addAttribute("editField", field);
        return "profile";
    }

    private String handleDuplicateError(String field, Model model) {
        User user = userService.findById(getCurrentUserId());
        model.addAttribute("user", user);
        model.addAttribute("openModal", true);
        model.addAttribute("editField", field);
        model.addAttribute("error", field.equals("username") ? "Username already exists" : "Email already exists");
        return "profile";
    }

    private void updateAuthenticationContext(User updatedUser) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        updatedUser.getUsername(),
                        updatedUser.getPassword(),
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @PostMapping("/profile/edit/username")
    public String editUsername(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                               BindingResult errors, Model model, RedirectAttributes ra) {
        return handleFieldUpdate("username", form.getUsername(), "Usuario", errors, model, ra, true);
    }

    @PostMapping("/profile/edit/name")
    public String editName(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                           BindingResult errors, Model model, RedirectAttributes ra) {
        return handleFieldUpdate("name", form.getName(), "Nombre", errors, model, ra, false);
    }

    @PostMapping("/profile/edit/surname")
    public String editSurname(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                              BindingResult errors, Model model, RedirectAttributes ra) {
        return handleFieldUpdate("surname", form.getSurname(), "Apellido", errors, model, ra, false);
    }

    @PostMapping("/profile/edit/cellphone")
    public String editCellphone(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                                BindingResult errors, Model model, RedirectAttributes ra) {
        return handleFieldUpdate("cellphone", form.getCellphone(), "Celular", errors, model, ra, false);
    }

    @PostMapping("/profile/edit/email")
    public String editEmail(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                            BindingResult errors, Model model, RedirectAttributes ra) {
        return handleFieldUpdate("email", form.getEmail(), "Email", errors, model, ra, false);
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
}