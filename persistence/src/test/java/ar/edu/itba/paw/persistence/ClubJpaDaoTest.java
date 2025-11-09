/*package ar.edu.itba.paw.persistence;

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
import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ClubJpaDaoTest {

    private static final String NAME = "Club Test";
    private static final String DESCRIPTION = "Descripción";
    private static final String LOCATION = "Ubicación";
    private static final Price PRICE_RANGE = Price.MEDIO;
    private static final MusicalGenre MUSIC_GENRE = MusicalGenre.ROCK;

    @Autowired private ClubJpaDao clubDao;
    @Autowired private UserJpaDao userDao;

    @PersistenceContext private EntityManager em;

    @Before
    public void clean() {
        // Con @Transactional cada método hace rollback, pero por claridad:
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();
    }

    // Helpers JPQL para verificaciones sin JDBC
    private long countClubs() {
        return em.createQuery("select count(c) from Club c", Long.class).getSingleResult();
    }

    /*@Test
    public void testCreate() {
        User contact = userDao.create("user","name","pass", "last", "email", "cell", Role.USER, );
        // Si NO pusiste @PrePersist en Club para isBar, pasá false explícito en el último parámetro:
        Club club = clubDao.create(NAME, DESCRIPTION, LOCATION, PRICE_RANGE, contact, MUSIC_GENRE, Zone.ZONA_CABA, false, null);

        assertNotNull(club);
        assertEquals(NAME, club.getClubName());
        assertEquals(DESCRIPTION, club.getDescription());
        assertEquals(LOCATION, club.getLocation());
        assertEquals(PRICE_RANGE, club.getPriceRange());
        assertEquals(MUSIC_GENRE, club.getMusicGenre());
        assertEquals(contact.getId(), club.getContactInfo().getId());
        assertEquals(1L, countClubs());
    }

    @Test
    public void testUpdate() {
        User contact = userDao.create("user","name","pass", "last", "email", "cell", Role.USER);
        Club club = clubDao.create(NAME, DESCRIPTION, LOCATION, PRICE_RANGE, contact, MUSIC_GENRE, Zone.ZONA_CABA, false, null);

        Club updated = clubDao.update(
                club.getClubId(),
                "NuevoNombre",
                LOCATION,
                null,          // rating
                5,             // quantityRatings
                Price.ALTO,
                "NuevaDesc",
                contact.getId(),
                MusicalGenre.POP,
                Zone.ZONA_SUR,
                true,// isBar
                null
        );

        assertNotNull(updated);
        assertEquals("NuevoNombre", updated.getClubName());
        assertEquals(Price.ALTO, updated.getPriceRange());
        assertEquals(MusicalGenre.POP, updated.getMusicGenre());
        assertEquals("NuevaDesc", updated.getDescription());
        assertEquals(Zone.ZONA_SUR, updated.getZone());
        assertTrue(updated.getIsBar());
    }

    @Test
    public void testDelete() {
        User contact = userDao.create("user","name","pass", "last", "email", "cell", Role.USER);
        Club club = clubDao.create(NAME, DESCRIPTION, LOCATION, PRICE_RANGE, contact, MUSIC_GENRE, Zone.ZONA_CABA, false, null);

        boolean deleted = clubDao.delete(club.getClubId());

        assertTrue(deleted);
        assertEquals(0L, countClubs());
    }

    @Test
    public void testFindById() {
        User contact = userDao.create("user","name","pass", "last", "email", "cell", Role.USER);
        Club club = clubDao.create(NAME, DESCRIPTION, LOCATION, PRICE_RANGE, contact, MUSIC_GENRE, Zone.ZONA_CABA, false, null);

        Optional<Club> found = clubDao.findById(club.getClubId());

        assertTrue(found.isPresent());
        assertEquals(club.getClubId(), found.get().getClubId());
    }

    @Test
    public void testFindAdminsByClubId() {
        User contact = userDao.create("user","name","pass", "last", "email", "cell", Role.USER);
        Club club = clubDao.create(NAME, DESCRIPTION, LOCATION, PRICE_RANGE, contact, MUSIC_GENRE, Zone.ZONA_CABA, false, null);

        assertEquals(1, clubDao.findAdminsByClubId(club.getClubId()).size());
    }

}
*/