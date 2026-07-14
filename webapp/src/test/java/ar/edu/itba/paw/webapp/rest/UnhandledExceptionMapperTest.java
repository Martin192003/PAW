package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.ErrorDto;
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnhandledExceptionMapperTest {

    private final UnhandledExceptionMapper mapper = new UnhandledExceptionMapper();

    @Test
    public void unexpectedExceptionReturnsJsonInternalServerError() {
        Response response = mapper.toResponse(new IllegalStateException("database detail"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertTrue(response.getEntity() instanceof ErrorDto);
        assertEquals("Ocurrio un error inesperado.", ((ErrorDto) response.getEntity()).getMessage());
    }

    @Test
    public void webApplicationExceptionKeepsStatusCode() {
        Response response = mapper.toResponse(new NotFoundException());

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertTrue(response.getEntity() instanceof ErrorDto);
        assertEquals("Recurso no encontrado.", ((ErrorDto) response.getEntity()).getMessage());
    }
}
