package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.BookingForm;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping
public class BookingController {

    private final BookingService bookingService;
    private final TableService tableService;
    private final UserService userService;
    private final ClubService clubService;
    private final SecurityService securityService;
    private final ReceiptService receiptService;

    public BookingController(BookingService bookingService,
                             TableService tableService,
                             UserService userService, ClubService clubService, EmailService emailService, SecurityService securityService, ReceiptService receiptService) {
        this.bookingService = bookingService;
        this.tableService = tableService;
        this.userService = userService;
        this.clubService = clubService;
        this.securityService = securityService;
        this.receiptService = receiptService;
    }

    @PostMapping("/clubs/{clubId}/reserve")
    public String reserve(@PathVariable UUID clubId,
                          @RequestParam UUID tableId,
                          @RequestParam String date,
                          Principal principal,
                          RedirectAttributes ra) {

        var username = Optional.ofNullable(principal).map(Principal::getName)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no está autenticado."));
        bookingService.reserveTableForClub(clubId, tableId, LocalDate.parse(date), username);

        ra.addFlashAttribute("reservationSent", true);
        ra.addFlashAttribute("success", "¡Reserva creada para " + date + " (23:59–06:00)!");
        return "redirect:/clubs/" + clubId + "?date=" + date;
    }

    /** Confirmar reserva → RESERVADA */
    @PostMapping("/admin/bookings/{bookingId}/confirm")
    public String confirm(@PathVariable UUID bookingId, RedirectAttributes ra) {
        try {
            bookingService.confirm(bookingId);
            ra.addFlashAttribute("success", "booking.confirmed");
        } catch (InvalidStatusTransitionException e) {
            ra.addFlashAttribute("error", "error.booking.cannotConfirm");
        } catch (BookingNotFoundException e) {
            ra.addFlashAttribute("error", "booking.notfound");
        }
        return "redirect:/admin/booking?bookingId=" + bookingId;
    }

    @PostMapping("/admin/bookings/{bookingId}/cancel")
    public String cancel(@PathVariable UUID bookingId, RedirectAttributes ra) {
        try {
            bookingService.cancel(bookingId);
            ra.addFlashAttribute("success", "booking.cancelled");
        } catch (InvalidStatusTransitionException e) {
            ra.addFlashAttribute("error", "error.booking.cannotCancel");
        } catch (BookingNotFoundException e) {
            ra.addFlashAttribute("error", "booking.notfound");
        }
        return "redirect:/admin/booking?bookingId=" + bookingId;
    }


    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancelUser(@PathVariable UUID bookingId, RedirectAttributes ra) {
        var current = securityService.getVerifiedLoggedUser();
        try {
            bookingService.cancelByUser(bookingId, current.getId());
            ra.addFlashAttribute("success", "booking.cancelled");
        } catch (UnauthorizedException e) {
            ra.addFlashAttribute("error", "booking.notAllowedToCancel");
        } catch (InvalidStatusTransitionException e) {
            ra.addFlashAttribute("error", "error.booking.cannotCancel");
        } catch (BookingNotFoundException e) {
            ra.addFlashAttribute("error", "booking.notfound");
        }
        return "redirect:/booking?bookingId=" + bookingId;
    }

    /*
    ---------- VISTAS DE ADMIN ----------
     */

    @GetMapping("/admin/clubs/bookings")
    public String bookingsOfMyClubs(@Valid @ModelAttribute("bookingSearchForm") BookingSearchForm form,
                                    BindingResult br,
                                    Model model) {

        // tamaño y página
        final int size = 10;
        int page = Math.max(form.getPage(), 1);
        int offset = (page - 1) * size;

        // Parseo seguro de bookingId (String -> UUID)
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
            result = bookingService.searchPaginated(
                    null, form.getClubId(), bookingUuid, form.getStatus(),
                    form.getSort(), size, offset
            );
            totalBookings = bookingService.countSearch(null, form.getClubId(), bookingUuid, form.getStatus());
        }

        int totalPages = (int) Math.ceil((double) totalBookings / size);

        Map<String, String> filters = new LinkedHashMap<>();
        if (form.getUserId() != null) filters.put("userId", form.getUserId().toString());
        if (form.getClubId() != null) filters.put("clubId", form.getClubId().toString());
        if (form.getBookingId() != null && !form.getBookingId().isBlank()) filters.put("bookingId", form.getBookingId());
        if (form.getStatus() != null) filters.put("status", form.getStatus().name());
        if (form.getSort() != null && !form.getSort().isBlank()) filters.put("sort", form.getSort());

        String queryParams = filters.entrySet().stream()
                .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));

        // Modelo
        model.addAttribute("bookings", result);
        model.addAttribute("clubNames", clubService.mapClubNames(result)); // o clubService.mapClubNames(...)
        model.addAttribute("totalResults", totalBookings);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("queryParams", queryParams);

        // echo de filtros (no depende de que haya error o no)
        model.addAttribute("sort", form.getSort());
        model.addAttribute("status", form.getStatus());
        model.addAttribute("clubId", form.getClubId());
        model.addAttribute("bookingId", form.getBookingId());

        // combos
        model.addAttribute("clubs", clubService.getClubsByUser(form.getUserId()));

        // usuario y nav
        model.addAttribute("user", securityService.getVerifiedLoggedUser());
        model.addAttribute("activePage", "clubsBookings");

        // importante: dejar el form en el modelo para <form:form>
        model.addAttribute("bookingSearchForm", form);

        return "clubsBookings";
    }

    /*
    ---------- VISTAS DE USUARIO ----------
     */

    @GetMapping("/bookings")
    public String listUserBookings(@Valid @ModelAttribute("bookingSearchForm") BookingSearchForm form,
                                   BindingResult br,
                                   Model model) {

        final int size = 10;
        int page = Math.max(form.getPage(), 1);
        int offset = (page - 1) * size;

        if (form.getUserId() == null) {
            var current = securityService.getVerifiedLoggedUser();
            form.setUserId(current.getId());
        }

        UUID bookingUuid = null;
        if (form.getBookingId() != null && !form.getBookingId().isBlank()) {
            try {
                bookingUuid = UUID.fromString(form.getBookingId().trim());
            } catch (IllegalArgumentException e) {
                br.rejectValue("bookingId", "bookingId.invalid", "El ID de reserva ingresado no es válido.");
            }
        }

        List<Booking> bookings = List.of();
        int totalBookings = 0;

        if (!br.hasErrors()) {
            bookings = bookingService.searchPaginated(
                    form.getUserId(),
                    form.getClubId(),
                    bookingUuid,
                    form.getStatus(),
                    form.getSort(),
                    size,
                    offset
            );

            totalBookings = bookingService.countSearch(
                    form.getUserId(),
                    form.getClubId(),
                    bookingUuid,
                    form.getStatus()
            );
        }

        int totalPages = (int) Math.ceil((double) totalBookings / size);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("userId", form.getUserId().toString()); // obligatorio
        if (form.getClubId() != null) filters.put("clubId", form.getClubId().toString());
        if (form.getBookingId() != null && !form.getBookingId().isBlank()) filters.put("bookingId", form.getBookingId());
        if (form.getStatus() != null) filters.put("status", form.getStatus().name());
        if (form.getSort() != null && !form.getSort().isBlank()) filters.put("sort", form.getSort());

        String queryParams = filters.entrySet().stream()
                .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        // Datos para la vista
        model.addAttribute("bookings", bookings);
        model.addAttribute("clubNames", clubService.mapClubNames(bookings));
        model.addAttribute("totalResults", totalBookings);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("queryParams", queryParams);

        // echo de filtros
        model.addAttribute("sort", form.getSort());
        model.addAttribute("status", form.getStatus());
        model.addAttribute("clubId", form.getClubId());
        model.addAttribute("bookingId", form.getBookingId());

        model.addAttribute("user", securityService.getVerifiedLoggedUser());
        model.addAttribute("activePage", "bookings");
        model.addAttribute("bookingSearchForm", form);

        return "userBookings";
    }


    @PostMapping(path = "/bookings/{bookingId}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadReceipt(@PathVariable UUID bookingId,
                                @RequestParam("file") MultipartFile file,
                                RedirectAttributes ra,
                                @RequestHeader(value = "Referer", required = false) String referer) {

            var current = securityService.getVerifiedLoggedUser();
        try {
            String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("receipt");
            receiptService.uploadForBooking(
                    bookingId, current.getId(), filename, file.getContentType(), file.getBytes());
            ra.addFlashAttribute("success", "receipt.uploaded");
        } catch (UnauthorizedException e) {
            ra.addFlashAttribute("error", "receipt.upload.notAllowed");
        } catch (InvalidReceiptException e) {
            ra.addFlashAttribute("error", "receipt.select.file");
        } catch (DuplicateKeyException e) {
            ra.addFlashAttribute("info", "receipt.already.exists");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "receipt.upload.ioError");
        } catch (BookingNotFoundException e) {
            ra.addFlashAttribute("error", "booking.notfound");
        }
        return safeRedirect(referer, bookingId);
    }


    private String safeRedirect(String referer, UUID bookingId) {
        if (referer != null && !referer.isEmpty() && referer.contains("/booking")) {
            return "redirect:" + referer;
        }
        return "redirect:/booking?bookingId=" + bookingId;
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

    @GetMapping("/booking")
    public String bookingUserHandler(@RequestParam UUID bookingId,
                                     Model model,
                                     @ModelAttribute("error") String error,
                                     @ModelAttribute("errorKey") String errorKey,
                                     @ModelAttribute("success") String success,
                                     @ModelAttribute("info") String info) {

        var current = securityService.getVerifiedLoggedUser();
        final Booking booking;
        try {
            booking = bookingService.getForUser(bookingId, current.getId());
        } catch (BookingNotFoundException e) {
            model.addAttribute("errorKey", "booking.notfound");
            return "error/error";
        } catch (UnauthorizedException e) {
            model.addAttribute("errorKey", "booking.notAllowedToView");
            return "error/error";
        }

        User u = userService.findById(booking.getUser().getId());
        model.addAttribute("user", u);

        Table table = tableService.findById(booking.getTableId()).orElse(null);
        model.addAttribute("table", table);

        String clubName = "";
        if (table != null) {
            clubName = clubService.getClubById(table.getClubId())
                    .map(Club::getClubName)
                    .orElse("");
        }
        model.addAttribute("clubName", clubName);

        model.addAttribute("booking", booking);

        var rOpt = receiptService.findByBooking(bookingId);
        model.addAttribute("hasReceipt", rOpt.isPresent());
        rOpt.ifPresent(r -> model.addAttribute("receipt", r));

        if (errorKey != null && !errorKey.isEmpty()) model.addAttribute("errorKey", errorKey);
        if (error != null && !error.isEmpty())       model.addAttribute("error", error);
        if (success != null && !success.isEmpty())   model.addAttribute("success", success);
        if (info != null && !info.isEmpty())         model.addAttribute("info", info);

        model.addAttribute("activePage", "bookings"); // para resaltar sección en sidebar/topbar

        return "bookingUserHandler";
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
            var current = securityService.getVerifiedLoggedUser();
            boolean deleted;
            try {
                    deleted = receiptService.deleteByBookingForUser(bookingId, current.getId());
                } catch (UnauthorizedException e) {
                    ra.addFlashAttribute("error", "receipt.delete.notAllowed");
                    return "redirect:/booking?bookingId=" + bookingId;
                } catch (BookingNotFoundException e) {
                    ra.addFlashAttribute("error", "booking.notfound");
                    return "redirect:/booking?bookingId=" + bookingId;
                }
        ra.addFlashAttribute(deleted ? "success" : "info",
                deleted ? "receipt.deleted" : "receipt.notfound");
        return "redirect:/booking?bookingId=" + bookingId;
    }

}
