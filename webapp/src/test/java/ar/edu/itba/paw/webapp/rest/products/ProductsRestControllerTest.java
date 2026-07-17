package ar.edu.itba.paw.webapp.rest.products;

import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.ProductDto;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.ProductService;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductsRestControllerTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private ProductService productService;
    private ClubService clubService;
    private AuthenticatedUserResolver authenticatedUserResolver;
    private ProductsRestController controller;

    @Before
    public void setUp() {
        productService = mock(ProductService.class);
        clubService = mock(ClubService.class);
        authenticatedUserResolver = mock(AuthenticatedUserResolver.class);
        controller = new ProductsRestController(
                productService, new ProductDtoAssembler(), clubService, authenticatedUserResolver);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));
        when(uriInfo.getAbsolutePathBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/clubs/" + CLUB_ID + "/menu"));
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);
    }

    @Test
    public void testListProducts_InvalidClubId_ReturnsBadRequest() {
        Response response = controller.listProducts("not-a-uuid", mock(SecurityContext.class));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testListProducts_ReturnsEmbeddedCollection() {
        when(productService.findByClubID(CLUB_ID)).thenReturn(List.of(product(PRODUCT_ID)));

        Response response = controller.listProducts(CLUB_ID.toString(), mock(SecurityContext.class));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("1", response.getHeaderString("X-Total-Count"));
        Object entity = response.getEntity();
        assertTrue(entity instanceof List);
        @SuppressWarnings("unchecked")
        List<ProductDto> products = (List<ProductDto>) entity;
        assertEquals(PRODUCT_ID, products.get(0).getProductId());
        assertTrue(products.get(0).getLinks().stream().anyMatch(link -> "self".equals(link.getRel())));
    }

    @Test
    public void testCreateProduct_Unauthenticated_ReturnsUnauthorized() {
        when(authenticatedUserResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        Response response = controller.createProduct(CLUB_ID.toString(), validProductDto(), mock(SecurityContext.class));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateProduct_MissingFields_ReturnsBadRequest() {
        User admin = user(UUID.randomUUID());
        when(authenticatedUserResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(admin);

        ProductDto body = new ProductDto();
        body.setProductName("Solo el nombre");

        Response response = controller.createProduct(CLUB_ID.toString(), body, mock(SecurityContext.class));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateProduct_Success_ReturnsCreated() {
        User admin = user(UUID.randomUUID());
        when(authenticatedUserResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(admin);
        Product created = product(PRODUCT_ID);
        when(productService.create(CLUB_ID, "Fernet", "Con coca", BigDecimal.TEN, ProductType.TRAGOS, true))
                .thenReturn(created);

        Response response = controller.createProduct(CLUB_ID.toString(), validProductDto(), mock(SecurityContext.class));

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertTrue(response.getLocation().toString().contains(PRODUCT_ID.toString()));
        assertTrue(response.getEntity() instanceof ProductDto);
    }

    @Test
    public void testDeleteProduct_NotBelongingToClub_ReturnsNotFound() {
        User admin = user(UUID.randomUUID());
        when(authenticatedUserResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(admin);
        when(productService.findByClubID(CLUB_ID)).thenReturn(List.of());

        Response response = controller.deleteProduct(CLUB_ID.toString(), PRODUCT_ID.toString(), mock(SecurityContext.class));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteProduct_Success_ReturnsNoContent() {
        User admin = user(UUID.randomUUID());
        when(authenticatedUserResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(admin);
        when(productService.findByClubID(CLUB_ID)).thenReturn(List.of(product(PRODUCT_ID)));
        when(productService.delete(PRODUCT_ID)).thenReturn(true);

        Response response = controller.deleteProduct(CLUB_ID.toString(), PRODUCT_ID.toString(), mock(SecurityContext.class));

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    private static ProductDto validProductDto() {
        ProductDto dto = new ProductDto();
        dto.setProductName("Fernet");
        dto.setDescription("Con coca");
        dto.setBasePrice(BigDecimal.TEN);
        dto.setProductType(ProductType.TRAGOS);
        dto.setIsAvailable(true);
        return dto;
    }

    private static Product product(UUID id) {
        Product product = new Product();
        product.setProductId(id);
        product.setProductName("Fernet");
        product.setDescription("Con coca");
        product.setBasePrice(BigDecimal.TEN);
        product.setProductType(ProductType.TRAGOS);
        product.setIsAvailable(true);
        product.setCreatedAt(OffsetDateTime.now());
        return product;
    }

    private static User user(UUID id) {
        User user = new User(
                id, "user-" + id, "Name", "Surname", id + "@example.com",
                "$2a$10$012345678901234567890uG0k34v3Ktb5sQ8kOLTNf5/4RswlD2oa",
                "1122334455", Role.USER, OffsetDateTime.now(), "es"
        );
        user.setVerified(true);
        return user;
    }
}
