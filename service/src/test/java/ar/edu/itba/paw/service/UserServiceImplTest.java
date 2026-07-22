package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.persistence.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService mailService;

    @Mock
    private BookingDao bookingDao;

    @Mock
    private ReviewsDao reviewDao;

    @Mock
    private ClubDao clubDao;

    @InjectMocks
    private UserServiceImpl userService;

    @Before
    public void setup() {
        userDao = mock(UserDao.class);
        passwordEncoder = mock(PasswordEncoder.class);
        mailService = mock(EmailService.class);
        bookingDao = mock(BookingDao.class);
        reviewDao = mock(ReviewsDao.class);
        clubDao = mock(ClubDao.class);

        userService = new UserServiceImpl(userDao, passwordEncoder, mailService, bookingDao, reviewDao, clubDao);

        when(userDao.findByUserName(anyString())).thenReturn(Optional.empty());
        when(userDao.findByEmail(anyString())).thenReturn(Optional.empty());
    }

    private User makeUser(UUID id) {
        User u = new User();
        u.setUserId(id);
        u.setUserName("test");
        u.setName("Test");
        u.setSurname("Test");
        u.setEmail("test@mail.com");
        u.setPasswordHash("pw");
        u.setCellphone("111");
        u.setRole(Role.USER);
        u.setLocale("es");
        return u;
    }


    @Test
    public void testCreateSuccess() {
        UUID id = UUID.randomUUID();
        User created = makeUser(id);

        when(passwordEncoder.encode("pw")).thenReturn("ENC");
        when(userDao.create(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(Role.class), anyString()))
                .thenReturn(created);

        User result = userService.create("test","Test","pw","Test","mail@mail.com","123","USER","es");

        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test(expected = DuplicateEntityException.class)
    public void testCreateDuplicateEmailThrows() {
        when(passwordEncoder.encode("pw")).thenReturn("ENC");
        when(userDao.create(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(Role.class), anyString()))
                .thenThrow(new DataIntegrityViolationException(""));

        userService.create("test","Test","pw","Test","mail@mail.com","123","USER","es");
    }

    @Test(expected = DuplicateEntityException.class)
    public void testCreateExistingEmailThrowsBeforePersisting() {
        when(userDao.findByEmail("mail@mail.com")).thenReturn(Optional.of(makeUser(UUID.randomUUID())));

        userService.create("test","Test","pw","Test","mail@mail.com","123","USER","es");
    }

    @Test(expected = DuplicateEntityException.class)
    public void testCreateExistingUsernameThrowsBeforePersisting() {
        when(userDao.findByUserName("test")).thenReturn(Optional.of(makeUser(UUID.randomUUID())));

        userService.create("test","Test","pw","Test","mail@mail.com","123","USER","es");
    }

    @Test
    public void testUpdateSuccess() {
        UUID id = UUID.randomUUID();
        User dbUser = makeUser(id);
        User newUser = makeUser(id);
        newUser.setUserName("nuevo");

        when(userDao.findById(id)).thenReturn(Optional.of(dbUser));
        when(userDao.update(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(Role.class), anyString()))
                .thenReturn(newUser);

        User updated = userService.update(newUser);

        assertEquals("nuevo", updated.getUsername());
    }

    @Test(expected = DuplicateEntityException.class)
    public void testUpdateDuplicateUsernameThrows() {
        UUID id = UUID.randomUUID();
        User dbUser = makeUser(id);
        User newUser = makeUser(id);
        newUser.setUserName("newName");

        when(userDao.findById(id)).thenReturn(Optional.of(dbUser));
        when(userDao.findByUserName("newName")).thenReturn(Optional.of(new User()));

        userService.update(newUser);
    }

    @Test(expected = DuplicateEntityException.class)
    public void testUpdateDuplicateUsernameThrowsWhenLoadedUserAlreadyHasNewValue() {
        UUID id = UUID.randomUUID();
        User mutatedLoadedUser = makeUser(id);
        mutatedLoadedUser.setUserName("newName");
        User otherUser = makeUser(UUID.randomUUID());
        otherUser.setUserName("newName");

        when(userDao.findById(id)).thenReturn(Optional.of(mutatedLoadedUser));
        when(userDao.findByUserName("newName")).thenReturn(Optional.of(otherUser));

        userService.update(mutatedLoadedUser);
    }

    @Test(expected = DuplicateEntityException.class)
    public void testUpdateDuplicateEmailThrows() {
        UUID id = UUID.randomUUID();
        User dbUser = makeUser(id);
        User newUser = makeUser(id);
        newUser.setEmail("dup@mail.com");
        User otherUser = makeUser(UUID.randomUUID());
        otherUser.setEmail("dup@mail.com");

        when(userDao.findById(id)).thenReturn(Optional.of(dbUser));
        when(userDao.findByEmail("dup@mail.com")).thenReturn(Optional.of(otherUser));

        userService.update(newUser);
    }

    @Test
    public void testUpdateKeepsSameUsernameWhenLookupFindsSelf() {
        UUID id = UUID.randomUUID();
        User dbUser = makeUser(id);
        User newUser = makeUser(id);
        newUser.setName("Nuevo");

        when(userDao.findById(id)).thenReturn(Optional.of(dbUser));
        when(userDao.findByUserName("test")).thenReturn(Optional.of(dbUser));
        when(userDao.findByEmail("test@mail.com")).thenReturn(Optional.of(dbUser));
        when(userDao.update(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(Role.class), anyString()))
                .thenReturn(newUser);

        User updated = userService.update(newUser);

        assertEquals("Nuevo", updated.getName());
    }

    @Test
    public void testDelete() {
        when(userDao.delete(any())).thenReturn(true);
        assertTrue(userService.delete(UUID.randomUUID()));
    }

    @Test
    public void testFindByEmail() {
        User user = makeUser(UUID.randomUUID());
        when(userDao.findByEmail("mail@mail.com")).thenReturn(Optional.of(user));

        assertEquals(user, userService.findByEmail("mail@mail.com"));
    }

    @Test
    public void testFindByUsername() {
        User user = makeUser(UUID.randomUUID());
        when(userDao.findByUserName("test")).thenReturn(Optional.of(user));

        assertEquals(user, userService.findByUserName("test"));
    }

    @Test
    public void testVerifyUserOk() {
        User user = makeUser(UUID.randomUUID());
        when(userDao.findByVerificationToken("abc")).thenReturn(Optional.of(user));

        boolean result = userService.verifyUser("abc");
        assertTrue(result);
    }

    @Test
    public void testVerifyUserFail() {
        when(userDao.findByVerificationToken("abc")).thenReturn(Optional.empty());
        assertFalse(userService.verifyUser("abc"));
    }

    @Test
    public void testResendVerificationSuccess() {
        User user = makeUser(UUID.randomUUID());
        when(userDao.findByUserName("test")).thenReturn(Optional.of(user));
        assertTrue(userService.resendVerificationToken("test"));
    }

    @Test
    public void testResendVerificationUserNotFound() {
        when(userDao.findByUserName("test")).thenReturn(Optional.empty());
        when(userDao.findByEmail("test")).thenReturn(Optional.empty());
        assertFalse(userService.resendVerificationToken("test"));
    }

    @Test
    public void testResetPasswordSuccess() {
        User user = makeUser(UUID.randomUUID());
        when(userDao.findByResetPasswordToken("tok")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpw")).thenReturn("ENC");

        Optional<User> result = userService.resetPassword("tok", "newpw");

        assertTrue(result.isPresent());
        assertEquals("ENC", result.get().getPassword());
        assertNull(result.get().getResetPasswordToken());
    }


    @Test
    public void testChangePasswordForAuthenticatedUser() {
        User user = makeUser(UUID.randomUUID());
        when(userDao.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("123")).thenReturn("ENC");

        assertTrue(userService.changePasswordForAuthenticatedUser(user.getId(),"123"));
    }

    @Test
    public void testAuthenticateLegacyPasswordUpgradesItImmediately() {
        User user = makeUser(UUID.randomUUID());
        user.setVerified(true);
        user.setPasswordHash("legacy-password");
        when(userDao.findByUserName("test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("legacy-password", "legacy-password")).thenReturn(true);
        when(passwordEncoder.upgradeEncoding("legacy-password")).thenReturn(true);
        when(passwordEncoder.encode("legacy-password")).thenReturn("bcrypt-password");

        User authenticated = userService.authenticateAndGetUser("test", "legacy-password");

        assertSame(user, authenticated);
        assertEquals("bcrypt-password", authenticated.getPassword());
        verify(userDao).updatePasswordHash(user.getId(), "bcrypt-password");
    }

    @Test
    public void testAuthenticateWrongLegacyPasswordDoesNotUpgradeIt() {
        User user = makeUser(UUID.randomUUID());
        user.setVerified(true);
        user.setPasswordHash("legacy-password");
        when(userDao.findByUserName("test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "legacy-password")).thenReturn(false);

        assertNull(userService.authenticateAndGetUser("test", "wrong"));

        verify(passwordEncoder, never()).encode(any());
        verify(userDao, never()).updatePasswordHash(any(), anyString());
    }

    @Test
    public void testUpdateFieldSuccess() {
        UUID id = UUID.randomUUID();
        User u = makeUser(id);
        when(userDao.findById(id)).thenReturn(Optional.of(u));
        when(userDao.update(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(u);

        User result = userService.updateUserField(id,"name","Nuevo");
        assertEquals("Nuevo", result.getName());
    }

    @Test(expected = DuplicateEntityException.class)
    public void testUpdateFieldDuplicateEmail() {
        UUID id = UUID.randomUUID();
        User u = makeUser(id);

        when(userDao.findById(id)).thenReturn(Optional.of(u));
        when(userDao.findByEmail("dup@mail.com")).thenReturn(Optional.of(new User()));

        userService.updateUserField(id,"email","dup@mail.com");
    }

    @Test
    public void testSendReviewRequests() {
        User user = makeUser(UUID.randomUUID());
        Booking past = mock(Booking.class);
        when(bookingDao.findByUser(user.getId())).thenReturn(Collections.singletonList(past));

        when(past.getBookingEnd()).thenReturn(OffsetDateTime.now().minusDays(1));
        when(past.getTable()).thenReturn(mock(ClubTable.class));
        when(past.getTable().getClubId()).thenReturn(UUID.randomUUID());
        when(reviewDao.existsByUserAndClub(any(), any())).thenReturn(false);
        when(clubDao.findById(any())).thenReturn(Optional.of(mock(Club.class)));

        userService.sendReviewRequestsForPastBookings(user);
    }
}
