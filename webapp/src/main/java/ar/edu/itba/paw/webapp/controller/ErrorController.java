package ar.edu.itba.paw.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class ErrorController {

    @RequestMapping(value = "/error")
    public ModelAndView handleError(final HttpServletRequest request) {
        Object statusAttr      = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageAttr     = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        /*Integer status = (statusAttr instanceof Integer) ? (Integer) statusAttr : null;
        String message = (messageAttr != null) ? messageAttr.toString() : null;*/

        /* String friendly = (message == null || message.isBlank()) PREGUNTAR ESTO TIRA MUCHA DATA EN EL ERROR 500, ES UN PROBLEMA DE SEGURIDAD
                ? defaultMessage(status)
                : message; */

        Integer status = (statusAttr instanceof Integer) ? (Integer) statusAttr : null;

        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("status", status);
        mav.addObject("message", defaultMessage(status));
        return mav;
    }

    private String defaultMessage(Integer code) {
        if (code == null) return "error.processing";
        return switch (code) {
            case 400 -> "error.400";
            case 401 -> "error.401";
            case 403 -> "error.403";
            case 404 -> "error.404";
            case 500 -> "error.500";
            default   -> "error.default";
        };
    }
}
