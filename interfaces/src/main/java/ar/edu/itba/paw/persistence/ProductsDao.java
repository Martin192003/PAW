package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductsDao {
    Product create(Club club, String name, String description, BigDecimal basePrice, ProductType type, boolean isAvailable);

    Product update(UUID productId, UUID clubId, String name, String description, BigDecimal basePrice, ProductType type, Boolean isAvailable);

    boolean delete(UUID id);
    Optional<Product> findById(UUID id);
    List<Product> findByClub(Club club);
    List<Product> findByClubID(UUID clubId);
}

