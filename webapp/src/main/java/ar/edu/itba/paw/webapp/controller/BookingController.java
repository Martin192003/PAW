package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.BookingForm;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping
public class BookingController {

    private final BookingService bookingService;
    private final TableService tableService;
    private final UserService userService;
    private final ClubService clubService;
    private final EmailService emailService;
    private final SecurityService securityService;
    private final ReceiptService receiptService;

    public BookingController(BookingService bookingService,
                             TableService tableService,
                             UserService userService, ClubService clubService, EmailService emailService, SecurityService securityService, ReceiptService receiptService) {
        this.bookingService = bookingService;
        this.tableService = tableService;
        this.userService = userService;
        this.clubService = clubService;
        this.emailService = emailService;
        this.securityService = securityService;
        this.receiptService = receiptService;
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

        var username = principal != null ? principal.getName() : null;
        if (username == null) {
            throw new IllegalArgumentException("El usuario no está autenticado.");
        }

        var user = userService.findByUserName(username);
        if (user == null) {
            throw new UserNotFoundException("No se encontró el usuario: " + username);
        }
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
    @PostMapping("/admin/bookings/{bookingId}/confirm")
    public String confirm(@PathVariable UUID bookingId, RedirectAttributes ra) {
        try {
            var booking = bookingService.findById(bookingId)
                    .orElseThrow(() -> new BookingNotFoundException("booking.notfound"));
            if (booking.getStatus() == Status.CANCELADA) {
                ra.addFlashAttribute("error", "error.booking.cannotConfirmCancelled");
            } else {
                bookingService.updateStatus(bookingId, Status.CONFIRMADA);
                ra.addFlashAttribute("success", "booking.confirmed");
            }
        } catch (InvalidStatusTransitionException e) {
            ra.addFlashAttribute("error", "error.booking.cannotConfirm");
        } catch (BookingNotFoundException e) {
            ra.addFlashAttribute("error", "booking.notfound");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "error.internal");
        }
        return "redirect:/admin/booking?bookingId=" + bookingId;
    }

    @PostMapping("/admin/bookings/{bookingId}/cancel")
    public String cancel(@PathVariable UUID bookingId, RedirectAttributes ra) {
        try {
            // Opcional: validar existencia para dar error amable
            bookingService.findById(bookingId)
                    .orElseThrow(() -> new BookingNotFoundException("booking.notfound"));

            bookingService.cancel(bookingId);
            ra.addFlashAttribute("success", "booking.cancelled");
        } catch (InvalidStatusTransitionException e) {
            // No conviertas a BadRequestException; devolvé mensaje de usuario y redirigí
            ra.addFlashAttribute("error", "error.booking.cannotCancel");
        } catch (BookingNotFoundException e) {
            ra.addFlashAttribute("error", "booking.notfound");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "error.internal");
        }
        return "redirect:/admin/booking?bookingId=" + bookingId;
    }


    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancelUser(@PathVariable UUID bookingId, RedirectAttributes ra) {
        // Validate booking exists and user is allowed to cancel
        var booking = bookingService.findById(bookingId)
                .orElse(null);
        if (booking == null) {
            ra.addFlashAttribute("error", "booking.notfound");
            return "redirect:/booking?bookingId=" + bookingId;
        }

        var current = securityService.getVerifiedLoggedUser();
        if (booking.getUser() == null || !booking.getUser().getId().equals(current.getId())) {
            ra.addFlashAttribute("error", "booking.notAllowedToCancel");
            return "redirect:/booking?bookingId=" + bookingId;
        }

        try{
            bookingService.cancel(bookingId);
            ra.addFlashAttribute("success", "booking.cancelled");
        } catch (InvalidStatusTransitionException e) {
            // flash a user-friendly message (use message code)
            ra.addFlashAttribute("error", "error.booking.cannotCancel");
        } catch (BookingNotFoundException e) {
            ra.addFlashAttribute("error", "booking.notfound");
        } catch (Exception e) {
            // generic catch to avoid 500 and provide feedback
            ra.addFlashAttribute("error", "error.internal");
        }
        return "redirect:/booking?bookingId=" + bookingId;
    }

    /*
    ---------- VISTAS DE ADMIN ----------
     */

    //Lista las reservas de los clubes administrados por un usuario
    @GetMapping("/admin/clubs/bookings")
    public String bookingsOfMyClubs(@RequestParam UUID userId, Model model) {
        // Buscar los clubes administrados por el usuario
        List<Club> myClubs = clubService.getClubsByUser(userId);
        List<Booking> allBookings = new ArrayList<>();
        Map<UUID, String> clubNames = new HashMap<>();
        for (Club club : myClubs) {
            List<Table> tables = tableService.findByClubId(club.getClubId());
            for (Table table : tables) {
                List<Booking> bookings = bookingService.findByTable(table.getTableId());
                for (Booking booking : bookings) {
                    allBookings.add(booking);
                    clubNames.put(booking.getBookingId(), club.getClubName());
                }
            }
        }
        User user = securityService.getVerifiedLoggedUser();
        model.addAttribute("user", user);
        model.addAttribute("bookings", allBookings);
        model.addAttribute("clubNames", clubNames);
        model.addAttribute("activePage", "clubsBookings");
        return "clubsBookings";
    }

    /*
    ---------- VISTAS DE USUARIO ----------
     */

    //Lista las reservas hechas por un usuario
    @GetMapping("/bookings")
    public String listUserBookings(
            @RequestParam UUID userId,
            Model model) {
        List<Booking> bookings = bookingService.findByUser(userId);

        // Map para bookingId -> clubName
        Map<UUID, String> clubNames = new HashMap<>();
        for (Booking booking : bookings) {
            Optional<Table> tableOpt = tableService.findById(booking.getTableId());
            if (tableOpt.isPresent()) {
                Table table = tableOpt.get();
                UUID clubId = table.getClubId();
                Optional<Club> clubOpt = clubService.getClubById(clubId);
                clubOpt.ifPresent(club -> clubNames.put(booking.getBookingId(), club.getClubName()));
            }
        }

        User user = securityService.getVerifiedLoggedUser();
        model.addAttribute("user", user);
        model.addAttribute("bookings", bookings);
        model.addAttribute("clubNames", clubNames);
        return "userBookings";
    }

    @GetMapping("/booking")
    public String bookingUserHandler(@RequestParam UUID bookingId, Model model) {
        Optional<Booking> bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            model.addAttribute("error", "Reserva no encontrada");
            return "error/error";
        }
        Booking booking = bookingOpt.get();

        var tableOpt = tableService.findById(booking.getTableId());
        Table table = tableOpt.orElse(null);

        String clubName = "";
        if (table != null) {
            clubName = clubService.getClubById(table.getClubId())
                    .map(Club::getClubName).orElse("");
        }

        User u = userService.findById(booking.getUser().getId());

        model.addAttribute("user", u);

        var rOpt = receiptService.findByBooking(bookingId);
        model.addAttribute("hasReceipt", rOpt.isPresent());
        rOpt.ifPresent(r -> model.addAttribute("receipt", r));

        model.addAttribute("booking", booking);
        model.addAttribute("table", table);
        model.addAttribute("clubName", clubName);
        return "bookingUserHandler";
    }

    @PostMapping(path = "/bookings/{bookingId}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadReceipt(@PathVariable UUID bookingId,
                                @RequestParam("file") MultipartFile file,
                                RedirectAttributes ra,
                                @RequestHeader(value = "Referer", required = false) String referer) throws IOException {

        var booking = bookingService.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "Select an image to upload");
            return referer != null ? "redirect:" + referer : "redirect:/bookings/" + bookingId;
        }

        if (receiptService.findByBooking(bookingId).isPresent()) {
            ra.addFlashAttribute("info", "A receipt is already uploaded. Delete it to upload a new one.");
            return referer != null ? "redirect:" + referer : "redirect:/bookings/" + bookingId;
        }

        try {
            receiptService.upload(
                    bookingId,
                    securityService.getVerifiedLoggedUser().getId(),
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt",
                    file.getContentType(),
                    file.getBytes()
            );
            ra.addFlashAttribute("success", "Receipt uploaded");
        } catch (DuplicateKeyException e) {
            ra.addFlashAttribute("info", "A receipt is already uploaded. Delete it to upload a new one.");
        }
        return referer != null ? "redirect:" + referer : "redirect:/bookings/" + bookingId;
    }


    @GetMapping("/admin/booking")
    public String bookingAdminHandler(@RequestParam UUID bookingId, Model model, @ModelAttribute("error") String error, @ModelAttribute("success") String success) {
        Optional<Booking> bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            model.addAttribute("error", "Reserva no encontrada");
            return "error/error";
        }
        Booking booking = bookingOpt.get();

        Optional<Table> tableOpt = tableService.findById(booking.getTableId());
        Table table = tableOpt.orElse(null);

        String clubName = "";
        if (table != null) {
            Optional<Club> clubOpt = clubService.getClubById(table.getClubId());
            clubName = clubOpt.map(Club::getClubName).orElse("");
        }

        var rOpt = receiptService.findByBooking(bookingId);
        model.addAttribute("hasReceipt", rOpt.isPresent());
        rOpt.ifPresent(r -> model.addAttribute("receipt", r));

        User u = userService.findById(booking.getUser().getId());
        model.addAttribute("user", u);

        model.addAttribute("booking", booking);
        model.addAttribute("table", table);
        model.addAttribute("clubName", clubName);

        if (error != null && !error.isEmpty()) {
            model.addAttribute("errorKey", error);
        }
        if (success != null && !success.isEmpty()) {
            model.addAttribute("success", success);
        }
        return "bookingAdminHandler";
    }

    @PostMapping("/admin/bookings/{bookingId}/receipt/delete")
    public String deleteReceipt(@PathVariable UUID bookingId, RedirectAttributes ra) {
        boolean deleted = receiptService.deleteByBooking(bookingId);
        ra.addFlashAttribute(deleted ? "success" : "info",
                deleted ? "receipt.deleted" : "receipt.notfound");
        return "redirect:/admin/booking?bookingId=" + bookingId;
    }

    @GetMapping("/bookings/{bookingId}")
    public String show(@PathVariable UUID bookingId, Model model) {
        final Optional<Booking> b = bookingService.findById(bookingId);
        if (b.isEmpty()) throw new BookingNotFoundException("La reserva no existe");
        model.addAttribute("booking", b.get());

        var rOpt = receiptService.findByBooking(bookingId);
        model.addAttribute("hasReceipt", rOpt.isPresent());
        rOpt.ifPresent(r -> model.addAttribute("receipt", r));

        return "bookings/show";
    }

    @GetMapping("/bookings/{bookingId}/receipt")
    @ResponseBody
    public ResponseEntity<byte[]> getReceipt(@PathVariable UUID bookingId) {
        var rOpt = receiptService.findByBooking(bookingId);
        if (rOpt.isEmpty()) return ResponseEntity.notFound().build();

        var rMeta = rOpt.get();
        var r = receiptService.getWithId(rMeta.getReceiptId()).orElse(rMeta);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + r.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(r.getContentType()))
                .body(r.getData());
    }

    @PostMapping("/bookings/{bookingId}/receipt/delete")
    public String deleteReceiptForUser(@PathVariable UUID bookingId, RedirectAttributes ra) {
        var booking = bookingService.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        var current = securityService.getVerifiedLoggedUser();
        if (booking.getUser() == null || !booking.getUser().getId().equals(current.getId())) {
            throw new BadRequestException("Not allowed to delete this receipt");
        }

        boolean deleted = receiptService.deleteByBooking(bookingId);
        ra.addFlashAttribute(deleted ? "success" : "info",
                deleted ? "receipt.deleted" : "receipt.notfound");
        return "redirect:/booking?bookingId=" + bookingId;
    }



}
