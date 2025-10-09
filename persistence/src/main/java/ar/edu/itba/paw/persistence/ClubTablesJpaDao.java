package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.Status;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class ClubTablesJpaDao implements ClubTablesDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public ClubTable create(int number, int capacity, UUID clubId, TableState tableState,
                            BigDecimal price, BigDecimal discount) {

        ClubTable t = new ClubTable();
        t.setTableId(UUID.randomUUID());
        t.setClubId(clubId);
        t.setTableNumber(number);
        t.setCapacity(capacity);
        t.setTableState(tableState);
        t.setPrice(price);
        t.setDiscount(discount);
        em.persist(t);
        return t;
    }

    @Override
    @Transactional
    public ClubTable update(UUID tableId, UUID clubId, Integer tableNumber, Integer capacity,
                        TableState state, BigDecimal price, BigDecimal discount) {

        ClubTable t = em.find(ClubTable.class, tableId);
        if (t == null) return null;

        t.setClubId(clubId);
        if (tableNumber != null) t.setTableNumber(tableNumber);
        if (capacity != null) t.setCapacity(capacity);
        if (state != null) t.setTableState(state);
        if (price != null) t.setPrice(price);
        if (discount != null) t.setDiscount(discount);

        return t;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClubTable> findAvailableByClubAndRange(UUID clubId, OffsetDateTime from, OffsetDateTime to) {
        return em.createQuery("""
                SELECT ct
               FROM ClubTable ct
               WHERE ct.clubId = :clubId
               AND NOT EXISTS (
                   SELECT 1
                   FROM Booking b
                   WHERE b.table = ct.id
                   AND b.status IN (:s1, :s2)
                   AND b.bookingStart < :to
                   AND b.bookingEnd   > :from
               )
                ORDER BY ct.tableNumber ASC
               """, ClubTable.class)
                .setParameter("clubId", clubId)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("s1", Status.PENDIENTE)
                .setParameter("s2", Status.CONFIRMADA)
                .getResultList();
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        Table t = em.find(Table.class, id);
        if (t == null) return false;
        em.remove(t);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClubTable> findById(UUID id) {
        return Optional.ofNullable(em.find(ClubTable.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClubTable> findAll() {
        return em.createQuery("""
                SELECT ct
                FROM ClubTable ct
                ORDER BY ct.tableNumber ASC
                """, ClubTable.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClubTable> findByClub(Club club) {
        return em.createQuery("""
                SELECT ct
                FROM ClubTable ct
                WHERE ct.clubId = :cid
                ORDER BY ct.tableNumber ASC
                """, ClubTable.class)
                .setParameter("cid", club.getClubId())
                .getResultList();
    }
}
