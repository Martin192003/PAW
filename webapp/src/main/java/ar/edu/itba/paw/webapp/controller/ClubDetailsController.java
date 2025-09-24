package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.ProductsDao;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.SecurityService;
import ar.edu.itba.paw.service.TableService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import ar.edu.itba.paw.service.PhotoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Controller
@RequestMapping("/clubs")
public class ClubDetailsController {

    private final ClubService clubService;
    private final ProductsDao productsDao;
    private final PhotoService photoService;
    private final TableService clubTables;
    private final SecurityService securityService;

    // Constructor injection (recomendado con Spring 5.x)
    public ClubDetailsController(final ClubService clubService, final ProductsDao productsDao, final PhotoService photoService, TableService clubTables, SecurityService securityService) {
        this.clubService = clubService;
        this.productsDao = productsDao;
        this.photoService = photoService;
        this.clubTables = clubTables;
        this.securityService = securityService;
    }

    @GetMapping("/{clubId}")
    public String clubDetails(@PathVariable UUID clubId,
                              @RequestParam(name = "date", required = false) String dateStr,
                              Model model) {

        var maybeClub = clubService.getClubById(clubId);
        if (maybeClub.isEmpty()) throw new NotFoundException("El boliche no existe.");
        var club = maybeClub.get();

        var zone = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDate selectedDate;
        if (dateStr == null || dateStr.isBlank()) {
            var today = LocalDate.now(zone);
            selectedDate = nextAllowedThuFriSat(today);
        } else {
            selectedDate = LocalDate.parse(dateStr);
            if (!isThuFriSat(selectedDate)) {
               throw new InvalidDateRangeException("La fecha de inicio es incorrecta");
            }
        }

        var start = selectedDate.atTime(23, 59).atZone(zone).toOffsetDateTime();
        var end   = selectedDate.plusDays(1).atTime(6, 0).atZone(zone).toOffsetDateTime();

        // --- productos (como ya lo tenías)
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
        if (ordered.isEmpty() && !products.isEmpty()) ordered.put("Tragos", products);

        var available = clubTables.findAvailableByClubAndRange(clubId, start, end);

        var photos = photoService.listByClub(clubId, 0, 1);
        String coverPhotoUrl = photos.isEmpty() ? null : photos.get(0).getUrl();

        User user = securityService.getVerifiedLoggedUser();
        if(user!=null){
            model.addAttribute("user", user);
        }

        model.addAttribute("club", club);
        model.addAttribute("productsByCategory", ordered);
        model.addAttribute("availableTables", available);
        model.addAttribute("locationText", toLocationText(club.getLocation()));
        model.addAttribute("genreText", toGenreText(club.getMusicGenre() != null ? club.getMusicGenre().getLabel() : null));
        model.addAttribute("coverPhotoUrl", coverPhotoUrl);
        model.addAttribute("clubId", clubId);

        model.addAttribute("selectedDate", selectedDate.toString()); // yyyy-MM-dd
        model.addAttribute("dayStart", start);
        model.addAttribute("dayEnd", end);

        return "clubDetails";
    }


    private String classifyCategory(final Product p) {
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

    /** Para tus datos actuales, location ya es String. */
    private String toLocationText(final String location) {
        return (location == null || location.isBlank()) ? "Sin ubicación" : location;
    }

    /** Para tus datos actuales, genre ya es String (ej: “Electrónica, Reggaetón”). */
    private String toGenreText(final String genre) {
        return (genre == null || genre.isBlank()) ? "—" : genre;
    }

    private static boolean isThuFriSat(java.time.LocalDate d) {
        var dow = d.getDayOfWeek(); // MON..SUN
        return dow == java.time.DayOfWeek.THURSDAY
                || dow == java.time.DayOfWeek.FRIDAY
                || dow == java.time.DayOfWeek.SATURDAY;
    }

    private static java.time.LocalDate nextAllowedThuFriSat(java.time.LocalDate from) {
        java.time.LocalDate d = from;
        while (!isThuFriSat(d)) d = d.plusDays(1);
        return d;
    }
}
