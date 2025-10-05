package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.persistence.ProductsDao;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.service.SecurityService;
import ar.edu.itba.paw.service.TableService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    private final TableService tableService;
    private final SecurityService securityService;

    private final ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
    private final Clock clock = Clock.system(zone);

    public ClubDetailsController(final ClubService clubService,
                                 final ProductsDao productsDao,
                                 final PhotoService photoService,
                                 final TableService tableService,
                                 final SecurityService securityService) {
        this.clubService = clubService;
        this.productsDao = productsDao;
        this.photoService = photoService;
        this.tableService = tableService;
        this.securityService = securityService;
    }

    @GetMapping("/{clubId}")
    public String clubDetails(@PathVariable UUID clubId,
                              @RequestParam(name = "date", required = false) String dateStr,
                              Model model) {

        var club = clubService.getClubById(clubId)
                .orElseThrow(() -> new NotFoundException("El boliche no existe."));

        // ====== FECHA: ahora delega en TableService ======
        LocalDate selectedDate;
        if (dateStr == null || dateStr.isBlank()) {
            // Elegimos la próxima fecha permitida iterando con la regla del servicio
            selectedDate = nextAllowedFrom(LocalDate.now(zone));
        } else {
            selectedDate = LocalDate.parse(dateStr);
            if (!tableService.isAllowedDay(selectedDate)) {
                throw new InvalidDateRangeException("La fecha no es uno de los días habilitados");
            }
            if (tableService.isPastDate(selectedDate, clock, zone)) {
                throw new InvalidDateRangeException("La fecha está en el pasado");
            }
        }

        var start = tableService.windowStart(selectedDate, zone);
        var end   = tableService.windowEnd(selectedDate, zone);

        // ====== PRODUCTOS (igual que antes) ======
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

        // ====== MESAS DISPONIBLES usando la regla del servicio ======
        var available = tableService.findAvailableOnDate(clubId, selectedDate, zone, clock);

        // ====== COVER ======
        var photos = photoService.listByClub(clubId, 0, 1);
        String coverPhotoUrl = photos.isEmpty() ? null : photos.get(0).getUrl();

        User user = securityService.getVerifiedLoggedUser();
        if (user != null) {
            model.addAttribute("user", user);
        }

        // ====== Modelo esperado por el JSP ======
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

    // ====== Helpers de presentación (no negocio) ======

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

    private String toLocationText(final String location) {
        return (location == null || location.isBlank()) ? "Sin ubicación" : location;
    }

    private String toGenreText(final String genre) {
        return (genre == null || genre.isBlank()) ? "—" : genre;
    }

    private LocalDate nextAllowedFrom(LocalDate from) {
        LocalDate d = from;
        int guard = 0;
        while (!tableService.isAllowedDay(d) || tableService.isPastDate(d, clock, zone)) {
            d = d.plusDays(1);
            if (++guard > 14) break; // por las dudas no quede en loop infinito
        }
        return d;
    }
}
