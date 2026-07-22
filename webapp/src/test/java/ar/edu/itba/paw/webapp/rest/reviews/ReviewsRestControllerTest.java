package ar.edu.itba.paw.webapp.rest.reviews;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.dto.ReviewDto;
import ar.edu.itba.paw.model.exceptions.ReviewNotFounException;
import ar.edu.itba.paw.service.ReviewService;
import ar.edu.itba.paw.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReviewsRestControllerTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final String LOGIN = "user@example.com";

    private ReviewService reviewService;
    private UserService userService;
    private ReviewsRestController controller;

    @Before
    public void setUp() {
        reviewService = mock(ReviewService.class);
        userService = mock(UserService.class);
        controller = new ReviewsRestController();

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));
        when(uriInfo.getRequestUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/reviews?clubId=" + CLUB_ID));
        when(uriInfo.getAbsolutePathBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/reviews"));

        ReflectionTestUtils.setField(controller, "reviewService", reviewService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);
    }

    @Test
    public void testListByClub_InvalidClubId_ReturnsBadRequest() {
        Response response = controller.list("not-a-uuid", null, 0, 20, mock(SecurityContext.class));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testList_MissingFilters_ReturnsBadRequest() {
        Response response = controller.list(null, null, 0, 20, mock(SecurityContext.class));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testListByClub_InvalidPagination_ReturnsBadRequest() {
        Response response = controller.list(CLUB_ID.toString(), null, -1, 20, mock(SecurityContext.class));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testListByClub_ReturnsItemLinksWithTotalCount() {
        when(reviewService.listByClub(CLUB_ID, null, 20, 0)).thenReturn(List.of(review(REVIEW_ID)));
        when(reviewService.countByClub(CLUB_ID)).thenReturn(1);

        Response response = controller.list(CLUB_ID.toString(), null, 0, 20, mock(SecurityContext.class));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("1", response.getHeaderString("X-Total-Count"));
        @SuppressWarnings("unchecked")
        List<LinkDto> reviews = (List<LinkDto>) response.getEntity();
        assertEquals(1, reviews.size());
        assertEquals("item", reviews.get(0).getRel());
        assertEquals("GET", reviews.get(0).getMethod());
        assertEquals("http://localhost/api/reviews/" + REVIEW_ID,
                reviews.get(0).getHref());
    }

    @Test
    public void testCreate_MissingRating_ReturnsBadRequest() {
        Response response = controller.create(CLUB_ID.toString(), new ReviewDto(), mock(SecurityContext.class));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreate_MissingClubId_ReturnsBadRequest() {
        ReviewDto body = new ReviewDto();
        body.setRating(5);

        Response response = controller.create(null, body, mock(SecurityContext.class));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreate_Unauthenticated_ReturnsUnauthorized() {
        ReviewDto body = new ReviewDto();
        body.setRating(5);

        Response response = controller.create(CLUB_ID.toString(), body, mock(SecurityContext.class));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreate_Success_ReturnsCreated() {
        User user = user(UUID.randomUUID());
        when(userService.findByEmail(LOGIN)).thenReturn(user);
        Review created = review(REVIEW_ID);
        created.setUser(user);
        when(reviewService.getUserReviewForClub(user.getId(), CLUB_ID)).thenReturn(Optional.of(created));

        ReviewDto body = new ReviewDto();
        body.setRating(5);
        body.setDescription("Buenísimo");

        Response response = controller.create(CLUB_ID.toString(), body, principalFor(LOGIN));

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ReviewDto);
        assertEquals(REVIEW_ID, ((ReviewDto) response.getEntity()).getReviewId());
    }

    @Test
    public void testCreate_ServiceValidationError_ReturnsBadRequest() throws Exception {
        User user = user(UUID.randomUUID());
        when(userService.findByEmail(LOGIN)).thenReturn(user);
        doThrow(new IllegalArgumentException("Rating must be between 1 and 5"))
                .when(reviewService)
                .createReview(user.getId(), CLUB_ID, 6, "rating invalido", null);

        ReviewDto body = new ReviewDto();
        body.setRating(6);
        body.setDescription("rating invalido");

        Response response = controller.create(CLUB_ID.toString(), body, principalFor(LOGIN));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetById_InvalidReviewId_ReturnsBadRequest() {
        Response response = controller.getById("not-a-uuid");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetById_NotFound_ReturnsNotFound() {
        when(reviewService.findById(REVIEW_ID)).thenReturn(Optional.empty());
        Response response = controller.getById(REVIEW_ID.toString());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetById_Found_ReturnsReviewDto() {
        when(reviewService.findById(REVIEW_ID)).thenReturn(Optional.of(review(REVIEW_ID)));
        Response response = controller.getById(REVIEW_ID.toString());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(REVIEW_ID, ((ReviewDto) response.getEntity()).getReviewId());
    }

    @Test
    public void testDelete_NotOwnedByCaller_ReturnsNotFound() {
        User caller = user(UUID.randomUUID());
        when(userService.findByEmail(LOGIN)).thenReturn(caller);
        Review someoneElses = review(REVIEW_ID);
        someoneElses.setUser(user(UUID.randomUUID()));
        when(reviewService.findById(REVIEW_ID)).thenReturn(Optional.of(someoneElses));

        Response response = controller.delete(REVIEW_ID.toString(), principalFor(LOGIN));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDelete_Owned_ReturnsNoContent() {
        User caller = user(UUID.randomUUID());
        when(userService.findByEmail(LOGIN)).thenReturn(caller);
        Review owned = review(REVIEW_ID);
        owned.setUser(caller);
        when(reviewService.findById(REVIEW_ID)).thenReturn(Optional.of(owned));

        Response response = controller.delete(REVIEW_ID.toString(), principalFor(LOGIN));

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdate_ServiceNotFound_ReturnsNotFound() throws Exception {
        User caller = user(UUID.randomUUID());
        when(userService.findByEmail(LOGIN)).thenReturn(caller);
        Review owned = review(REVIEW_ID);
        owned.setUser(caller);
        when(reviewService.findById(REVIEW_ID)).thenReturn(Optional.of(owned));
        doThrow(new ReviewNotFounException("No se encontro la review"))
                .when(reviewService)
                .updateReview(REVIEW_ID, 4, "Actualizada", null);

        ReviewDto body = new ReviewDto();
        body.setRating(4);
        body.setDescription("Actualizada");

        Response response = controller.update(REVIEW_ID.toString(), body, principalFor(LOGIN));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    private static Review review(UUID id) {
        Club club = new Club();
        club.setClubId(CLUB_ID);
        Review review = new Review();
        review.setReviewId(id);
        review.setClub(club);
        review.setRating(5);
        review.setDescription("Buenísimo");
        review.setCreatedAt(OffsetDateTime.now());
        return review;
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

    private SecurityContext principalFor(String login) {
        SecurityContext context = mock(SecurityContext.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(login);
        when(context.getUserPrincipal()).thenReturn(principal);
        return context;
    }
}
