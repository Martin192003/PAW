package ar.edu.itba.paw.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ar.edu.itba.paw.interfaces.UserDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import ar.edu.itba.paw.model.User.User;
import ar.edu.itba.paw.model.User.Role;
import org.springframework.stereotype.Repository;

@Repository
public class UserJdbcDao implements UserDao {
    private JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;
    private final static RowMapper<User> ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();
        user.setId(UUID.fromString(rs.getString("userId")));
        user.setUsername(rs.getString("userName"));
        user.setSurname(rs.getString("surname"));
        user.setPassword(rs.getString("password"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setEmail(rs.getString("email"));
        user.setCellphone(rs.getString("cellphone"));
        user.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
        return user;
    };

    @Autowired
    public UserJdbcDao(final DataSource ds) {
        this.jdbcTemplate = new JdbcTemplate(ds);

        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingGeneratedKeyColumns("userId")
                .usingColumns("userName", "surname", "email", "cellphone", "password", "role");
    }

    @Override
    public User findById(final long id) {
        final List<User> list = jdbcTemplate.query("SELECT * FROM users WHERE userid = ?", ROW_MAPPER, id);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public User create(final String username, final String password,
                       final String surname, final String email,
                       final String cellphone, final Role role) {

        final Map<String, Object> args = new HashMap<>();
        args.put("username", username);
        args.put("password", password);
        args.put("surname", surname);
        args.put("email", email);
        args.put("cellphone", cellphone);
        args.put("role", role.name());

        final UUID userId = UUID.randomUUID();
        args.put("userId",userId);

        jdbcInsert.execute(args);

        User user = new User(userId, username, password, surname,
                email, cellphone, role);
        user.setId(userId);

        return user;
    }

}