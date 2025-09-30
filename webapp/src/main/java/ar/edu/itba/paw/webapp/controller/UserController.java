package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.webapp.form.UserForm;
import ar.edu.itba.paw.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

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
        ra.addFlashAttribute("success", "Account created. Please check your email.");
        return "redirect:/login";
    }

    @GetMapping("/verify")
    public String verifyUser(@RequestParam("token") final String token, RedirectAttributes ra){
        try{
            userService.verifyUser(token);
            ra.addFlashAttribute("success", "Account verified successfully.");
        } catch(IllegalAccessError e){
            ra.addFlashAttribute("error", "Invalid verification token.");
        }
        return "redirect:/login";
    }

}
