package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.webapp.form.EditUserForm;
import ar.edu.itba.paw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
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
                               BindingResult errors,
                               Model model,
                               RedirectAttributes ra) {
        User user = userService.findById(getCurrentUserId());

        if (errors.hasFieldErrors("username")) {
            model.addAttribute("user", user);
            model.addAttribute("openModal", true);
            model.addAttribute("editField", "username");
            return "profile";
        }

        user.setUserName(form.getUsername());
        userService.update(user);

        // actualizar auth con nuevo username
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        user.getPassword(),
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ra.addFlashAttribute("success", "Usuario actualizado correctamente.");
        return "redirect:/profile";
    }

    @PostMapping("/edit/name")
    public String editName(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                           BindingResult errors,
                           Model model,
                           RedirectAttributes ra) {
        User user = userService.findById(getCurrentUserId());

        if (errors.hasFieldErrors("name")) {
            model.addAttribute("user", user);
            model.addAttribute("openModal", true);
            model.addAttribute("editField", "name");
            return "profile";
        }

        user.setName(form.getName());
        userService.update(user);

        ra.addFlashAttribute("success", "Nombre actualizado correctamente.");
        return "redirect:/profile";
    }

    @PostMapping("/edit/surname")
    public String editSurname(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                              BindingResult errors,
                              Model model,
                              RedirectAttributes ra) {
        User user = userService.findById(getCurrentUserId());

        if (errors.hasFieldErrors("surname")) {
            model.addAttribute("user", user);
            model.addAttribute("openModal", true);
            model.addAttribute("editField", "surname");
            return "profile";
        }

        user.setSurname(form.getSurname());
        userService.update(user);

        ra.addFlashAttribute("success", "Apellido actualizado correctamente.");
        return "redirect:/profile";
    }

    @PostMapping("/edit/cellphone")
    public String editCellphone(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                                BindingResult errors,
                                Model model,
                                RedirectAttributes ra) {
        User user = userService.findById(getCurrentUserId());

        if (errors.hasFieldErrors("cellphone")) {
            model.addAttribute("user", user);
            model.addAttribute("openModal", true);
            model.addAttribute("editField", "cellphone");
            return "profile";
        }

        user.setCellphone(form.getCellphone());
        userService.update(user);

        ra.addFlashAttribute("success", "Celular actualizado correctamente.");
        return "redirect:/profile";
    }

    @PostMapping("/edit/email")
    public String editEmail(@Valid @ModelAttribute("editUserForm") EditUserForm form,
                            BindingResult errors,
                            Model model,
                            RedirectAttributes ra) {
        User user = userService.findById(getCurrentUserId());

        if (errors.hasFieldErrors("email")) {
            model.addAttribute("user", user);
            model.addAttribute("openModal", true);
            model.addAttribute("editField", "email");
            return "profile";
        }

        user.setEmail(form.getEmail());
        userService.update(user);

        ra.addFlashAttribute("success", "Email actualizado correctamente.");
        return "redirect:/profile";
    }
}