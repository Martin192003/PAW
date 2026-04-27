package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.ErrorDto;
import org.glassfish.jersey.server.ParamException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ParamExceptionMapper implements ExceptionMapper<ParamException> {
    @Override
    public Response toResponse(ParamException ex) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto("Parametro invalido: " + ex.getParameterName()))
                .build();
    }
}
