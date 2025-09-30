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
            @RequestParam(value = "name") final String name,
            @RequestParam(value = "password") final String password,
            @RequestParam(value = "surname") final String surname,
            @RequestParam(value = "email") final String email,
            @RequestParam(value = "cellphone") final String cellphone,
            @RequestParam(value = "role") final String role
    ) {
        //final String userRole = Role.valueOf(role.toUpperCase()); // Convertir a mayúsculas
        final User u = us.create(username, name, password, surname, email, cellphone, role);
        return new ModelAndView("redirect:/user?id=" + u.getId());
    }

    @RequestMapping("/user")
    public ModelAndView index(@RequestParam(value = "id", required = true) final UUID id) {
        final ModelAndView mav = new ModelAndView("user");
        mav.addObject("user", us.findById(id));
        return mav;
    }

    @RequestMapping("/login")
    public ModelAndView login(@RequestParam(value = "success", required = false) final boolean success, @RequestParam(value = "usernameOrEmail", required = false) final String usernameOrEmail, @RequestParam(value = "password", required = false) final String password) {
        ModelAndView mav = new ModelAndView("login");
        mav.addObject("success", success);
        mav.addObject("usernameOrEmail", usernameOrEmail);

        if(usernameOrEmail != null && password != null) {
            boolean isAuthenticated = us.authenticateUsernameOrEmail(usernameOrEmail, password);
            if(isAuthenticated) {
                mav.setViewName("redirect:/clubs");
            }
            mav.addObject("error", "Invalid credentials");
        }
        return mav;
    }


}
