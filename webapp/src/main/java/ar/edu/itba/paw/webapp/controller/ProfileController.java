package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.form.EditUserForm;
import ar.edu.itba.paw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/edit")
    public String editProfile(Model model, @AuthenticationPrincipal PawUserDetails userDetails) {
        prepareProfileModel(model, userDetails.getUser());
        return "profile";
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public String showProfile(Model model, @AuthenticationPrincipal PawUserDetails userDetails) {
        prepareProfileModel(model, userDetails.getUser());
        return "profile";
    }


    @PreAuthorize("isAuthenticated()")
    @PostMapping("/edit/username")
    public String editUsername(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                               BindingResult errors, Model model, RedirectAttributes ra,
                               @AuthenticationPrincipal PawUserDetails userDetails) {
        return handleFieldUpdate(userDetails.getUser(), "username", form.getUsername(), "Usuario", errors, model, ra, true);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/edit/name")
    public String editName(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                           BindingResult errors, Model model, RedirectAttributes ra,
                           @AuthenticationPrincipal PawUserDetails userDetails) {
        return handleFieldUpdate(userDetails.getUser(), "name", form.getName(), "Nombre", errors, model, ra, false);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/edit/surname")
    public String editSurname(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                              BindingResult errors, Model model, RedirectAttributes ra,
                              @AuthenticationPrincipal PawUserDetails userDetails) {
        return handleFieldUpdate(userDetails.getUser(), "surname", form.getSurname(), "Apellido", errors, model, ra, false);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/edit/cellphone")
    public String editCellphone(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                                BindingResult errors, Model model, RedirectAttributes ra,
                                @AuthenticationPrincipal PawUserDetails userDetails) {
        return handleFieldUpdate(userDetails.getUser(), "cellphone", form.getCellphone(), "Celular", errors, model, ra, false);
    }


    @PreAuthorize("isAuthenticated()")
    @PostMapping("/edit/email")
    public String editEmail(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                            BindingResult errors, Model model, RedirectAttributes ra,
                            @AuthenticationPrincipal PawUserDetails userDetails) {
        return handleFieldUpdate(userDetails.getUser(), "email", form.getEmail(), "Email", errors, model, ra, false);
    }


    @PreAuthorize("isAuthenticated()")
    @PostMapping("/edit/password")
    public String editPassword(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                               BindingResult errors, Model model, RedirectAttributes ra,
                               @AuthenticationPrincipal PawUserDetails userDetails) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "password.mismatch", "reset.password.mismatch");
        }

        if (errors.hasFieldErrors("password") || errors.hasFieldErrors("confirmPassword")) {
            return handleValidationError(userDetails.getUser(), "password", model);
        }

        try {
            boolean success = userService.changePasswordForAuthenticatedUser(userDetails.getUser().getId(), form.getPassword());

            if (success) {
                ra.addFlashAttribute("success", "password.updated");
                return "redirect:/profile";
            } else {
                model.addAttribute("error", "error.updated.password");
                return handleValidationError(userDetails.getUser(), "password", model);
            }
        } catch (Exception e) {
            model.addAttribute("error", "error.updated.password");
            return handleValidationError(userDetails.getUser(), "password", model);
        }
    }

    @PreAuthorize("isAuthenticated()")
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

    private void prepareProfileModel(Model model, User user) {
        model.addAttribute("user", user);

        EditUserForm editUserForm = new EditUserForm();
        editUserForm.setUsername(user.getUsername());
        editUserForm.setName(user.getName());
        editUserForm.setSurname(user.getSurname());
        editUserForm.setEmail(user.getEmail());
        editUserForm.setCellphone(user.getCellphone());
        model.addAttribute("editUserForm", editUserForm);
    }

    private String handleDuplicateError(User user, String field, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("openModal", true);
        model.addAttribute("editField", field);
        model.addAttribute("error", field.equals("username") ? "error.username.exists" : "error.email.exists");
        return "profile";
    }

    private String handleValidationError(User user, String field, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("openModal", true);
        model.addAttribute("editField", field);
        return "profile";
    }

    private String handleFieldUpdate(User user, String field, String value, String fieldName,
                                     BindingResult errors, Model model,
                                     RedirectAttributes ra, boolean requiresAuthUpdate) {
        if (errors.hasFieldErrors(field)) {
            return handleValidationError(user, field, model);
        }

        try {
            User updatedUser = userService.updateUserField(user.getId(), field, value);
            updateAuthenticationContext(updatedUser);

            if (requiresAuthUpdate) {
                updateAuthenticationContext(updatedUser);
            }

            ra.addFlashAttribute("success", fieldName + "updated.correctly");
            return "redirect:/profile";
        } catch (DuplicateEntityException e) {
            return handleDuplicateError(user, field, model);
        }
    }

    private void updateAuthenticationContext(User updatedUser) {
        PawUserDetails newDetails = new PawUserDetails(updatedUser, updatedUser.getUsername());
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        newDetails,
                        null,
                        newDetails.getAuthorities()
                );
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
    }


}
