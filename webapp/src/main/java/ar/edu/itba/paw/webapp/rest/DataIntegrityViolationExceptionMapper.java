package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.ErrorDto;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


@Provider
public class DataIntegrityViolationExceptionMapper implements ExceptionMapper<DataIntegrityViolationException> {
    @Override
    public Response toResponse(DataIntegrityViolationException ex) {
        return Response
                .status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto(resolveMessage(ex)))
                .build();
    }

    private String resolveMessage(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof ConstraintViolationException) {
            String constraintName = ((ConstraintViolationException) cause).getConstraintName();
            if (constraintName != null && constraintName.contains("table_number")) {
                return "table.duplicateNumber";
            }
        }
        return "entity.duplicate";
    }
}
