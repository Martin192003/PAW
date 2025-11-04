package ar.edu.itba.paw.webapp.controller;


import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.BookingNotFoundException;
import ar.edu.itba.paw.model.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class BookingAdminController {

    private final BookingService bookingService;
    private final SecurityService securityService;
    private final ClubService clubService;
    private final TableService tableService;
    private final UserService userService;
    private final ReceiptService receiptService;
    private final PhotoService photoService;

    public BookingAdminController(BookingService bookingService, SecurityService securityService, ClubService clubService, TableService tableService, UserService userService, ReceiptService receiptService, PhotoService photoService) {
        this.bookingService = bookingService;
        this.securityService = securityService;
        this.clubService = clubService;
        this.tableService = tableService;
        this.userService = userService;
        this.receiptService = receiptService;
        this.photoService = photoService;
    }

    @GetMapping("/activity")
    public String userClubs(@RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            Model model) {

        if (page < 1) page = 1;
        if (size < 1) size = 10;
        int offset = (page - 1) * size;

        User current = securityService.getVerifiedLoggedUser();
        UUID userId = current.getId();

        List<Club> clubs = clubService.getClubsByUserPaginated(userId, size, offset);
        int totalClubs = clubService.countClubsByUser(userId);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalClubs / size));

        Map<UUID,String> photoUrlByClub = photoService.getCoverUrls(
                clubs.stream().map(Club::getClubId).collect(Collectors.toSet())
        );

        model.addAttribute("user", current);
        model.addAttribute("clubs", clubs);
        model.addAttribute("photoUrlByClub", photoUrlByClub);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("activePage", "clubs");
        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalResults", totalClubs);
        return "clubActivity";
    }


    @PostMapping("/bookings/{bookingId}/confirm")
    public String confirm(@PathVariable UUID bookingId, RedirectAttributes ra) {
        try {
            bookingService.confirm(bookingId);
            ra.addFlashAttribute("success", "booking.confirmed");
            ra.addFlashAttribute("bookingAction", "confirmed");
        } catch (InvalidStatusTransitionException e) {
            ra.addFlashAttribute("error", "error.booking.cannotConfirm");
        } catch (BookingNotFoundException e) {
            ra.addFlashAttribute("error", "booking.notfound");
        }
        return "redirect:/admin/booking?bookingId=" + bookingId;
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancel(@PathVariable UUID bookingId, RedirectAttributes ra) {
        try {
            bookingService.cancel(bookingId);
            ra.addFlashAttribute("success", "booking.cancelled");
            ra.addFlashAttribute("bookingAction", "cancelled");
        } catch (InvalidStatusTransitionException e) {
            ra.addFlashAttribute("error", "error.booking.cannotCancel");
        } catch (BookingNotFoundException e) {
            ra.addFlashAttribute("error", "booking.notfound");
        }
        return "redirect:/admin/booking?bookingId=" + bookingId;
    }

    @GetMapping(value = "/clubs/bookings", produces = "text/html;charset=UTF-8")
    public String bookingsOfMyClubs(@ModelAttribute("bookingSearchForm") BookingSearchForm form,
                                    BindingResult br,
                                    Model model) {

        final int size = 10;
        final int page = Math.max(form.getPage(), 1);
        final int offset = (page - 1) * size;

        final User current = securityService.getVerifiedLoggedUser();

        UUID bookingUuid = null;
        if (form.getBookingId() != null && !form.getBookingId().isBlank()) {
            try {
                bookingUuid = UUID.fromString(form.getBookingId().trim());
            } catch (IllegalArgumentException e) {
                br.rejectValue("bookingId", "bookingId.invalid", "El ID de reserva ingresado no es válido.");
            }
        }

        List<Booking> result = List.of();
        int totalBookings = 0;

        if (!br.hasErrors()) {
            result = bookingService.searchForAdminPaginated(
                    current.getId(),
                    form.getClubId(),
                    bookingUuid,
                    form.getStatus(),
                    form.getSort(),
                    size, offset
            );
            totalBookings = bookingService.countSearchForAdmin(
                    current.getId(),
                    form.getClubId(),
                    bookingUuid,
                    form.getStatus()
            );
        }

        int totalPages = (int) Math.ceil((double) totalBookings / size);

        Map<String, String> filters = new LinkedHashMap<>();

        if (form.getClubId() != null) filters.put("clubId", form.getClubId().toString());
        if (form.getBookingId() != null && !form.getBookingId().isBlank()) filters.put("bookingId", form.getBookingId());
        if (form.getStatus() != null) filters.put("status", form.getStatus().name());
        if (form.getSort() != null && !form.getSort().isBlank()) filters.put("sort", form.getSort());


        String queryParams = filters.entrySet().stream()
                .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));

        Map<UUID, Boolean> hasReceiptMap = new HashMap<>();
        for (Booking b : result) {
            hasReceiptMap.put(b.getBookingId(), receiptService.findByBooking(b.getBookingId()).isPresent());
        }
        model.addAttribute("hasReceiptMap", hasReceiptMap);

        model.addAttribute("bookings", result);
        model.addAttribute("clubNames", clubService.mapClubNames(result));
        model.addAttribute("totalResults", totalBookings);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("queryParams", queryParams);

        model.addAttribute("sort", form.getSort());
        model.addAttribute("status", form.getStatus());
        model.addAttribute("clubId", form.getClubId());
        model.addAttribute("bookingId", form.getBookingId());

        model.addAttribute("clubs", clubService.getClubsByUser(current.getId()));

        model.addAttribute("user", current);
        model.addAttribute("activePage", "clubsBookings");
        model.addAttribute("bookingSearchForm", form);

        return "clubsBookings";
    }


    @GetMapping("/booking")
    public String bookingAdminHandler(@RequestParam UUID bookingId, Model model, @ModelAttribute("error") String error, @ModelAttribute("success") String success) {
        Optional<Booking> bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            model.addAttribute("error", "Reserva no encontrada");
            return "error/error";
        }
        Booking booking = bookingOpt.get();

        Optional<ClubTable> tableOpt = tableService.findById(booking.getTableId());
        ClubTable table = tableOpt.orElse(null);

        String clubName = "";
        String clubCbu = "";
        if (table != null) {
            Optional<Club> clubOpt = clubService.getClubById(table.getClubId());
            if (clubOpt.isPresent()) {
                Club club = clubOpt.get();
                clubName = club.getClubName();
                clubCbu = club.getCbu();
            }
        }

        var rOpt = receiptService.findByBooking(bookingId);
        model.addAttribute("hasReceipt", rOpt.isPresent());
        rOpt.ifPresent(r -> model.addAttribute("receipt", r));

        Optional<User> u = userService.findById(booking.getUser().getId());
        model.addAttribute("user", u.orElse(null));

        model.addAttribute("booking", booking);
        model.addAttribute("table", table);
        model.addAttribute("clubName", clubName);
        model.addAttribute("clubCbu", clubCbu);


        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withLocale(new Locale("es","AR"));

        String bookingStartLocal = booking.getBookingStart().atZoneSameInstant(zone).format(fmt);
        String bookingEndLocal   = booking.getBookingEnd().atZoneSameInstant(zone).format(fmt);

        model.addAttribute("bookingStartLocal", bookingStartLocal);
        model.addAttribute("bookingEndLocal", bookingEndLocal);

        if (error != null && !error.isEmpty()) {
            model.addAttribute("errorKey", error);
        }
        if (success != null && !success.isEmpty()) {
            model.addAttribute("success", success);
        }
        return "bookingAdminHandler";
    }

    @PostMapping("/bookings/{bookingId}/receipt/delete")
    public String deleteReceipt(@PathVariable UUID bookingId, RedirectAttributes ra) {
        boolean deleted = receiptService.deleteByBooking(bookingId);
        ra.addFlashAttribute(deleted ? "success" : "info",
                deleted ? "receipt.deleted" : "receipt.notfound");
        return "redirect:/admin/booking?bookingId=" + bookingId;
    }
}
