package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.ErrorDto;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnhandledExceptionMapperTest {

    private final UnhandledExceptionMapper mapper = new UnhandledExceptionMapper();

    @Test
    public void testUnexpectedExceptionReturnsInternalServerError() {
        Response response = mapper.toResponse(new RuntimeException("boom"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertTrue(response.getEntity() instanceof ErrorDto);
        assertEquals("Ocurrio un error inesperado.", ((ErrorDto) response.getEntity()).getMessage());
    }

    @Test
    public void testWebApplicationExceptionPreservesClientErrorStatus() {
        WebApplicationException exception = new WebApplicationException(
                Response.status(Response.Status.NOT_FOUND).build()
        );

        Response response = mapper.toResponse(exception);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertTrue(response.getEntity() instanceof ErrorDto);
        assertEquals("Recurso no encontrado.", ((ErrorDto) response.getEntity()).getMessage());
    }
}
