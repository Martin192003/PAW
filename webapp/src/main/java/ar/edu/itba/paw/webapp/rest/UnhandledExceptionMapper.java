package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnhandledExceptionMapper.class);
    private static final String INTERNAL_ERROR_MESSAGE = "Ocurrio un error inesperado.";

    @Override
    public Response toResponse(Throwable ex) {
        int status = statusCode(ex);
        if (status >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            LOGGER.error("Unhandled API exception", ex);
        }

        return Response
                .status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorDto(messageForStatus(status)))
                .build();
    }

    private int statusCode(Throwable ex) {
        if (ex instanceof WebApplicationException webException && webException.getResponse() != null) {
            int status = webException.getResponse().getStatus();
            if (status >= 400) {
                return status;
            }
        }
        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    private String messageForStatus(int status) {
        if (status == Response.Status.NOT_FOUND.getStatusCode()) {
            return "Recurso no encontrado.";
        }
        if (status == Response.Status.METHOD_NOT_ALLOWED.getStatusCode()) {
            return "Metodo no permitido.";
        }
        if (status == Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode()) {
            return "Formato de contenido no soportado.";
        }
        if (status == Response.Status.BAD_REQUEST.getStatusCode()) {
            return "Solicitud invalida.";
        }
        if (status == Response.Status.UNAUTHORIZED.getStatusCode()) {
            return "No autenticado.";
        }
        if (status == Response.Status.FORBIDDEN.getStatusCode()) {
            return "No tienes permisos para acceder a este recurso.";
        }
        return INTERNAL_ERROR_MESSAGE;
    }
}
