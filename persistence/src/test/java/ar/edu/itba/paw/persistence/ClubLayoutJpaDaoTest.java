package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.ClubLayout;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ClubLayoutJpaDaoTest {

    @Autowired
    private ClubLayoutJpaDao clubLayoutDao;

    @PersistenceContext
    private EntityManager em;

    private User uploader;
    private UUID clubId;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM ClubLayout l").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();

        uploader = persistUser();
        Club club = persistClub(uploader);
        clubId = club.getClubId();
    }

    // ----------------- Tests save -----------------

    @Test
    public void testSavePersistsNewLayout() {
        ClubLayout layout = buildNewLayoutForClub(clubId);

        ClubLayout saved = clubLayoutDao.save(layout);
        em.flush();
        em.clear();

        assertNotNull(saved);
        assertNotNull(saved.getLayoutId());
        assertEquals(1L, countLayouts());
        assertEquals(clubId, saved.getClubId());
        assertEquals(uploader.getId(), saved.getUploadedBy());
        assertEquals("/clubs/club/" + clubId + "/layout/fake-id/image", saved.getUrl());
        assertEquals("layout.png", saved.getFilename());
        assertEquals("image/png", saved.getContentType());
        assertEquals(Long.valueOf(3L), saved.getSizeBytes());
    }

    @Test
    public void testSaveMergesExistingLayoutWhenIdNotNull() {
        ClubLayout original = buildNewLayoutForClub(clubId);
        em.persist(original);
        em.flush();

        assertEquals(1L, countLayouts());
        UUID existingId = original.getLayoutId();
        assertNotNull(existingId);

        original.setUrl("/custom/url/" + existingId);
        original.setFilename("layout2.png");

        ClubLayout merged = clubLayoutDao.save(original);
        em.flush();
        em.clear();

        assertEquals(existingId, merged.getLayoutId());
        assertEquals("/custom/url/" + existingId, merged.getUrl());
        assertEquals("layout2.png", merged.getFilename());
        assertEquals(1L, countLayouts());
    }

    @Test
    public void testSaveWithoutClubIdFails() {
        ClubLayout invalid = new ClubLayout(
                null,
                uploader.getId(),
                "/some/url",
                new byte[]{1, 2, 3},
                "layout.png",
                "image/png",
                3L
        );

        assertThrows(PersistenceException.class, () -> {
            clubLayoutDao.save(invalid);
            em.flush();
        });
    }

    // ----------------- Tests findByClubId -----------------

    @Test
    public void testFindByClubIdReturnsLayout() {
        ClubLayout layout = buildNewLayoutForClub(clubId);
        em.persist(layout);
        em.flush();

        Optional<ClubLayout> result = clubLayoutDao.findByClubId(clubId);

        assertTrue(result.isPresent());
        assertEquals(clubId, result.get().getClubId());
    }

    @Test
    public void testFindByClubIdReturnsEmptyWhenNotFound() {
        Optional<ClubLayout> result = clubLayoutDao.findByClubId(UUID.randomUUID());
        assertFalse(result.isPresent());
    }

    // ----------------- Tests deleteByClubId -----------------

    @Test
    public void testDeleteByClubIdDeletesLayout() {
        ClubLayout layout = buildNewLayoutForClub(clubId);
        em.persist(layout);
        em.flush();

        assertEquals(1L, countLayouts());

        clubLayoutDao.deleteByClubId(clubId);
        em.flush();

        assertEquals(0L, countLayouts());
    }

    @Test
    public void testDeleteByClubIdDoesNothingWhenNoLayout() {
        assertEquals(0L, countLayouts());

        clubLayoutDao.deleteByClubId(UUID.randomUUID());
        em.flush();

        assertEquals(0L, countLayouts());
    }


    // ----------------- Helpers -----------------

    private User persistUser() {
        User u = new User(
                UUID.randomUUID(),
                "user", "name", "last", "email",
                "{noop}hash",
                "cell",
                Role.USER,
                OffsetDateTime.now(),
                null
        );
        em.persist(u);
        return u;
    }

    private Club persistClub(User contact) {
        Club c = new Club(
                "Club Test",
                "Location",
                new BigDecimal("5.0"),
                1,
                null,
                "desc",
                contact,
                null,
                OffsetDateTime.now(),
                null,
                Boolean.FALSE,
                null
        );
        em.persist(c);
        return c;
    }

    private long countLayouts() {
        return em.createQuery("SELECT COUNT(l) FROM ClubLayout l", Long.class)
                .getSingleResult();
    }

    private ClubLayout buildNewLayoutForClub(UUID clubId) {
        String url = "/clubs/club/" + clubId + "/layout/fake-id/image";
        return new ClubLayout(
                clubId,
                uploader.getId(),
                url,
                new byte[]{1, 2, 3},
                "layout.png",
                "image/png",
                3L
        );
    }
}
