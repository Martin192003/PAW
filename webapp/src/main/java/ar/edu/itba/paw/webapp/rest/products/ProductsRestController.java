package ar.edu.itba.paw.webapp.rest.products;

import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.dto.ProductDto;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.ProductService;
import ar.edu.itba.paw.webapp.rest.ApiMediaTypes;
import ar.edu.itba.paw.webapp.rest.CollectionLinks;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/clubs/{clubId}/menu")
@Produces({ApiMediaTypes.PRODUCT, ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
@Consumes({ApiMediaTypes.PRODUCT, ApiMediaTypes.APPLICATION_JSON})
public class ProductsRestController {

    private final ProductService productService;
    private final ProductDtoAssembler productDtoAssembler;
    private final ClubService clubService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ProductsRestController(final ProductService productService,
                                  final ProductDtoAssembler productDtoAssembler,
                                  final ClubService clubService,
                                  final AuthenticatedUserResolver authenticatedUserResolver) {
        this.productService = productService;
        this.productDtoAssembler = productDtoAssembler;
        this.clubService = clubService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GET
    @Produces({ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
    public Response listProducts(@PathParam("clubId") String clubId,
                                 @Context SecurityContext securityContext) {
        UUID clubUuid;
        try {
            clubUuid = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("club.id.invalid"))
                    .build();
        }

        var products = productService.findByClubID(clubUuid);
        if (products == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("menu.notfound"))
                    .build();
        }

        boolean canManage = canManageClub(clubUuid, securityContext);
        List<LinkDto> body = CollectionLinks.items(products, product -> uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/menu/{productId}")
                .resolveTemplate("clubId", clubUuid)
                .resolveTemplate("productId", product.getProductId())
                .build());
        Response.ResponseBuilder response = Response.ok(new GenericEntity<List<LinkDto>>(body) {})
                .header("X-Total-Count", body.size());
        if (canManage) {
            response.header("Cache-Control", "private, no-store")
                    .header("Pragma", "no-cache")
                    .header("Vary", "Authorization");
        } else {
            response.header("Cache-Control", "no-cache");
        }
        return response.build();
    }

    @POST
    @Consumes({ApiMediaTypes.PRODUCT, ApiMediaTypes.APPLICATION_JSON})
    public Response createProduct(@PathParam("clubId") String clubId,
                                  ProductDto body,
                                  @Context SecurityContext securityContext) {
        UUID clubUuid;
        try {
            clubUuid = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("club.id.invalid"))
                    .build();
        }

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubUuid, user.getId());

        if (body == null ||
                isBlank(body.getProductName()) ||
                isBlank(body.getDescription()) ||
                body.getBasePrice() == null ||
                body.getProductType() == null
        ) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("fields.required"))
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
        return Response.created(uri)
                .entity(productDtoAssembler.toDto(uriInfo, clubUuid, created, true))
                .build();
    }

    @GET
    @Path("/{productId}")
    public Response getProduct(@PathParam("clubId") String clubId,
                               @PathParam("productId") String productId,
                               @Context SecurityContext securityContext) {
        UUID clubUuid;
        UUID productUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            productUuid = UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("club_or_product.id.invalid"))
                    .build();
        }

        List<Product> products = productService.findByClubID(clubUuid);
        if (products == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("product.notfound"))
                    .build();
        }

        boolean canManage = canManageClub(clubUuid, securityContext);
        return products.stream()
                .filter(product -> productUuid.equals(product.getProductId()))
                .findFirst()
                .map(product -> Response.ok(productDtoAssembler.toDto(uriInfo, clubUuid, product, canManage)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorDto("product.notfound"))
                        .build());
    }

    @PUT
    @Path("/{productId}")
    @Consumes({ApiMediaTypes.PRODUCT, ApiMediaTypes.APPLICATION_JSON})
    public Response updateProduct(@PathParam("clubId") String clubId,
                                  @PathParam("productId") String productId,
                                  ProductDto body,
                                  @Context SecurityContext securityContext) {
        UUID clubUuid, productUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            productUuid = UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("club_or_product.id.invalid"))
                    .build();
        }

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubUuid, user.getId());

        if (!productBelongsToClub(clubUuid, productUuid)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("product.notfound"))
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
                    .entity(new ErrorDto("fields.required"))
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
                    .entity(new ErrorDto("product_or_club.notfound"))
                    .build();
        }

        ProductDto responseBody = productDtoAssembler.toDto(uriInfo, clubUuid, updated, true);
        return Response.ok(responseBody).build();
    }

    @DELETE
    @Path("/{productId}")
    public Response deleteProduct(@PathParam("clubId") String clubId,
                                  @PathParam("productId") String productId,
                                  @Context SecurityContext securityContext) {
        UUID clubUuid, productUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            productUuid = UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("club_or_product.id.invalid"))
                    .build();
        }

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubUuid, user.getId());

        if (!productBelongsToClub(clubUuid, productUuid)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("product.notfound"))
                    .build();
        }

        boolean removed = productService.delete(productUuid);
        if (!removed) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("product.notfound"))
                    .build();
        }

        return Response.noContent().build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean productBelongsToClub(UUID clubId, UUID productId) {
        return productService.findByClubID(clubId).stream()
                .anyMatch(product -> productId.equals(product.getProductId()));
    }

    private boolean canManageClub(UUID clubId, SecurityContext securityContext) {
        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return false;
        }
        try {
            clubService.assertAdmin(clubId, user.getId());
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}

