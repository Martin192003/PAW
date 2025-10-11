package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ClubAdminsJpaDaoTest {

    @Autowired private ClubAdminsJpaDao clubAdminsDao;
    @Autowired private ClubJpaDao clubDao;
    @Autowired private UserJpaDao userDao;

    @PersistenceContext private EntityManager em;

    @Before
    public void setUp() {
        // Con @Transactional, cada método hace rollback. No necesitamos limpiar a mano.
        em.flush();
    }

    // -------- Helpers de verificación (JPQL) --------
    private long countAdminsForClub(UUID clubId) {
        return em.createQuery(
                        "select count(a) from Club c join c.admins a where c.clubId = :cid", Long.class)
                .setParameter("cid", clubId)
                .getSingleResult();
    }

    private long countClubsForAdmin(UUID adminId) {
        return em.createQuery(
                        "select count(c) from User u join u.adminOfClubs c where u.id = :uid", Long.class)
                .setParameter("uid", adminId)
                .getSingleResult();
    }

    @Test
    public void testAddAdmin() {
        User admin = userDao.create("admin", "name","pass", "last", "email", "cell", Role.USER);
        Club club = clubDao.create("Club", "Desc", "Loc", Price.MEDIO, admin, MusicalGenre.ROCK, Zone.ZONA_CABA, null);

        boolean added = clubAdminsDao.addAdmin(club, admin);

        assertTrue(added);
        assertEquals(1L, countAdminsForClub(club.getClubId()));
    }

    @Test
    public void testExistsReturnsFalse() {
        Club club = new Club();
        club.setClubId(UUID.randomUUID());
        User admin = new User();
        admin.setUserId(UUID.randomUUID());

        assertFalse(clubAdminsDao.exists(club, admin));
    }

    @Test
    public void testFindAdminsReturnsEmptyList() {
        Club club = new Club();
        club.setClubId(UUID.randomUUID());

        List<User> admins = clubAdminsDao.findAdmins(club);

        assertNotNull(admins);
        assertTrue(admins.isEmpty());
    }

    @Test
    public void testFindClubsByAdminReturnsEmptyList() {
        User admin = new User();
        admin.setUserId(UUID.randomUUID());

        List<Club> clubs = clubAdminsDao.findClubsByAdmin(admin);

        assertNotNull(clubs);
        assertTrue(clubs.isEmpty());
    }

    @Test
    public void testRemoveAdminReturnsFalseIfNotFound() {
        Club club = new Club();
        club.setClubId(UUID.randomUUID());
        User admin = new User();
        admin.setUserId(UUID.randomUUID());

        assertFalse(clubAdminsDao.removeAdmin(club, admin));
    }

    @Test
    public void testRemoveAdminRemovesExisting() {
        User admin = userDao.create("admin2", "name2","pass2", "last2", "email2", "cell2", Role.USER);
        Club club = clubDao.create("Club2", "Desc2", "Loc2", Price.MEDIO, admin, MusicalGenre.ROCK, Zone.ZONA_CABA, null);
        clubAdminsDao.addAdmin(club, admin);

        boolean removed = clubAdminsDao.removeAdmin(club, admin);

        assertTrue(removed);
        assertEquals(0L, countAdminsForClub(club.getClubId()));
    }

    @Test
    public void testFindAdminsReturnsAdminAfterAdd() {
        User admin = userDao.create("admin3", "name3","pass3", "last3", "email3", "cell3", Role.USER);
        Club club = clubDao.create("Club3", "Desc3", "Loc3", Price.MEDIO, admin, MusicalGenre.ROCK, Zone.ZONA_CABA, null);
        clubAdminsDao.addAdmin(club, admin);

        List<User> admins = clubAdminsDao.findAdmins(club);

        assertEquals(1, admins.size());
        assertEquals(admin.getId(), admins.get(0).getId());
    }

    @Test
    public void testFindClubsByAdminReturnsClubAfterAdd() {
        User admin = userDao.create("admin4", "name4","pass4", "last4", "email4", "cell4", Role.USER);
        Club club = clubDao.create("Club4", "Desc4", "Loc4", Price.MEDIO, admin, MusicalGenre.ROCK, Zone.ZONA_CABA, null);
        clubAdminsDao.addAdmin(club, admin);

        List<Club> clubs = clubAdminsDao.findClubsByAdmin(admin);

        assertEquals(1, clubs.size());
        assertEquals(club.getClubId(), clubs.get(0).getClubId());
    }

    @Test
    public void testAddAdminDoesNotDuplicate() {
        User admin = userDao.create("admin5", "name5","pass5", "last5", "email5", "cell5", Role.USER);
        Club club = clubDao.create("Club5", "Desc5", "Loc5", Price.MEDIO, admin, MusicalGenre.ROCK, Zone.ZONA_CABA, null);

        boolean firstAdd = clubAdminsDao.addAdmin(club, admin);
        boolean secondAdd = clubAdminsDao.addAdmin(club, admin);

        assertTrue(firstAdd);
        assertTrue(secondAdd);
        assertEquals(1L, countAdminsForClub(club.getClubId()));
        assertEquals(1L, countClubsForAdmin(admin.getId()));
    }
}
