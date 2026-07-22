package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.LinkDto;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Component
@Path("/")
@Produces({ApiMediaTypes.API_ROOT, ApiMediaTypes.APPLICATION_JSON})
public class ApiRootController {

    @Context
    private UriInfo uriInfo;

    @GET
    public Response root() {
        List<LinkDto> links = List.of(
                LinkBuilder.link("self", uriInfo.getBaseUriBuilder().build(), "GET"),
                LinkBuilder.link("clubs", uriInfo.getBaseUriBuilder().path("clubs").build(), "GET"),
                LinkBuilder.link("events", uriInfo.getBaseUriBuilder().path("events").build(), "GET"),
                LinkBuilder.link("users", uriInfo.getBaseUriBuilder().path("users").build(), "POST"),
                LinkBuilder.link("bookings", uriInfo.getBaseUriBuilder().path("bookings").build(), "GET")
        );
        return Response.ok(new GenericEntity<List<LinkDto>>(links) {})
                .header("Cache-Control", "no-cache")
                .build();
    }
}
