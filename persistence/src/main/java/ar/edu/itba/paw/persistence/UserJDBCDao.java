package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class UserJDBCDao implements UserDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;

    private static final RowMapper<User> ROW_MAPPER = (rs, i) -> {
        User u = new User();
        u.setUserId(UUID.fromString(rs.getString("user_id")));
        u.setUserName(rs.getString("user_name"));
        u.setSurname(rs.getString("surname"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setCellphone(rs.getString("cellphone"));
        final String roleStr = rs.getString("role");
        u.setRole(roleStr == null ? null : Role.valueOf(roleStr.trim().toUpperCase(Locale.ROOT)));
        u.setCreatedAt(rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toInstant().atOffset(java.time.ZoneOffset.UTC));
        return u;
    };

    @Autowired
    public UserJDBCDao(final DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("users")
                .usingColumns("user_id","user_name","surname","email","password_hash","cellphone","role");
    }

    @Override
    public User create(String username, String passwordHash, String surname, String email, String cellphone, Role role) {

        final String dbRole = role.name().toLowerCase(Locale.ROOT);
        final String dbEmail = email.trim().toLowerCase(Locale.ROOT);

        Map<String, Object> params = new HashMap<>();
        params.put("user_id", UUID.randomUUID());
        params.put("user_name", username);
        params.put("surname", surname);
        params.put("email", dbEmail);
        params.put("password_hash", passwordHash);
        params.put("cellphone", cellphone);
        params.put("role", dbRole);
        insert.execute(params);
        return findByEmail(email).orElseThrow();
    }

    @Override
    public User update(UUID userId, String userName, String surname, String email,
                       String passwordHash, String cellphone, Role role) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(userName, "userName is required");
        Objects.requireNonNull(surname, "surname is required");
        Objects.requireNonNull(email, "email is required");
        Objects.requireNonNull(passwordHash, "passwordHash is required");
        Objects.requireNonNull(cellphone, "cellphone is required");
        Objects.requireNonNull(role, "role is required");

        final String sql = "UPDATE users SET user_name=?, surname=?, email=?, password_hash=?, cellphone=?, role=? " +
                "WHERE user_id=?";

        int upd;
        try {
            upd = jdbc.update(sql,
                    userName,
                    surname,
                    email.trim().toLowerCase(Locale.ROOT),
                    passwordHash,
                    cellphone,
                    role.name().toLowerCase(Locale.ROOT),
                    userId
            );
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new IllegalStateException("El email ya está registrado: " + email, e);
        }

        return (upd > 0) ? findById(userId).orElse(null) : null;
    }


    @Override
    public boolean delete(UUID id) {
        return jdbc.update("DELETE FROM users WHERE user_id = ?", id) > 0;
    }

    @Override
    public Optional<User> findById(UUID id) {
        final String sql = "SELECT * FROM users WHERE user_id = ?";
        return jdbc.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        final String sql = "SELECT * FROM users WHERE LOWER(email)=LOWER(?)";
        return jdbc.query(sql, ROW_MAPPER, email).stream().findFirst();
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        final String sql = "SELECT * FROM users WHERE LOWER(user_name)=LOWER(?)";
        return jdbc.query(sql, ROW_MAPPER, userName).stream().findFirst();
    }

    @Override
    public List<User> findAll() {
        final String sql = "SELECT * FROM users ORDER BY user_name ASC";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public List<User> findByRole(String role) {
        final String sql = "SELECT * FROM users WHERE role = ? ORDER BY user_name ASC";
        return jdbc.query(sql, ROW_MAPPER, role);
    }

    @Override
    public RowMapper<User> getRowMapper() {
        return ROW_MAPPER;
    }
}
