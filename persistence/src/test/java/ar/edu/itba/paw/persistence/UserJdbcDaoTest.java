package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import javax.sql.DataSource;
import ar.edu.itba.paw.model.User;
import org.junit.runner.RunWith;
import org.hsqldb.jdbc.JDBCDriver;

import java.util.Optional;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class UserJdbcDaoTest {
    private static final String PASSWORD = "Password";
    private static final String USERNAME = "Username";
    private static final String EMAIL = "Email";
    private static final String NAME = "Name";
    private static final String LASTNAME = "LastName";
    private static final String CELLPHONE = "cellphone";

    @Autowired
    private DataSource ds;
    @Autowired
    private UserJDBCDao userDao;
    private JdbcTemplate jdbcTemplate;
    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
    }
    @Test
    public void testCreate() {
        final User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        assertNotNull(user);
        assertEquals(USERNAME, user.getUsername());
        assertEquals(PASSWORD, user.getPassword());
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "users"));
    }
}