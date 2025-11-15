package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.UserDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class SecurityServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private ClubService clubService;

    @InjectMocks
    private SecurityServiceImpl securityService;

    private User user;

    @Before
    public void setUp() {
        user = Mockito.mock(User.class);
        UUID fixedId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Mockito.when(user.getId()).thenReturn(fixedId);
    }

    @Test
    public void testGetLoggedUserPrincipalIsUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user, null, "ROLE_USER")
        );

        Optional<User> result = securityService.getLoggedUser();

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(user, result.get());
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetLoggedUserStringPrincipalFoundByUsername() {
        Mockito.when(userDao.findByUserName("test")).thenReturn(Optional.of(user));

        TestingAuthenticationToken token = new TestingAuthenticationToken("test", null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);

        Optional<User> result = securityService.getLoggedUser();

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(user, result.get());
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetLoggedUserStringPrincipalFallbackToEmail() {
        Mockito.when(userDao.findByUserName("test")).thenReturn(Optional.empty());
        Mockito.when(userDao.findByEmail("test")).thenReturn(Optional.of(user));

        TestingAuthenticationToken token = new TestingAuthenticationToken("test", null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);

        Optional<User> result = securityService.getLoggedUser();

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(user, result.get());
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetLoggedUserNotAuthenticated() {
        SecurityContextHolder.clearContext();
        Optional<User> result = securityService.getLoggedUser();
        Assert.assertFalse(result.isPresent());
    }


    @Test
    public void testGetVerifiedLoggedUserReturnsUser() {
        TestingAuthenticationToken token = new TestingAuthenticationToken(user, null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);

        User result = securityService.getVerifiedLoggedUser();

        Assert.assertEquals(user, result);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetVerifiedLoggedUserReturnsNullWhenNoAuth() {
        SecurityContextHolder.clearContext();
        User result = securityService.getVerifiedLoggedUser();
        Assert.assertNull(result);
    }


    @Test
    public void testIsClubAdminWhenUserHasAdminRole() {
        UUID club = UUID.randomUUID();
        Mockito.when(user.getRole()).thenReturn(Role.ADMIN);
        Mockito.when(clubService.getAdminsByClub(club)).thenReturn(List.of(user));

        TestingAuthenticationToken token = new TestingAuthenticationToken(user, null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);

        boolean result = securityService.isClubAdmin(club);

        Assert.assertTrue(result);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testIsClubAdminWhenUserIsClubAdmin() {
        UUID club = UUID.randomUUID();
        Mockito.when(user.getRole()).thenReturn(Role.ADMIN);
        Mockito.when(clubService.getAdminsByClub(club)).thenReturn(List.of(user));

        TestingAuthenticationToken token = new TestingAuthenticationToken(user, null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);

        boolean result = securityService.isClubAdmin(club);

        Assert.assertTrue(result);
        SecurityContextHolder.clearContext();
    }


    @Test
    public void testIsClubAdminWhenUserIsNotAdmin() {
        UUID club = UUID.randomUUID();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user, null)
        );

        boolean result = securityService.isClubAdmin(club);

        Assert.assertFalse(result);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testIsClubAdminWhenNoLoggedUser() {
        SecurityContextHolder.clearContext();
        boolean result = securityService.isClubAdmin(UUID.randomUUID());
        Assert.assertFalse(result);
    }

    @Test
    public void testCanAccessUserWhenSameUser() {
        TestingAuthenticationToken token = new TestingAuthenticationToken(user, null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);

        boolean result = securityService.canAccessUser(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        Assert.assertTrue(result);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testCanAccessUserWhenUserIsAdmin() {
        TestingAuthenticationToken token = new TestingAuthenticationToken(user, null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);

        boolean result = securityService.canAccessUser(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        Assert.assertTrue(result);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testCanAccessUserNotAllowed() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user, null)
        );

        boolean result = securityService.canAccessUser(UUID.randomUUID());

        Assert.assertFalse(result);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testCanAccessUserNoLoggedUser() {
        SecurityContextHolder.clearContext();
        boolean result = securityService.canAccessUser(UUID.randomUUID());
        Assert.assertFalse(result);
    }
}
