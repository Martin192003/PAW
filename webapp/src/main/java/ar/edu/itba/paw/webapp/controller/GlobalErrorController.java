package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.time.format.DateTimeParseException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class GlobalErrorController {

    private static final String VIEW = "error/error";
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalErrorController.class);

    /* ------------ 401 (no autenticado) ------------ */
    @ExceptionHandler(UnauthorizedException.class)
    public ModelAndView unauthorized(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.UNAUTHORIZED, ex, req);
    }

    /* ------------ 403 (sin permisos) ------------ */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView forbidden(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.FORBIDDEN, ex, req);
    }

    /* ------------ 404 ------------ */
    @ExceptionHandler({
            NotFoundException.class,
            UserNotFoundException.class,
            BookingNotFoundException.class,
            PhotoNotFoundException.class,
            TableNotFoundException.class
    })
    public ModelAndView notFound(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.NOT_FOUND, ex, req);
    }

    /* ------------ 409 (conflictos de negocio) ------------ */
    @ExceptionHandler({
            DuplicateEntityException.class,
            InvalidStatusTransitionException.class,
            TableAlreadyBookedException.class,
            InvalidBookingStateException.class
    })
    public ModelAndView conflict(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.CONFLICT, ex, req);
    }

    /* ------------ 422 (validación / dominio) ------------ */
    @ExceptionHandler({
            ValidationException.class,
            InvalidDateRangeException.class,
            BadRequestException.class, // si preferís 400, sacalo de acá
            DateTimeParseException.class,
            MethodArgumentNotValidException.class,
            BindException.class
    })
    public ModelAndView unprocessable(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.UNPROCESSABLE_ENTITY, ex, req);
    }

    /* ------------ 400 (request mal formado) ------------ */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ModelAndView badRequest(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.BAD_REQUEST, ex, req);
    }

    /* ------------ 502/503 (servicios externos) ------------ */
    @ExceptionHandler(EmailWasntSentException.class)
    public ModelAndView badGateway(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.BAD_GATEWAY, ex, req);
    }

    /* ------------ 500 fallback ------------ */
    @ExceptionHandler(Exception.class)
    public ModelAndView serverError(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.INTERNAL_SERVER_ERROR, ex, req);
    }

    private ModelAndView view(HttpStatus status, Exception ex, HttpServletRequest req) {
        // Log level según familia de código
        if (status.is5xxServerError()) {
            LOGGER.error("Handled {} {} {} -> {}", status.value(), req.getMethod(), req.getRequestURI(), ex.toString(), ex);
        } else {
            LOGGER.warn ("Handled {} {} {} -> {}", status.value(), req.getMethod(), req.getRequestURI(), ex.toString());
        }

        ModelAndView mv = new ModelAndView(VIEW);
        mv.setStatus(status);
        mv.addObject("status", status.value());
        mv.addObject("error", status.getReasonPhrase());

        // Para 4xx mostramos el mensaje de negocio; para 5xx, genérico
        String message = status.is4xxClientError()
                ? (ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Solicitud inválida." : ex.getMessage())
                : "Ha ocurrido un error inesperado.";
        mv.addObject("message", message);

        mv.addObject("path", req.getRequestURI());
        return mv;
    }
}
