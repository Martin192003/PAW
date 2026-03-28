package ar.edu.itba.paw.webapp.rest.products;

import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.dto.ProductCreateRequestDto;
import ar.edu.itba.paw.model.dto.ProductUpdateRequestDto;
import ar.edu.itba.paw.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/clubs/{clubId}/menu")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductsRestController {

    private final ProductService productService;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ProductsRestController(final ProductService productService) {
        this.productService = productService;
    }

    @GET
    public Response listProducts(@PathParam("clubId") String clubId) {
        UUID clubUuid;
        try {
            clubUuid = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
                    .build();
        }

        List<Product> products = productService.findByClubID(clubUuid);
        if (products == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "El club no tiene menú cargado"))
                    .build();
        }

        return Response.ok(new GenericEntity<List<Product>>(products) {}).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createProduct(@PathParam("clubId") String clubId, ProductCreateRequestDto body) {
        UUID clubUuid;
        try {
            clubUuid = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
                    .build();
        }

        if (body == null ||
                isBlank(body.getProductName()) ||
                isBlank(body.getDescription()) ||
                body.getBasePrice() == null ||
                body.getProductType() == null
        ) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Missing required fields"))
                    .build();
        }

        Product created = productService.create(
                clubUuid,
                body.getProductName(),
                body.getDescription(),
                body.getBasePrice(),
                body.getProductType(),
                body.getIsAvailable() != null ? body.getIsAvailable() : true
        );
        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(created.getProductId().toString())
                .build();
        return Response.created(uri).entity(created).build();
    }

    @PUT
    @Path("/{productId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProduct(@PathParam("clubId") String clubId,
                                  @PathParam("productId") String productId,
                                  ProductUpdateRequestDto body) {
        UUID clubUuid, productUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            productUuid = UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId o productId inválido"))
                    .build();
        }

        if (body == null ||
                isBlank(body.getProductName()) ||
                isBlank(body.getDescription()) ||
                body.getBasePrice() == null ||
                body.getProductType() == null ||
                body.getIsAvailable() == null
        ) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Missing required fields"))
                    .build();
        }

        Product updated = productService.update(
                productUuid,
                clubUuid,
                body.getProductName(),
                body.getDescription(),
                body.getBasePrice(),
                body.getProductType(),
                body.getIsAvailable()
        );
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Producto o club no encontrado"))
                    .build();
        }

        return Response.ok(updated).build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}