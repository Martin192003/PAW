package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.webapp.form.AddProductForm;
import  ar.edu.itba.paw.webapp.component.ClubHandlerModelBuilder;
import ar.edu.itba.paw.webapp.form.EditProductForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import javax.servlet.http.HttpServletRequest;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class GlobalErrorControllerAdvice {

    private static final String VIEW = "error/error";
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalErrorControllerAdvice.class);

    private final MessageSource messageSource;
    private final ClubHandlerModelBuilder clubHandlerModelBuilder;

    private static final Map<Class<? extends Exception>, String> EXCEPTION_CODE_MAP = new LinkedHashMap<>();
    static {
        // 400
        EXCEPTION_CODE_MAP.put(MissingServletRequestParameterException.class, "error.missing_parameter");
        EXCEPTION_CODE_MAP.put(MethodArgumentTypeMismatchException.class, "error.type_mismatch");
        EXCEPTION_CODE_MAP.put(ConversionFailedException.class,            "error.type_mismatch");
        EXCEPTION_CODE_MAP.put(TypeMismatchException.class,                "error.type_mismatch");
        EXCEPTION_CODE_MAP.put(NumberFormatException.class,                "error.type_mismatch");
        EXCEPTION_CODE_MAP.put(BadRequestException.class,                  "error.bad_request");

        // 401/403
        EXCEPTION_CODE_MAP.put(UnauthorizedException.class,                "error.unauthorized");
        EXCEPTION_CODE_MAP.put(AccessDeniedException.class,                "error.forbidden");

        // 404
        EXCEPTION_CODE_MAP.put(NotFoundException.class,                    "error.not_found");
        EXCEPTION_CODE_MAP.put(UserNotFoundException.class,                "error.not_found");
        EXCEPTION_CODE_MAP.put(BookingNotFoundException.class,             "error.not_found");
        EXCEPTION_CODE_MAP.put(PhotoNotFoundException.class,               "error.not_found");
        EXCEPTION_CODE_MAP.put(TableNotFoundException.class,               "error.not_found");

        // 409
        EXCEPTION_CODE_MAP.put(DuplicateEntityException.class,             "error.conflict");
        EXCEPTION_CODE_MAP.put(InvalidStatusTransitionException.class,     "error.conflict");
        EXCEPTION_CODE_MAP.put(TableAlreadyBookedException.class,          "error.conflict");
        EXCEPTION_CODE_MAP.put(InvalidBookingStateException.class,         "error.conflict");

        // 422
        EXCEPTION_CODE_MAP.put(ValidationException.class,                  "error.validation");
        EXCEPTION_CODE_MAP.put(InvalidDateRangeException.class,            "error.invalid_date_range");
        EXCEPTION_CODE_MAP.put(MethodArgumentNotValidException.class,      "error.validation");
        EXCEPTION_CODE_MAP.put(BindException.class,                        "error.validation");
        EXCEPTION_CODE_MAP.put(InvalidCapacityException.class,             "error.validation");
        EXCEPTION_CODE_MAP.put(InvalidTableNumberException.class,          "error.validation");
        EXCEPTION_CODE_MAP.put(InvalidPriceException.class,                "error.validation");

        // 502
        EXCEPTION_CODE_MAP.put(EmailWasntSentException.class,              "error.bad_gateway");
    }

    public GlobalErrorControllerAdvice(MessageSource messageSource, ClubHandlerModelBuilder clubHandlerModelBuilder) {
        this.messageSource = messageSource;
        this.clubHandlerModelBuilder = clubHandlerModelBuilder;
    }

    /* ------------ 400 (request mal formado) ------------ */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, // @RequestParam enum/boolean/number inválido
            ConversionFailedException.class,
            TypeMismatchException.class,
            BadRequestException.class,
            NumberFormatException.class
    })
    public ModelAndView badRequest(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.BAD_REQUEST, ex, req);
    }

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
            DateTimeParseException.class,
            MethodArgumentNotValidException.class,
            BindException.class,
            InvalidCapacityException.class,
            InvalidTableNumberException.class,
    })
    public ModelAndView unprocessable(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.UNPROCESSABLE_ENTITY, ex, req);
    }



    @ExceptionHandler(NoHandlerFoundException.class)
    public ModelAndView noHandler(NoHandlerFoundException ex,
                                  HttpServletRequest req) {
        return view(HttpStatus.NOT_FOUND, ex, req);
    }

    /* ------------ 500 fallback ------------ */
    @ExceptionHandler(Exception.class)
    public ModelAndView serverError(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.INTERNAL_SERVER_ERROR, ex, req);
    }

    /* ------------ 502/503 (servicios externos) ------------ */
    @ExceptionHandler(EmailWasntSentException.class)
    public ModelAndView badGateway(Exception ex, HttpServletRequest req) {
        return view(HttpStatus.BAD_GATEWAY, ex, req);
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

        String genericMessage = resolveGenericMessage(status, ex);
        mv.addObject("errorMessage", genericMessage);
        mv.addObject("path", req.getRequestURI());
        return mv;
    }

    @ExceptionHandler(MaxProductsReachedException.class)
    public ModelAndView handleMaxProducts(MaxProductsReachedException ex) {
        UUID clubId = ex.getClubId();

        AddProductForm form = new AddProductForm();
        form.setClubId(clubId);

        BindingResult br = new BeanPropertyBindingResult(form, "productForm");
        br.addError(new ObjectError("productForm", "{max_products}"));

        ExtendedModelMap model = new ExtendedModelMap();
        clubHandlerModelBuilder.populateForAdd(model, clubId, form);
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "productForm", br);

        ModelAndView mv = new ModelAndView("clubHandler", model);
        mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        return mv;
    }

    @ExceptionHandler(InvalidPriceException.class)
    public ModelAndView handleEditInvalidPrice(InvalidPriceException ex) {
        UUID clubId = ex.getClubId();

        EditProductForm form = new EditProductForm();
        form.setClubId(clubId);

        BindingResult br = new BeanPropertyBindingResult(form, "editProductForm");
        br.addError(new ObjectError("editProductForm", "{price_max_product}"));

        ExtendedModelMap model = new ExtendedModelMap();
        clubHandlerModelBuilder.populateForEditErrors(model, clubId, form);
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "editProductForm", br);
        model.addAttribute("openModal", "editProduct");

        ModelAndView mv = new ModelAndView("clubHandler", model);
        mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        return mv;
    }


    private String resolveGenericMessage(HttpStatus status, Exception ex) {
        Locale locale = LocaleContextHolder.getLocale();
        String code = resolveMessageCode(status, ex);
        return messageSource.getMessage(code, null, code, locale);
    }

    private String resolveMessageCode(HttpStatus status, Exception ex) {
        for (Map.Entry<Class<? extends Exception>, String> e : EXCEPTION_CODE_MAP.entrySet()) {
            if (e.getKey().isAssignableFrom(ex.getClass())) {
                return e.getValue();
            }
        }
        return switch (status) {
            case BAD_REQUEST -> "error.bad_request";
            case UNAUTHORIZED -> "error.unauthorized";
            case FORBIDDEN -> "error.forbidden";
            case NOT_FOUND -> "error.not_found";
            case CONFLICT -> "error.conflict";
            case UNPROCESSABLE_ENTITY -> "error.unprocessable";
            case BAD_GATEWAY -> "error.bad_gateway";
            default -> status.is5xxServerError() ? "error.internal" : "error.generic";
        };
    }
}

