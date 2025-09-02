package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import ar.edu.itba.paw.persistence.UserDao;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {
    private static final String PASSWORD = "Password";
    private static final String USERNAME = "Username";
    private static final String EMAIL = "Email";
    private static final String LASTNAME = "LastName";
    private static final String CELLPHONE = "cellphone";
    private static final String ROLE1 = "user";
    private static final Role ROLE = Role.USER;
    private static final OffsetDateTime now =  OffsetDateTime.now();

    @Mock
    private UserDao mockDao;
    
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void testCreate() {
        // 1. Setup!
        Mockito.when(mockDao.create(USERNAME, PASSWORD,LASTNAME, EMAIL, CELLPHONE, ROLE))
                .thenReturn(new User(UUID.randomUUID(), USERNAME, LASTNAME, EMAIL, PASSWORD, CELLPHONE, ROLE, now));
        // 2. "ejercito" la class under test
        User maybeUser = userService.create(USERNAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, ROLE1);
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
        User maybeUser = userService.create(USERNAME, "", LASTNAME, EMAIL, CELLPHONE, ROLE1);
        // 3. Asserts!
        Assert.assertNull(maybeUser);
        //Assert.assertFalse(maybeUser.isPresent());
    }
    @Test
    public void testCreateAlreadyExists() {
        // 1. Setup!
        Mockito.lenient().when(mockDao.findByUserName(Mockito.eq(USERNAME)))
                .thenReturn(Optional.of(new User(UUID.randomUUID(), USERNAME, LASTNAME, EMAIL, PASSWORD, CELLPHONE, ROLE, now)));
        // 2. "ejercito" la class under test
        User maybeUser = userService.create(USERNAME, "", LASTNAME, EMAIL, CELLPHONE, ROLE1);
        // 3. Asserts!
        Assert.assertNull(maybeUser);
        //Assert.assertFalse(maybeUser.isPresent());
    }
}