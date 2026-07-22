package ar.edu.itba.paw.webapp.config;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;


@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request,
                       ContainerResponseContext response) throws IOException {
        response.getHeaders().putSingle("Access-Control-Expose-Headers",
                "Location, Link, X-Total-Count, Paw-Auth-Token, Paw-Token-Type, Paw-Token-Expires-In");
    }
}
