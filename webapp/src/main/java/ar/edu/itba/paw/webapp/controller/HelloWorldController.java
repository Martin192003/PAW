package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.User.Role;
import ar.edu.itba.paw.model.User.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.RequestParam;
import ar.edu.itba.paw.interfaces.UserService;

import java.util.UUID;

@Controller
public class HelloWorldController {

    private UserService us; // Inyección del servicio

    @RequestMapping("/")
    public ModelAndView helloWorld() {
        return  new ModelAndView("index");
    }
    @Autowired
    public HelloWorldController(UserService us){
        this.us = us;
    }

    @RequestMapping("/user")
    public ModelAndView index(@RequestParam(value = "userId", required = true) final UUID id) {
        final ModelAndView mav = new ModelAndView("user");
        mav.addObject("user", us.findById(id));
        return mav;
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
        final Role userRole = Role.valueOf(role.toUpperCase()); // Convertir a mayúsculas
        final User u = us.create(username, password, surname, email, cellphone, userRole);
        return new ModelAndView("redirect:/user?userId=" + u.getId());
    }

}
