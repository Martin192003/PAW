package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProductService {
    Product create(UUID clubID, String name, String description, BigDecimal basePrice, ProductType type, boolean isAvailable);

    Product update(UUID productId, UUID clubId, String name, String description, BigDecimal basePrice, ProductType type, Boolean isAvailable);

    boolean delete(UUID id);
    Optional<Product> findById(UUID id);
    Optional<Product> findByName(String name);
    List<Product> findByClub(Club club);
    List<Product> findByClubID(UUID clubId);
    String classifyCategory(Product p);
    boolean canAddProduct(Club club);
    public Map<String, List<Product>> getVisibleProductsByCategory(UUID clubId);
}
