package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class UserJDBCDao implements UserDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;

    private static final RowMapper<User> ROW_MAPPER = (rs, i) -> {
        User u = new User();
        u.setUserId(rs.getObject("user_id", java.util.UUID.class));
        u.setUserName(rs.getString("user_name"));
        u.setSurname(rs.getString("surname"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setCellphone(rs.getString("cellphone"));
        final String roleStr = rs.getString("role");
        u.setRole(roleStr == null ? null : Role.valueOf(roleStr.trim().toUpperCase(Locale.ROOT)));
        u.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return u;
    };

    @Autowired
    public UserJDBCDao(final DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("users")
                .usingColumns("user_id","user_name","surname","email","password_hash","cellphone","role","created_at");
    }

    @Override
    public User create(String userName, String passwordHash, String surname, String email, String cellphone, String role) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", id);
        params.put("user_name", userName);
        params.put("surname", surname);
        params.put("email", email);
        params.put("password_hash", passwordHash);
        params.put("cellphone", cellphone);
        params.put("role", role);
        params.put("created_at", now);
        insert.execute(params);
        return findById(id);
    }

    @Override
    public User update(User user) {
        final String sql = "UPDATE users SET user_name=?, surname=?, email=?, password_hash=?, cellphone=?, role=? WHERE user_id=?";
        int upd = jdbc.update(sql,
                user.getUsername(),
                user.getSurname(),
                user.getEmail(),
                user.getPassword(),
                user.getCellphone(),
                user.getRole(),
                user.getId());
        return upd > 0 ? findById(user.getId()) : null;
    }

    @Override
    public boolean delete(UUID id) {
        return jdbc.update("DELETE FROM users WHERE user_id = ?", id) > 0;
    }

    @Override
    public User findById(UUID id) {
        final String sql = "SELECT user_id,user_name,surname,email,password_hash,cellphone,role,created_at FROM users WHERE user_id = ?";
        return jdbc.query(sql, ROW_MAPPER, id).stream().findFirst().orElse(null);
    }

    @Override
    public User findByEmail(String email) {
        final String sql = "SELECT user_id,user_name,surname,email,password_hash,cellphone,role,created_at FROM users WHERE LOWER(email)=LOWER(?)";
        return jdbc.query(sql, ROW_MAPPER, email).stream().findFirst().orElse(null);
    }

    @Override
    public User findByUserName(String userName) {
        final String sql = "SELECT user_id,user_name,surname,email,password_hash,cellphone,role,created_at FROM users WHERE LOWER(user_name)=LOWER(?)";
        return jdbc.query(sql, ROW_MAPPER, userName).stream().findFirst().orElse(null);
    }

    @Override
    public List<User> findAll() {
        final String sql = "SELECT user_id,user_name,surname,email,password_hash,cellphone,role,created_at FROM users ORDER BY user_name ASC";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public List<User> findByRole(String role) {
        final String sql = "SELECT user_id,user_name,surname,email,password_hash,cellphone,role,created_at FROM users WHERE role = ? ORDER BY user_name ASC";
        return jdbc.query(sql, ROW_MAPPER, role);
    }
}
