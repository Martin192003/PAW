package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.exceptions.TableNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class TableNotFoundExceptionMapper implements ExceptionMapper<TableNotFoundException> {
    @Override
    public Response toResponse(TableNotFoundException ex) {
        return Response
                .status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto(ex.getMessage()))
                .build();
    }
}
