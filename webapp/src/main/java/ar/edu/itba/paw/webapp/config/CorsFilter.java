package ar.edu.itba.paw.webapp.config;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Filtro CORS para recursos Jersey (/api/**).
 *
 * El CORS principal esta en WebAuthConfig#corsConfigurationSource.
 * Este filtro solo expone headers de navegacion y paginacion que el frontend
 * necesita leer desde respuestas JAX-RS.
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request,
                       ContainerResponseContext response) throws IOException {
        response.getHeaders().putSingle("Access-Control-Expose-Headers",
                "Location, Link, X-Total-Count");
    }
}
