package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.exceptions.ClubNotFoundException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.persistence.ProductsDao;
import org.springframework.stereotype.Service;
import ar.edu.itba.paw.service.ProductService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
        if(basePrice.compareTo(BigDecimal.valueOf(100000000)) >= 0)
            throw new ValidationException("Precio demasiado alto");
        if(name.length() > 100)
            throw new IllegalArgumentException("Nombre demasiado largo");
        List<Product> current = productsDao.findByClub(club);
        if (current.size() >= MAX_MENU_PRODUCTS) {
            throw new ValidationException("max_products_menu");
        }
        return productsDao.create(club, name, description, basePrice, type, isAvailable);
    }

    @Override
    @Transactional
    public Product update(UUID productId, UUID clubId, String name, String description, BigDecimal basePrice, ProductType type, Boolean isAvailable) {
        if(basePrice.compareTo(BigDecimal.valueOf(100000000)) >= 0)
            throw new ValidationException("Precio demasiado alto");
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<Product>> getVisibleProductsByCategory(UUID clubId) { // <-- Recibe UUID

        // Usa findByClubID en lugar de findByClub
        var products = this.findByClubID(clubId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()))
                .sorted(Comparator.comparing(Product::getProductName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        var grouped = products.stream().collect(Collectors.groupingBy(this::classifyCategory));
        var desired = List.of("Cervezas", "Tragos", "Champagne", "Sin Alcohol");
        var ordered = new LinkedHashMap<String, List<Product>>();
        desired.forEach(cat -> {
            var list = grouped.remove(cat);
            if (list != null && !list.isEmpty()) {
                ordered.put(cat, list);
            }
        });
        grouped.entrySet().stream().sorted(Map.Entry.comparingByKey(String::compareToIgnoreCase)).forEach(e -> ordered.put(e.getKey(), e.getValue()));
        if (ordered.isEmpty() && !products.isEmpty()) {
            ordered.put("Tragos", products);
        }
        return ordered;
    }

}
