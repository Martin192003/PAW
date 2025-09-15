package ar.edu.itba.paw.webapp.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/testerror")
public class ErrorTestController {

    @GetMapping("/400") public void e400(HttpServletResponse res) throws IOException { res.sendError(400, "Bad request"); }
    @GetMapping("/401") public void e401(HttpServletResponse res) throws IOException { res.sendError(401, "Unauthorized"); }
    @GetMapping("/403") public void e403(HttpServletResponse res) throws IOException { res.sendError(403, "Forbidden"); }
    @GetMapping("/404") public void e404(HttpServletResponse res) throws IOException { res.sendError(404, "Not Found"); }
    @GetMapping("/500") public String e500() { throw new RuntimeException("Forced 500 error"); }
}