package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.persistence.ProductsDao;
import org.springframework.stereotype.Service;
import ar.edu.itba.paw.service.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductsDao productsDao;

    public ProductServiceImpl(ProductsDao productsDao) {
        this.productsDao = productsDao;
    }

    @Override
    public Product create(Club club, String name, String description, BigDecimal basePrice, ProductType type, boolean isAvailable) {
        if(basePrice.compareTo(BigDecimal.valueOf(100000000)) >= 0)
            throw new ValidationException("Precio demasiado alto");
        if(name.length() > 100)
            throw new IllegalArgumentException("Nombre demasiado largo");
        return productsDao.create(club, name, description, basePrice, type, isAvailable);
    }

    @Override
    public Product update(UUID productId, UUID clubId, String name, String description, BigDecimal basePrice, ProductType type, Boolean isAvailable) {
        if(basePrice.compareTo(BigDecimal.valueOf(100000000)) >= 0)
            throw new ValidationException("Precio demasiado alto");
        return productsDao.update(productId, clubId, name, description, basePrice, type, isAvailable);
    }

    @Override
    public boolean delete(UUID id) {
        return productsDao.delete(id);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return productsDao.findById(id);
    }

    @Override
    public Optional<Product> findByName(String name) {
        return productsDao.findByName(name);
    }

    @Override
    public List<Product> findAll() {
        return productsDao.findAll();
    }

    @Override
    public List<Product> findByClub(Club club) {
        return productsDao.findByClub(club);
    }

    @Override
    public List<Product> findByClubID(UUID clubId) {
        return productsDao.findByClubID(clubId);
    }
}
