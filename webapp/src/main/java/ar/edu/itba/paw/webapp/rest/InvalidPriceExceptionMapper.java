package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.exceptions.InvalidPriceException;
import ar.edu.itba.paw.model.dto.ErrorDto;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidPriceExceptionMapper implements ExceptionMapper<InvalidPriceException> {
    @Override
    public Response toResponse(InvalidPriceException ex) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto(ex.getMessage()))
                .build();
    }
}
