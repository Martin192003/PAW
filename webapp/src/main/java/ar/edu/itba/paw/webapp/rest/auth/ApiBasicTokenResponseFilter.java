package ar.edu.itba.paw.webapp.rest.auth;

import ar.edu.itba.paw.webapp.auth.ApiTokenHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Component
@Provider
public class ApiBasicTokenResponseFilter implements ContainerResponseFilter {

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        ApiTokenHeaders.addTo(responseContext.getHeaders(), request);
    }
}
