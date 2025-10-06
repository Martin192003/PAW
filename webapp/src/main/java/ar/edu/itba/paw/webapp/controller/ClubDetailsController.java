package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Controller
@RequestMapping("/clubs")
public class ClubDetailsController {

    private final ClubService clubService;
    private final ProductService productsService;
    private final PhotoService photoService;
    private final TableService tableService;
    private final SecurityService securityService;

    private final ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
    private final Clock clock = Clock.system(zone);

    public ClubDetailsController(final ClubService clubService,
                                 final ProductService productsService,
                                 final PhotoService photoService,
                                 final TableService tableService,
                                 final SecurityService securityService) {
        this.clubService = clubService;
        this.productsService = productsService;
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

        LocalDate defaultDate = nextAllowedFrom(LocalDate.now(zone));
        String selectedDateStr = (dateStr != null && !dateStr.isBlank()) ? dateStr : defaultDate.toString();
        model.addAttribute("selectedDate", selectedDateStr);
        model.addAttribute("availableTables", List.of());

        OffsetDateTime start = null;
        OffsetDateTime end = null;
        List<ar.edu.itba.paw.model.Table> available = List.of();

        if (dateStr == null || dateStr.isBlank()) {
            var ld = defaultDate;
            available = tableService.findAvailableOnDate(clubId, ld, zone, clock);
            start = tableService.windowStart(ld, zone);
            end   = tableService.windowEnd(ld, zone);
        } else {
            try {
                var ld = LocalDate.parse(dateStr);
                available = tableService.findAvailableOnDate(clubId, ld, zone, clock);
                start = tableService.windowStart(ld, zone);
                end   = tableService.windowEnd(ld, zone);
            } catch (ValidationException | InvalidDateRangeException ex) {
                model.addAttribute("error", "invalid.date");
            } catch (DateTimeParseException ex) {
                model.addAttribute("error", "Fecha inválida.");
            }
        }

        var products = productsService.findByClub(club).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()))
                .sorted(Comparator.comparing(Product::getProductName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        var grouped = products.stream().collect(Collectors.groupingBy(productsService::classifyCategory));
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

        var photos = photoService.listByClub(clubId, 0, 1);
        String coverPhotoUrl = photos.isEmpty() ? null : photos.get(0).getUrl();

        User user = securityService.getVerifiedLoggedUser();
        if (user != null) {
            model.addAttribute("user", user);
        }

        model.addAttribute("club", club);
        model.addAttribute("productsByCategory", ordered);
        model.addAttribute("availableTables", available);
        model.addAttribute("locationText", toLocationText(club.getLocation()));
        model.addAttribute("genreText", toGenreText(club.getMusicGenre() != null ? club.getMusicGenre().getLabel() : null));
        model.addAttribute("coverPhotoUrl", coverPhotoUrl);
        model.addAttribute("clubId", clubId);

        if (start != null && end != null) {
            model.addAttribute("dayStart", start);
            model.addAttribute("dayEnd", end);
        }

        return "clubDetails";
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
            if (++guard > 14) break;
        }
        return d;
    }
}