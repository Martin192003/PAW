package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.BookingForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping
public class BookingController {

    private final BookingService bookingService;
    private final TableService tableService;
    private final UserService userService;
    private final ClubService clubService;
    private  final EmailService emailService;

    public BookingController(BookingService bookingService,
                             TableService tableService,
                             UserService userService, ClubService clubService, EmailService emailService) {
        this.bookingService = bookingService;
        this.tableService = tableService;
        this.userService = userService;
        this.clubService = clubService;
        this.emailService = emailService;
    }

    @PostMapping("/clubs/{clubId}/reserve")
    public String reserve(@PathVariable UUID clubId,
                          @RequestParam UUID tableId,
                          @RequestParam String date,
                          Principal principal,
                          RedirectAttributes ra) {

        var maybeTable = tableService.findById(tableId);
        if (maybeTable.isEmpty()) {
            throw new TableNotFoundException(tableId);
        }
        var table = maybeTable.get();
        if (!clubId.equals(table.getClubId())) {
            throw new BadRequestException("El club no existe");
        }

        var zone = java.time.ZoneId.of("America/Argentina/Buenos_Aires");
        var selectedDate = java.time.LocalDate.parse(date);
        var dow = selectedDate.getDayOfWeek();
        if (!(dow == java.time.DayOfWeek.THURSDAY
                || dow == java.time.DayOfWeek.FRIDAY
                || dow == java.time.DayOfWeek.SATURDAY)) {
            throw new InvalidDateRangeException("Solo se puede reservar para jueves, viernes o sábado.");
        }

        var start = selectedDate.atTime(23, 59).atZone(zone).toOffsetDateTime();
        var end   = selectedDate.plusDays(1).atTime(6, 0).atZone(zone).toOffsetDateTime();

        var libres = tableService.findAvailableByClubAndRange(clubId, start, end);
        boolean disponible = libres.stream().anyMatch(t -> t.getTableId().equals(tableId));
        if (!disponible) {
            throw new BadRequestException("La mesa no está disponible en el rango indicado.");
        }

        var user = userService.findByUserName(principal.getName());
        var total = table.getPrice() != null ? table.getPrice() : java.math.BigDecimal.ZERO;
        var booking = bookingService.create(user, tableId, start, end, total);

        var club = clubService.getClubById(clubId).orElseThrow(IllegalArgumentException::new);

        emailService.sendTemplatedEmail(
                user.getEmail(),
                "Reserva confirmada - " + club.getClubName(),
                "table-reserved-user",
                java.util.Map.of(
                        "clubName", club.getClubName(),
                        "tableNumber", table.getTableNumber(),
                        "capacity", table.getCapacity(),
                        "date", selectedDate.toString(),
                        "from", "23:59",
                        "to", "06:00"
                ),
                "Ver reserva", "/bookings/" + booking.getBookingId()
        );

        String ownerEmail = resolveOwnerEmail(club); // método de abajo

        var esAR = new java.util.Locale("es", "AR");
        var df = java.time.format.DateTimeFormatter.ofPattern("EEE dd/MM", esAR);
        String dateTimePretty = df.format(selectedDate) + " 23:59–06:00";

        String ctaUrl = "/clubs/" + clubId + "?date=" + selectedDate;

        java.util.Map<String, Object> ownerModel = new java.util.HashMap<>();
        ownerModel.put("clubName", club.getClubName());
        ownerModel.put("tableNumber", table.getTableNumber());
        ownerModel.put("capacity", table.getCapacity());
        ownerModel.put("dateTime", dateTimePretty);

        ownerModel.put("username", user.getUsername());
        ownerModel.put("userEmail", user.getEmail());
        ownerModel.put("ctaUrl", ctaUrl);
        ownerModel.put("ctaText", "Abrir panel");


        ownerModel.put("brandName", "BlingBling");
        ownerModel.put("brandFooter", "© 2025 BlingBling. Panel de administración.");
        ownerModel.put("sentAt", java.time.OffsetDateTime.now(zone)); // para #temporals.format

        if (isLikelyEmail(ownerEmail)) {
            emailService.sendTemplatedEmail(
                    ownerEmail,
                    "Nueva reserva de mesa - " + club.getClubName(),
                    "table-reserved-owner",
                    ownerModel,
                    "Abrir panel", ctaUrl
            );
        } else {
            throw new UserNotFoundException("Email del dueño inválido: " + ownerEmail);
        }
        ra.addFlashAttribute("reservationSent", true);
        ra.addFlashAttribute("success", "¡Reserva creada para " + selectedDate + " (23:59–06:00)!");
        return "redirect:/clubs/" + clubId + "?date=" + selectedDate; // volvemos con la fecha elegida
    }

    private String resolveOwnerEmail(Club club) {
        String email = null;
        if (club.getContactInfo() != null) {
            email = club.getContactInfo().getEmail();
        }
        email = (email == null) ? "" : email.trim();
        if (email.isEmpty()) {
            email = "owner@" + club.getClubName()
                    .toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "") + ".test";
        }
        return email;
    }

    private boolean isLikelyEmail(String s) {
        return s != null && s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }



    /* ---------- Vistas ---------- */

    /** Form para crear reserva de una mesa */
    @GetMapping("/tables/{tableId}/bookings/new")
    public String newBooking(@PathVariable UUID tableId, Model model) {
        final Optional<Table> tableOpt = tableService.findById(tableId);
        if (tableOpt.isEmpty()) {
            throw new TableNotFoundException(tableId);
        }
        if (!model.containsAttribute("bookingForm")) {
            model.addAttribute("bookingForm", new BookingForm());
        }
        model.addAttribute("table", tableOpt.get());
        return "bookings/new"; // JSP: /WEB-INF/jsp/bookings/new.jsp
    }

    /** Detalle de una reserva */
    @GetMapping("/bookings/{bookingId}")
    public String show(@PathVariable UUID bookingId, Model model) {
        final Optional<Booking> b = bookingService.findById(bookingId);
        if (b.isEmpty()) throw new BookingNotFoundException("La reserva no existe");
        model.addAttribute("booking", b.get());
        return "bookings/show"; // JSP: /WEB-INF/jsp/bookings/show.jsp
    }

    /* ---------- Acciones ---------- */

    /** Crear reserva (queda PENDIENTE y bloquea la mesa) */
    @PostMapping("/tables/{tableId}/bookings")
    public String create(@PathVariable UUID tableId,
                         @Valid @ModelAttribute("bookingForm") BookingForm form,
                         BindingResult br,
                         Principal principal,
                         RedirectAttributes ra) {

        if (br.hasErrors()) {
            throw new ValidationException(br.getAllErrors().get(0).getDefaultMessage());
        }

        final User user = userService.findByUserName(principal.getName());
        final Booking b = bookingService.create(
                user,
                tableId,
                form.getStart(),
                form.getEnd(),
                form.getTotal()
        );

        ra.addFlashAttribute("success", "Reserva creada en estado PENDIENTE");
        return "redirect:/bookings/" + b.getBookingId();
    }

    /** Confirmar reserva → RESERVADA */
    @PostMapping("/bookings/{bookingId}/confirm")
    public String confirm(@PathVariable UUID bookingId, RedirectAttributes ra) {
        final Booking updated = bookingService.updateStatus(bookingId, Status.RESERVADA);
        if(updated == null) {
            throw new BookingNotFoundException("La reserva no existe");
        }
        ra.addFlashAttribute("success", "Reserva confirmada");
        return "redirect:/bookings/" + updated.getBookingId();
    }

    /** Cancelar / liberar → DISPONIBLE */
    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancel(@PathVariable UUID bookingId, RedirectAttributes ra) {
        final Booking updated = bookingService.updateStatus(bookingId, Status.DISPONIBLE);
        if(updated == null) {
            throw new BookingNotFoundException("La reserva no existe");
        }
        ra.addFlashAttribute("success", "Reserva cancelada y mesa liberada");
        return "redirect:/bookings/" + updated.getBookingId();
    }
}
