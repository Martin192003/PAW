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

        Integer status = (statusAttr instanceof Integer) ? (Integer) statusAttr : null;
        String message = (messageAttr != null) ? messageAttr.toString() : null;

        /* String friendly = (message == null || message.isBlank()) PREGUNTAR ESTO TIRA MUCHA DATA EN EL ERROR 500, ES UN PROBLEMA DE SEGURIDAD
                ? defaultMessage(status)
                : message; */

        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("status", status);
        mav.addObject("message", defaultMessage(status));
        return mav;
    }

    private String defaultMessage(Integer code) {
        if (code == null) return "Ocurrió un error al procesar tu solicitud.";
        return switch (code) {
            case 400 -> "Solicitud inválida.";
            case 401 -> "No autorizado.";
            case 403 -> "Acceso denegado.";
            case 404 -> "Página no encontrada.";
            case 500 -> "Error interno del servidor.";
            default   -> "Se produjo un error (" + code + ").";
        };
    }
}
