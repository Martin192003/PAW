package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class ClubAdminsJDBCDao implements ClubAdminDao {

    private final JdbcTemplate jdbc;
    private final UserDao userDao;
    private final ClubDao clubDao;

    @Autowired
    public ClubAdminsJDBCDao(final DataSource ds, final UserDao userDao, final ClubDao clubDao) {
        this.jdbc = new JdbcTemplate(ds);
        this.userDao = userDao;
        this.clubDao = clubDao;
    }

    @Override
    public boolean exists(Club club, User admin) {
        final String sql = "SELECT 1 FROM club_admins WHERE club_id = ? AND admin_id = ? LIMIT 1";
        return jdbc.query(sql, (rs, i) -> 1, club.getClubId(), admin.getId()).stream().findFirst().isPresent();
    }

    @Override
    public List<User> findAdmins(Club club) {
        final String sql = "SELECT admin_id FROM club_admins WHERE club_id = ?";
        return jdbc.query(sql, (rs, i) -> userDao.findById(rs.getObject("admin_id", java.util.UUID.class)), club.getClubId());
    }

    @Override
    public List<Club> findClubsByAdmin(User admin) {
        final String sql = "SELECT club_id FROM club_admins WHERE admin_id = ?";
        return jdbc.query(sql, (rs, i) -> clubDao.findById(rs.getObject("club_id", java.util.UUID.class)), admin.getId());
    }

    @Override
    public boolean addAdmin(Club club, User admin) {
        if (exists(club, admin)) return true;
        final String sql = "INSERT INTO club_admins (club_id, admin_id) VALUES (?, ?)";
        return jdbc.update(sql, club.getClubId(), admin.getId()) > 0;
    }

    @Override
    public boolean removeAdmin(Club club, User admin) {
        final String sql = "DELETE FROM club_admins WHERE club_id = ? AND admin_id = ?";
        return jdbc.update(sql, club.getClubId(), admin.getId()) > 0;
    }
}
