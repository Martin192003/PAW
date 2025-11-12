package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.exceptions.ClubNotFoundException;
import ar.edu.itba.paw.model.exceptions.InvalidPriceException;
import ar.edu.itba.paw.model.exceptions.MaxProductsReachedException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.persistence.ProductsDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductsDao productsDao;
    private final ClubService clubService;
    private static final int MAX_MENU_PRODUCTS = 50;

    public ProductServiceImpl(ProductsDao productsDao, ClubService clubService) {
        this.productsDao = productsDao;
        this.clubService = clubService;
    }

    @Override
    @Transactional
    public Product create(UUID clubID, String name, String description, BigDecimal basePrice, ProductType type, boolean isAvailable) {
        Club club = clubService.getClubById(clubID).orElseThrow(ClubNotFoundException::new);

        List<Product> current = productsDao.findByClub(club);
        if (current.size() >= MAX_MENU_PRODUCTS) {
            throw new MaxProductsReachedException(clubID);
        }
        if (basePrice.compareTo(BigDecimal.valueOf(100000000)) >= 0) {
            throw new InvalidPriceException(clubID, "price too high");
        }
        return productsDao.create(club, name, description, basePrice, type, isAvailable);
    }

    @Override
    @Transactional
    public Product update(UUID productId, UUID clubId, String name, String description, BigDecimal basePrice, ProductType type, Boolean isAvailable) {
        if (basePrice.compareTo(BigDecimal.valueOf(100000000)) >= 0) {
            throw new InvalidPriceException(clubId, productId, "price too high");
        }
        return productsDao.update(productId, clubId, name, description, basePrice, type, isAvailable);
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        return productsDao.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(UUID id) {
        return productsDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findByName(String name) {
        return productsDao.findByName(name);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Product> findByClub(Club club) {
        return productsDao.findByClub(club);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByClubID(UUID clubId) {
        return productsDao.findByClubID(clubId);
    }

    @Override
    public String classifyCategory(final Product p) {
        final String name = Optional.ofNullable(p.getProductName())
                .orElse("")
                .toLowerCase(Locale.ROOT);

        if (name.contains("cerveza") || name.contains("ipa") || name.contains("lager")
                || name.contains("stout") || name.contains("apa")) {
            return "CERVEZAS";
        }
        if (name.contains("champ") || name.contains("espum") || name.contains("brut")) {
            return "CHAMPAGNE";
        }

        ProductType type = p.getProductType();
        if (type == ProductType.NO_ALCOHOLICA) return "NO_ALCOHOLICA";
        if (type == ProductType.ALCOHOLICA) return "ALCOHOLICA";

        return "TRAGOS";
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAddProduct(Club club) {
        if (club == null) return false;
        List<Product> current = productsDao.findByClub(club);
        return current.size() < MAX_MENU_PRODUCTS;
    }

}
