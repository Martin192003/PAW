package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.ProductsDao;
import ar.edu.itba.paw.service.ClubService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import ar.edu.itba.paw.service.PhotoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Controller
@RequestMapping("/clubs")
public class ClubDetailsController {

    private final ClubService clubService;
    private final ProductsDao productsDao;
    private final PhotoService photoService;

    // Constructor injection (recomendado con Spring 5.x)
    public ClubDetailsController(final ClubService clubService, final ProductsDao productsDao, final PhotoService photoService) {
        this.clubService = clubService;
        this.productsDao = productsDao;
        this.photoService = photoService;
    }

    @GetMapping("/{clubId}") // <- detalles en /clubs/{clubId}
    public String clubDetails(@PathVariable UUID clubId, Model model) {
        var maybeClub = clubService.getClubById(clubId);
        if (maybeClub.isEmpty()) throw new NotFoundException("El boliche no existe.");

        var photos = photoService.listByClub(clubId, 0, 1);
        String coverPhotoUrl = photos.isEmpty() ? null : photos.get(0).getUrl();
        var club = maybeClub.get();

        var products = productsDao.findByClub(club).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()))
                .sorted(Comparator.comparing(Product::getProductName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        var grouped = products.stream().collect(Collectors.groupingBy(this::classifyCategory));

        var desired = List.of("Cervezas","Tragos","Champagne","Sin Alcohol");
        var ordered = new LinkedHashMap<String, List<Product>>();
        desired.forEach(cat -> {
            var list = grouped.remove(cat);
            if (list != null && !list.isEmpty()) ordered.put(cat, list);
        });
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String::compareToIgnoreCase))
                .forEach(e -> ordered.put(e.getKey(), e.getValue()));

        if (ordered.isEmpty() && !products.isEmpty()) {
            ordered.put("Tragos", products);
        }

        model.addAttribute("club", club);
        model.addAttribute("productsByCategory", ordered);
        model.addAttribute("locationText", toLocationText(club.getLocation()));
        model.addAttribute("genreText", toGenreText(club.getMusicGenre() != null ? club.getMusicGenre().getLabel() : null));
        model.addAttribute("coverPhotoUrl", coverPhotoUrl);

        return "clubDetails";
    }


    private String classifyCategory(final Product p) {
        final String name = Optional.ofNullable(p.getProductName())
                .orElse("")
                .toLowerCase(Locale.ROOT);

        if (name.contains("cerveza") || name.contains("ipa") || name.contains("lager")
                || name.contains("stout") || name.contains("apa")) {
            return "Cervezas";
        }
        if (name.contains("champ") || name.contains("espum") || name.contains("brut")) {
            return "Champagne";
        }


        ProductType type = p.getProductType();
        if (type == ProductType.NO_ALCOHOLICA) return "Sin Alcohol";

        // Default
        return "Tragos";
    }

    /** Para tus datos actuales, location ya es String. */
    private String toLocationText(final String location) {
        return (location == null || location.isBlank()) ? "Sin ubicación" : location;
    }

    /** Para tus datos actuales, genre ya es String (ej: “Electrónica, Reggaetón”). */
    private String toGenreText(final String genre) {
        return (genre == null || genre.isBlank()) ? "—" : genre;
    }
}
