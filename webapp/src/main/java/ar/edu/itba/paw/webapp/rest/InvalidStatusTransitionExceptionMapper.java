package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.exceptions.InvalidStatusTransitionException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidStatusTransitionExceptionMapper implements ExceptionMapper<InvalidStatusTransitionException> {
    @Override
    public Response toResponse(InvalidStatusTransitionException ex) {
        return Response
                .status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto(ex.getMessage()))
                .build();
    }
}
