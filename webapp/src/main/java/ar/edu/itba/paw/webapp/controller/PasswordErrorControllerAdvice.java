package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.exceptions.ValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Order(Ordered.HIGHEST_PRECEDENCE - 1)
@ControllerAdvice(assignableTypes = PasswordController.class)
public class PasswordErrorControllerAdvice {

    @ExceptionHandler(ValidationException.class)
    public ModelAndView handleValidation(ValidationException ex, HttpServletRequest request) {
        ModelAndView mv = new ModelAndView("resetPasswordResult");
        mv.addObject("error", ex.getMessage());
        mv.addObject("token", request.getParameter("token"));
        return mv;
    }
}

