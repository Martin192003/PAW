package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.form.UserForm;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import ar.edu.itba.paw.service.UserService;

import javax.validation.Valid;
import java.util.UUID;

@Controller
public class HelloWorldController {

    private UserService us; // Inyección del servicio

    @GetMapping({"", "/"})
    public String HelloWorldController() {
        return "redirect:/clubs";
    }

    @Autowired
    public HelloWorldController(UserService us){
        this.us = us;
    }

    @RequestMapping("/create")
    public ModelAndView create(
            @RequestParam(value = "username") final String username,
            @RequestParam(value = "password") final String password,
            @RequestParam(value = "surname") final String surname,
            @RequestParam(value = "email") final String email,
            @RequestParam(value = "cellphone") final String cellphone,
            @RequestParam(value = "role") final String role
    ) {
        //final String userRole = Role.valueOf(role.toUpperCase()); // Convertir a mayúsculas
        final User u = us.create(username, password, surname, email, cellphone, role);
        return new ModelAndView("redirect:/user?id=" + u.getId());
    }

    @RequestMapping("/user")
    public ModelAndView index(@RequestParam(value = "id", required = true) final UUID id) {
        final ModelAndView mav = new ModelAndView("user");
        mav.addObject("user", us.findById(id));
        return mav;
    }

    @RequestMapping("/login")
    public ModelAndView login(@RequestParam(value = "success", required = false) final boolean success, @RequestParam(value = "username", required = false) final String username, @RequestParam(value = "password", required = false) final String password) {
        ModelAndView mav = new ModelAndView("login");
        mav.addObject("success", success);
        mav.addObject("username", username);
        return mav;
    }

    @RequestMapping("/register")
    public ModelAndView register() {
        ModelAndView mav = new ModelAndView("register");
        mav.addObject("userForm", new UserForm());
        return mav;
    }

    @PostMapping("/register")
    public ModelAndView register(@Valid @ModelAttribute("userForm") final UserForm userForm, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            return new ModelAndView("register");
        }
        us.create(userForm.getUsername(), userForm.getPassword(), userForm.getSurname(), userForm.getEmail(), userForm.getCellphone(), Role.USER.toString());
        return new ModelAndView("redirect:/login");
    }

}
