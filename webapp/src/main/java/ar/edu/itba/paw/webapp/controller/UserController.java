package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
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
            userService.create(form.getUsername(), form.getName(), form.getPassword(), form.getSurname(), form.getEmail(), form.getCellphone(), "USER");
        } catch (DuplicateEntityException ex) {
            binding.rejectValue("email", "user.email.duplicate", "Email already in use");
            return "register";
        }
        return "verifyMailNotice";
    }

    @GetMapping("/verify")
    public String showVerificationPage() {
        return "verify"; // Page with the form to input the token
    }

    @PostMapping("/verify")
    public String verifyToken(@RequestParam("token") final String token, Model model) {
        boolean success = userService.verifyUser(token);
        if (success) {
            model.addAttribute("success", "Account verified successfully.");
        } else {
            model.addAttribute("error", "Invalid verification token.");
        }
        return "verifyResult"; // Page showing the verification result
    }

    private UUID getCurrentUserId() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userService.findByUserName(username);
        return user.getId();
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model) {
        User user = userService.findById(getCurrentUserId());
        UserForm userForm = new UserForm();
        userForm.setUsername(user.getUsername());
        userForm.setName(user.getName());
        userForm.setSurname(user.getSurname());
        userForm.setEmail(user.getEmail());
        userForm.setCellphone(user.getCellphone());
        model.addAttribute("userForm", userForm);
        return "profile";
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        User user = userService.findById(getCurrentUserId());
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(
            @RequestParam("fieldName") String fieldName,
            @RequestParam("fieldValue") String fieldValue,
            final RedirectAttributes ra,
            Model model
    ) {
        User user = userService.findById(getCurrentUserId());

        if ("username".equals(fieldName)) {
            User existing = userService.findByUserName(fieldValue);
            if (existing != null && !existing.getId().equals(user.getId())) {
                model.addAttribute("user", user);
                model.addAttribute("usernameError", "El nombre de usuario ya está en uso.");
                model.addAttribute("openModal", true);
                model.addAttribute("editField", "username");
                return "profile";
            }
            user.setUserName(fieldValue);
        } else if ("name".equals(fieldName)) {
            user.setName(fieldValue);
        } else if ("surname".equals(fieldName)) {
            user.setSurname(fieldValue);
        } else if ("cellphone".equals(fieldName)) {
            user.setCellphone(fieldValue);
        } else {
            throw new IllegalArgumentException("Invalid field name");
        }

        userService.update(user);
        if (fieldName.equals("username")) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(), // nuevo username
                    user.getPassword(),
                    SecurityContextHolder.getContext().getAuthentication().getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        ra.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/profile";
    }
}
