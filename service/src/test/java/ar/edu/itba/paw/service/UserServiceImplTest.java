package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.security.crypto.password.PasswordEncoder;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String USERNAME = "Username";
    private static final String EMAIL = "email@test.com";
    private static final String PASSWORD = "Password";
    private static final String ENCODED_PASSWORD = "Encoded";
    private static final String TOKEN = "token";
    private static final String ROLE1 = "user";
    private static final String NAME = "name";
    private static final String LASTNAME = "lastname";
    private static final String CELLPHONE = "123456";
    private static final Role ROLE = Role.USER;
    private static final OffsetDateTime now = OffsetDateTime.now();

    @Mock
    private UserDao mockDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailServiceImpl mailService;

    @InjectMocks
    private UserServiceImpl userService;

    private final User user = new User(ID, USERNAME, "Name", "Surname", EMAIL, PASSWORD, "123", Role.USER, OffsetDateTime.now());

    @Test
    public void testCreate() {
        // 1. Setup!
        Mockito.when(mockDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, ROLE))
                .thenReturn(new User(UUID.randomUUID(), USERNAME, NAME, LASTNAME, EMAIL, PASSWORD, CELLPHONE, ROLE, now));
        Mockito.when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD);
        // 2. "ejercito" la class under test
        User maybeUser = userService.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, ROLE1);
        // 3. Asserts!
        Assert.assertNotNull(maybeUser);
        //Assert.assertTrue(maybeUser.isPresent());
        Assert.assertEquals(USERNAME, maybeUser.getUsername());
        Assert.assertEquals(PASSWORD, maybeUser.getPassword());
    }

    @Test
    public void testCreateEmptyPassword() {
        // 1. Setup!
        // 2. "ejercito" la class under test
        User maybeUser = userService.create(USERNAME, NAME, "", LASTNAME, EMAIL, CELLPHONE, ROLE1);
        // 3. Asserts!
        Assert.assertNull(maybeUser);
        //Assert.assertFalse(maybeUser.isPresent());
    }

    @Test
    public void testCreateAlreadyExists() {
        // 1. Setup!
        Mockito.lenient().when(mockDao.findByUserName(Mockito.eq(USERNAME)))
                .thenReturn(Optional.of(new User(UUID.randomUUID(), USERNAME, NAME, LASTNAME, EMAIL, PASSWORD, CELLPHONE, ROLE, now)));
        // 2. "ejercito" la class under test
        User maybeUser = userService.create(USERNAME, NAME, "", LASTNAME, EMAIL, CELLPHONE, ROLE1);
        // 3. Asserts!
        Assert.assertNull(maybeUser);
        //Assert.assertFalse(maybeUser.isPresent());
    }

    @Test
    public void testFindByIdFound() {
        Mockito.when(mockDao.findById(ID)).thenReturn(Optional.of(user));
        User found = userService.findById(ID);
        Assert.assertNotNull(found);
        Assert.assertEquals(USERNAME, found.getUsername());
    }

    @Test
    public void testFindByIdNotFound() {
        Mockito.when(mockDao.findById(ID)).thenReturn(Optional.empty());
        User found = userService.findById(ID);
        Assert.assertNull(found);
    }

    @Test
    public void testFindByEmailFound() {
        Mockito.when(mockDao.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        User found = userService.findByEmail(EMAIL);
        Assert.assertEquals(EMAIL, found.getEmail());
    }

    @Test
    public void testFindByEmailNotFound() {
        Mockito.when(mockDao.findByEmail(EMAIL)).thenReturn(Optional.empty());
        Assert.assertNull(userService.findByEmail(EMAIL));
    }

    @Test
    public void testFindByUserNameFound() {
        Mockito.when(mockDao.findByUserName(USERNAME)).thenReturn(Optional.of(user));
        Assert.assertEquals(USERNAME, userService.findByUserName(USERNAME).getUsername());
    }

    @Test
    public void testFindByUserNameNotFound() {
        Mockito.when(mockDao.findByUserName(USERNAME)).thenReturn(Optional.empty());
        Assert.assertNull(userService.findByUserName(USERNAME));
    }

    @Test
    public void testDeleteReturnsTrue() {
        Mockito.when(mockDao.delete(ID)).thenReturn(true);
        Assert.assertTrue(userService.delete(ID));
    }

    @Test
    public void testDeleteReturnsFalse() {
        Mockito.when(mockDao.delete(ID)).thenReturn(false);
        Assert.assertFalse(userService.delete(ID));
    }

    @Test
    public void testVerifyUserTokenExists() {
        Mockito.when(mockDao.findByVerificationToken(TOKEN)).thenReturn(Optional.of(user));
        Assert.assertTrue(userService.verifyUser(TOKEN));
    }

    @Test
    public void testVerifyUserTokenNotExists() {
        Mockito.when(mockDao.findByVerificationToken(TOKEN)).thenReturn(Optional.empty());
        Assert.assertFalse(userService.verifyUser(TOKEN));
    }

    @Test
    public void testAuthenticateByUsername() {
        user.setVerified(true);
        Mockito.when(mockDao.findByUserName(USERNAME)).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches(PASSWORD, PASSWORD)).thenReturn(true);
        Assert.assertTrue(userService.authenticateUsernameOrEmail(USERNAME, PASSWORD));
    }

    @Test(expected = IllegalStateException.class)
    public void testAuthenticateNotVerified() {
        user.setVerified(false);
        Mockito.when(mockDao.findByUserName(USERNAME)).thenReturn(Optional.of(user));
        userService.authenticateUsernameOrEmail(USERNAME, PASSWORD);
    }

    @Test
    public void testAuthenticateFallbackToEmail() {
        user.setVerified(true);
        Mockito.when(mockDao.findByUserName(USERNAME)).thenReturn(Optional.empty());
        Mockito.when(mockDao.findByEmail(USERNAME)).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches(PASSWORD, PASSWORD)).thenReturn(true);
        Assert.assertTrue(userService.authenticateUsernameOrEmail(USERNAME, PASSWORD));
    }

    @Test
    public void testAuthenticateNotFound() {
        Mockito.when(mockDao.findByUserName(USERNAME)).thenReturn(Optional.empty());
        Mockito.when(mockDao.findByEmail(USERNAME)).thenReturn(Optional.empty());
        Assert.assertFalse(userService.authenticateUsernameOrEmail(USERNAME, PASSWORD));
    }

    @Test
    public void testResendVerificationTokenUserFound() {
        user.setVerified(false);
        Mockito.when(mockDao.findByUserName(USERNAME)).thenReturn(Optional.of(user));
        Assert.assertTrue(userService.resendVerificationToken(USERNAME));
    }

    @Test
    public void testResendVerificationTokenVerified() {
        user.setVerified(true);
        Mockito.when(mockDao.findByUserName(USERNAME)).thenReturn(Optional.of(user));
        Assert.assertFalse(userService.resendVerificationToken(USERNAME));
    }

    @Test
    public void testResendVerificationTokenUserNotFound() {
        Mockito.when(mockDao.findByUserName(USERNAME)).thenReturn(Optional.empty());
        Mockito.when(mockDao.findByEmail(USERNAME)).thenReturn(Optional.empty());
        Assert.assertFalse(userService.resendVerificationToken(USERNAME));
    }

    @Test
    public void testRequestPasswordResetSuccess() {
        Mockito.when(mockDao.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Assert.assertTrue(userService.requestPasswordReset(EMAIL));
    }

    @Test
    public void testRequestPasswordResetUserNotFound() {
        Mockito.when(mockDao.findByEmail(EMAIL)).thenReturn(Optional.empty());
        Assert.assertFalse(userService.requestPasswordReset(EMAIL));
    }

    @Test
    public void testResetPasswordSuccess() {
        Mockito.when(mockDao.findByResetPasswordToken(TOKEN)).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn(ENCODED_PASSWORD);
        Assert.assertTrue(userService.resetPassword(TOKEN, PASSWORD));
    }

    @Test
    public void testResetPasswordTokenNotFound() {
        Mockito.when(mockDao.findByResetPasswordToken(TOKEN)).thenReturn(Optional.empty());
        Assert.assertFalse(userService.resetPassword(TOKEN, PASSWORD));
    }

    @Test
    public void testFindByVerificationTokenFound() {
        Mockito.when(mockDao.findByVerificationToken(TOKEN)).thenReturn(Optional.of(user));
        Assert.assertNotNull(userService.findByVerificationToken(TOKEN));
    }

    @Test
    public void testFindByVerificationTokenNotFound() {
        Mockito.when(mockDao.findByVerificationToken(TOKEN)).thenReturn(Optional.empty());
        Assert.assertNull(userService.findByVerificationToken(TOKEN));
    }

}