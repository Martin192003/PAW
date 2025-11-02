package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.webapp.form.EditUserForm;
import ar.edu.itba.paw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
            throw new SecurityException("Usuario no autenticado");
        }

        String identifier = authentication.getName();
        User user = userService.findByUserName(identifier);
        if (user == null) user = userService.findByEmail(identifier);
        if (user == null) throw new SecurityException("Usuario no encontrado: " + identifier);

        return user.getId();
    }


    @GetMapping("/edit")
    public String editProfile(Model model) {
        prepareProfileModel(model);
        return "profile";
    }

    @GetMapping
    public String showProfile(Model model) {
        prepareProfileModel(model);
        return "profile";
    }


    @PostMapping("/edit/username")
    public String editUsername(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                               BindingResult errors, Model model, RedirectAttributes ra) {
        return handleFieldUpdate("username", form.getUsername(), "Usuario", errors, model, ra, true);
    }

    @PostMapping("/edit/name")
    public String editName(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                           BindingResult errors, Model model, RedirectAttributes ra) {
        return handleFieldUpdate("name", form.getName(), "Nombre", errors, model, ra, false);
    }

    @PostMapping("/edit/surname")
    public String editSurname(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                              BindingResult errors, Model model, RedirectAttributes ra) {
        return handleFieldUpdate("surname", form.getSurname(), "Apellido", errors, model, ra, false);
    }

    @PostMapping("/edit/cellphone")
    public String editCellphone(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                                BindingResult errors, Model model, RedirectAttributes ra) {
        return handleFieldUpdate("cellphone", form.getCellphone(), "Celular", errors, model, ra, false);
    }


    @PostMapping("/edit/email")
    public String editEmail(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                            BindingResult errors, Model model, RedirectAttributes ra) {
        return handleFieldUpdate("email", form.getEmail(), "Email", errors, model, ra, false);
    }

    @PostMapping("/edit/password")
    public String editPassword(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                               BindingResult errors, Model model, RedirectAttributes ra) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "password.mismatch", "Las contraseñas no coinciden");
        }

        if (errors.hasFieldErrors("password") || errors.hasFieldErrors("confirmPassword")) {
            return handleValidationError("password", model);
        }

        try {
            UUID currentUserId = getCurrentUserId();
            boolean success = userService.changePasswordForAuthenticatedUser(currentUserId, form.getPassword());

            if (success) {
                ra.addFlashAttribute("success", "Contraseña actualizada correctamente.");
                return "redirect:/profile";
            } else {
                model.addAttribute("error", "Error al actualizar la contraseña");
                return handleValidationError("password", model);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar la contraseña");
            return handleValidationError("password", model);
        }
    }

    private void prepareProfileModel(Model model) {
        User user = userService.findById(getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        model.addAttribute("user", user);

        EditUserForm editUserForm = new EditUserForm();
        editUserForm.setUsername(user.getUsername());
        editUserForm.setName(user.getName());
        editUserForm.setSurname(user.getSurname());
        editUserForm.setEmail(user.getEmail());
        editUserForm.setCellphone(user.getCellphone());
        model.addAttribute("editUserForm", editUserForm);
    }

    private String handleDuplicateError(String field, Model model) {
        User user = userService.findById(getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        model.addAttribute("user", user);
        model.addAttribute("openModal", true);
        model.addAttribute("editField", field);
        model.addAttribute("error", field.equals("username") ? "Username already exists" : "Email already exists");
        return "profile";
    }

    private String handleValidationError(String field, Model model) {
        User user = userService.findById(getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        model.addAttribute("user", user);
        model.addAttribute("openModal", true);
        model.addAttribute("editField", field);
        return "profile";
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
    private void updateAuthenticationContext(User updatedUser) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        updatedUser.getUsername(),
                        updatedUser.getPassword(),
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }


}
