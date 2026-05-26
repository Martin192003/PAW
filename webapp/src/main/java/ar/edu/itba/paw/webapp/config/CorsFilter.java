package ar.edu.itba.paw.webapp.config;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Filtro CORS para recursos Jersey (/api/**).
 * Solo @Provider — no @Component, para no colisionar con el bean "corsFilter"
 * que Spring Security espera de tipo org.springframework.web.filter.CorsFilter.
 *
 * El CORS principal (configurable por properties) está en WebAuthConfig#corsConfigurationSource,
 * que Spring Security aplica a todos los requests incluidos los de Jersey.
 * Este filtro agrega los headers de exposición adicionales que Jersey necesita para
 * que el frontend pueda leer X-Access-Token, Location, Link, etc.
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request,
                       ContainerResponseContext response) throws IOException {

        // Solo exponer headers custom — el Access-Control-Allow-Origin
        // lo maneja Spring Security según cors.allowed-origins en application.properties.
        response.getHeaders().putSingle("Access-Control-Expose-Headers",
                "X-Access-Token, X-Token-Type, X-Expires-In, X-User-Href, Location, Link, X-Total-Count");
    }
}
