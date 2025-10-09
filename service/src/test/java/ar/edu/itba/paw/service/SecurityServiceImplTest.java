package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.UserDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class SecurityServiceImplTest {
    @Mock
    private UserDao userDao;

    @InjectMocks
    private SecurityServiceImpl securityService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @Test
    public void testGetLoggedUser_UserPrincipal() {
        User user = Mockito.mock(User.class);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getPrincipal()).thenReturn(user);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        Optional<User> result = securityService.getLoggedUser();
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(user, result.get());
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetLoggedUser_DefaultPrincipal() {
        String username = "defaultuser";
        User user = Mockito.mock(User.class);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getPrincipal()).thenReturn(username);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.when(userDao.findByUserName(username)).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        Optional<User> result = securityService.getLoggedUser();
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(user, result.get());
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetLoggedUser_DefaultPrincipalFallbackToEmail() {
        String username = "defaultuser";
        User user = Mockito.mock(User.class);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getPrincipal()).thenReturn(username);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.when(userDao.findByUserName(username)).thenReturn(Optional.empty());
        Mockito.when(userDao.findByEmail(username)).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
        Optional<User> result = securityService.getLoggedUser();
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(user, result.get());
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetLoggedUser_NotAuthenticated() {
        Mockito.when(authentication.isAuthenticated()).thenReturn(false);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        Optional<User> result = securityService.getLoggedUser();
        Assert.assertFalse(result.isPresent());
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetVerifiedLoggedUser() {
        User user = Mockito.mock(User.class);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getPrincipal()).thenReturn(user);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        User result = securityService.getVerifiedLoggedUser();
        Assert.assertEquals(user, result);
        SecurityContextHolder.clearContext();
    }
}
