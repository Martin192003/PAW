package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.ErrorDto;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Red de contencion generica para violaciones de constraints de base de
 * datos que no se validaron proactivamente antes del insert/update (por
 * ejemplo, un unique constraint). Sin este mapper, Jersey deja propagar la
 * excepcion y el servlet container devuelve un 500 con stacktrace crudo en
 * vez de una respuesta manejable por el frontend.
 *
 * Caso conocido: club_tables_club_id_table_number_key (numero de mesa
 * duplicado dentro del mismo club).
 */
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
