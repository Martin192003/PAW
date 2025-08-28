package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductsDao {
    Product findById(UUID id);
    Product findByName(String name);

    List<Product> findAll();
    List<Product> findByClub(Club club);

    Product create(Club club, String name, String description, BigDecimal basePrice, ProductType type, boolean isAvailable);
    Product update(Product product);
    boolean delete(UUID id);
}
