package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalErrorController {

    private static final String VIEW = "error/error";
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalErrorController.class);

    // --- 400
    @ExceptionHandler({
            BadRequestException.class,
            ValidationException.class,
            InvalidClubException.class,
            InvalidBookingStateException.class
    })
    public ModelAndView badRequest(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.BAD_REQUEST, ex, req);
    }

    // --- 403
    @ExceptionHandler({UnauthorizedException.class, AccessDeniedException.class})
    public ModelAndView forbidden(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.FORBIDDEN, ex, req);
    }

    // --- 404
    @ExceptionHandler({
            NotFoundException.class,
            UserNotFoundException.class,
            BookingNotFoundException.class,
            PhotoNotFoundException.class
    })
    public ModelAndView notFound(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.NOT_FOUND, ex, req);
    }

    // --- 409
    @ExceptionHandler(DuplicateEntityException.class)
    public ModelAndView conflict(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.CONFLICT, ex, req);
    }

    // --- 500 DEFAULT
    @ExceptionHandler(Exception.class)
    public ModelAndView serverError(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.INTERNAL_SERVER_ERROR, ex, req);
    }

    private ModelAndView view(HttpStatus status, Exception ex, HttpServletRequest req) {
        LOGGER.error("Handled {} at {} -> {}", status, req.getRequestURI(), ex.toString(), ex);
        ModelAndView mv = new ModelAndView(VIEW);
        mv.setStatus(status);
        mv.addObject("status", status.value());
        mv.addObject("error", status.getReasonPhrase());
        mv.addObject("message", "Ha ocurrido un error inesperado.");
        mv.addObject("path", req.getRequestURI());
        return mv;
    }
}