package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.ErrorDto;
import org.springframework.dao.DuplicateKeyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DuplicateKeyExceptionMapper implements ExceptionMapper<DuplicateKeyException> {
    @Override
    public Response toResponse(DuplicateKeyException ex) {
        return Response
                .status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto(ex.getMessage()))
                .build();
    }
}
