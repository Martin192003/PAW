package ar.edu.itba.paw.webapp.rest.users;

import ar.edu.itba.paw.model.dto.UserCreateRequestDto;
import ar.edu.itba.paw.model.dto.UserDto;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRestController {

    private static final Map<UUID, UserDto> USERS = new ConcurrentHashMap<>();

    @Context
    private UriInfo uriInfo;

    @GET
    @Path("/{userId}")
    public Response getUser(@PathParam("userId") String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "userId inválido"))
                    .build();
        }

        UserDto user = USERS.get(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Usuario no encontrado"))
                    .build();
        }

        return Response.ok(user).build();
    }

    @POST
    public Response createUser(UserCreateRequestDto body) {

        if (body == null ||
                isBlank(body.getUsername()) ||
                isBlank(body.getName()) ||
                isBlank(body.getSurname()) ||
                isBlank(body.getEmail()) ||
                isBlank(body.getPassword()) ||
                isBlank(body.getCellphone())
        ) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Faltan campos requeridos"))
                    .build();
        }

        UUID id = UUID.randomUUID();

        UserDto created = new UserDto();
        created.setUserId(id);
        created.setUsername(body.getUsername());
        created.setName(body.getName());
        created.setSurname(body.getSurname());
        created.setEmail(body.getEmail());
        created.setCellphone(body.getCellphone());
        created.setRole("USER");
        created.setVerified(false);
        created.setLocale(body.getLocale());
        created.setCreatedAt(OffsetDateTime.now());

        USERS.put(id, created);

        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(id.toString())
                .build();

        return Response.created(uri)
                .entity(created)
                .build();
    }

    @PUT
    @Path("/{userId}")
    public Response updateUser(@PathParam("userId") String userId, UserCreateRequestDto body) {

        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "userId inválido"))
                    .build();
        }

        UserDto existing = USERS.get(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Usuario no encontrado"))
                    .build();
        }

        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Body requerido"))
                    .build();
        }

        if (!isBlank(body.getUsername())) existing.setUsername(body.getUsername());
        if (!isBlank(body.getName())) existing.setName(body.getName());
        if (!isBlank(body.getSurname())) existing.setSurname(body.getSurname());
        if (!isBlank(body.getEmail())) existing.setEmail(body.getEmail());
        if (!isBlank(body.getCellphone())) existing.setCellphone(body.getCellphone());
        if (!isBlank(body.getLocale())) existing.setLocale(body.getLocale());

        USERS.put(id, existing);

        return Response.ok(existing).build();
    }

    @DELETE
    @Path("/{userId}")
    public Response deleteUser(@PathParam("userId") String userId) {

        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "userId inválido"))
                    .build();
        }

        UserDto removed = USERS.remove(id);
        if (removed == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Usuario no encontrado"))
                    .build();
        }

        return Response.noContent().build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
