package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.exceptions.TableAlreadyBookedException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class TableAlreadyBookedExceptionMapper implements ExceptionMapper<TableAlreadyBookedException> {
    @Override
    public Response toResponse(TableAlreadyBookedException ex) {
        return Response
                .status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto(ex.getMessage()))
                .build();
    }
}
