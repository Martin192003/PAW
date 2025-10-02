package ar.edu.itba.paw.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UnverifiedController {

    @GetMapping("/unverified")
    public String showUnverifiedPage(@RequestParam(required = false) String username, Model model) {
        if (username != null) {
            model.addAttribute("username", username);
        }
        return "unverified";
    }
}