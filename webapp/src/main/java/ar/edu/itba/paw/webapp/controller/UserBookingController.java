package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ar.edu.itba.paw.webapp.form.ReceiptUploadForm;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping
public class UserBookingController {

    private final BookingService bookingService;
    private final TableService tableService;
    private final UserService userService;
    private final ClubService clubService;
    private final ReceiptService receiptService;

    public UserBookingController(BookingService bookingService,
                             TableService tableService,
                             UserService userService, ClubService clubService, ReceiptService receiptService) {
        this.bookingService = bookingService;
        this.tableService = tableService;
        this.userService = userService;
        this.clubService = clubService;
        this.receiptService = receiptService;
    }

    @PostMapping("/clubs/{clubId}/reserve")
    @PreAuthorize("isAuthenticated()")
    public String reserve(@PathVariable UUID clubId,
                          @RequestParam UUID tableId,
                          @RequestParam String date,
                          @RequestParam(value = "fromEvent", defaultValue = "false") String fromEventStr,
                          Principal principal,
                          RedirectAttributes ra) {

        boolean fromEvent = "true".equals(fromEventStr);

        final String username = Optional.ofNullable(principal)
                .map(Principal::getName)
                .orElseThrow(() -> new IllegalArgumentException("error.user.notAuthenticated"));

        try {
            Booking booking = bookingService.reserveTableForClub(clubId, tableId, LocalDate.parse(date), username, fromEvent);
            ra.addFlashAttribute("reservationSent", true);
            return "redirect:/booking?bookingId=" + booking.getBookingId();
        } catch (InvalidDateRangeException | ValidationException e) {
            ra.addFlashAttribute("error", "invalid.date");
            ra.addFlashAttribute("reservationError", true);
        } catch (BadRequestException e) {
            ra.addFlashAttribute("error", "tables.notavailable");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "error.generic");
        }
        return "redirect:/clubs/" + clubId + "?date=" + date + "#dd-tables";
    }


    @PostMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public String cancelUser(@PathVariable UUID bookingId, RedirectAttributes ra, @AuthenticationPrincipal PawUserDetails userDetails) {
        try {
            bookingService.cancelByUser(bookingId, userDetails.getId());
            ra.addFlashAttribute("success", "booking.cancelled");
            ra.addFlashAttribute("bookingAction", "cancelled");
        } catch (UnauthorizedException e) {
            ra.addFlashAttribute("error", "booking.notAllowedToCancel");
        } catch (InvalidStatusTransitionException e) {
            ra.addFlashAttribute("error", "error.booking.cannotCancel");
        } catch (BookingNotFoundException e) {
            ra.addFlashAttribute("error", "booking.notfound");
        }
        return "redirect:/booking?bookingId=" + bookingId;
    }

    @GetMapping("/bookings")
    @PreAuthorize("isAuthenticated()")
    public String listUserBookings(@Valid @ModelAttribute("bookingSearchForm") BookingSearchForm form,
                                   BindingResult br, Model model, @AuthenticationPrincipal PawUserDetails userDetails) {

        final int size = 10;
        int page = Math.max(form.getPage(), 1);
        int offset = (page - 1) * size;

        if (form.getUserId() == null) {
            form.setUserId(userDetails.getId());
        }

        LocalDate bookingDate = null;
        if (form.getBookingDate() != null && !form.getBookingDate().isBlank()) {
            bookingDate = LocalDate.parse(form.getBookingDate());
        }

        UUID bookingUuid = null;
        if (form.getBookingId() != null && !form.getBookingId().isBlank()) {
            try {
                bookingUuid = UUID.fromString(form.getBookingId().trim());
            } catch (IllegalArgumentException e) {
                br.rejectValue("bookingId", "bookingId.invalid", "bookingId.invalid");
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
                    bookingDate,
                    form.getHasReceipt(),
                    size,
                    offset
            );

            totalBookings = bookingService.countSearch(
                    form.getUserId(),
                    form.getClubId(),
                    bookingUuid,
                    form.getStatus(),
                    bookingDate,
                    form.getHasReceipt()
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

        Map<UUID, Date> bookingStartDates = new HashMap<>();
        for (Booking booking : bookings) {
            if (booking.getBookingStart() != null) {
                bookingStartDates.put(
                        booking.getBookingId(),
                        Date.from(booking.getBookingStart().toInstant())
                );
            }
        }
        model.addAttribute("bookingStartDates", bookingStartDates);

        // Datos para la vista
        model.addAttribute("bookings", bookings);
        model.addAttribute("clubNames", clubService.mapClubNames(bookings));
        model.addAttribute("totalResults", totalBookings);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("queryParams", queryParams);
        model.addAttribute("clubs", clubService.getClubsWithBookingsByUser(form.getUserId()));


        // echo de filtros
        model.addAttribute("sort", form.getSort());
        model.addAttribute("status", form.getStatus());
        model.addAttribute("clubId", form.getClubId());
        model.addAttribute("bookingId", form.getBookingId());
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("activePage", "bookings");
        model.addAttribute("bookingSearchForm", form);

        return "userBookings";
    }


    @PostMapping(path = "/bookings/{bookingId}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public String uploadReceipt(@PathVariable UUID bookingId,
                                @Valid @ModelAttribute("receiptForm") ReceiptUploadForm form,
                                BindingResult errors,
                                RedirectAttributes ra,
                                @RequestHeader(value = "Referer", required = false) String referer,
                                Model model, @AuthenticationPrincipal PawUserDetails userDetails) {
        if (form.getFile() == null || form.getFile().isEmpty()) {
            errors.rejectValue("file", "receipt.file.required");
        }
        if (errors.hasErrors()) {
            var booking = bookingService.findById(bookingId)
                    .orElseThrow(() -> new BookingNotFoundException("error.booking"));

            model.addAttribute("booking", booking);
            model.addAttribute("user", userService.findById(booking.getUser().getId()));

            ClubTable table = tableService.findById(booking.getTableId()).orElse(null);
            model.addAttribute("table", table);

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
            ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withLocale(new Locale("es","AR"));
            model.addAttribute("clubName", clubName);
            model.addAttribute("clubCbu", clubCbu);
            var rOpt = receiptService.findByBooking(bookingId);
            model.addAttribute("hasReceipt", rOpt.isPresent());
            rOpt.ifPresent(r -> model.addAttribute("receipt", r));
            model.addAttribute("activePage", "bookings");
            model.addAttribute("bookingStartLocal", booking.getBookingStart().atZoneSameInstant(zone).format(fmt));
            model.addAttribute("bookingEndLocal", booking.getBookingEnd().atZoneSameInstant(zone).format(fmt));


            return "bookingUserHandler";
        }
        try {
            try {
                receiptService.deleteByBookingForUser(bookingId, userDetails.getId());
            } catch (Exception e) {
                // Ignorar errores al eliminar recibo previo
            }
            String filename = Optional.ofNullable(form.getFile().getOriginalFilename()).orElse("receipt");
            receiptService.uploadForBooking(
                    bookingId, userDetails.getId(), filename, form.getFile().getContentType(), form.getFile().getBytes());
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
        if (referer != null && referer.contains("/booking")) {
            return "redirect:" + referer;
        }
        return "redirect:/booking?bookingId=" + bookingId;
    }

    @GetMapping("/booking")
    @PreAuthorize("isAuthenticated()")
    public String bookingUserHandler(@RequestParam UUID bookingId,
                                     Model model,
                                     @ModelAttribute("error") String error,
                                     @ModelAttribute("errorKey") String errorKey,
                                     @ModelAttribute("success") String success,
                                     @ModelAttribute("info") String info, @AuthenticationPrincipal PawUserDetails userDetails) {

        final Booking booking;
        try {
            booking = bookingService.getForUser(bookingId, userDetails.getId());
        } catch (BookingNotFoundException e) {
            model.addAttribute("errorKey", "booking.notfound");
            return "error/error";
        } catch (UnauthorizedException e) {
            model.addAttribute("errorKey", "booking.notAllowedToView");
            return "error/error";
        }

        Optional<User> u = userService.findById(booking.getUser().getId());
        model.addAttribute("user", u.orElse(null));

        ClubTable table = tableService.findById(booking.getTableId()).orElse(null);
        model.addAttribute("table", table);

        String clubName = "";
        String clubCbu = null;
        if (table != null) {
            var clubOpt = clubService.getClubById(table.getClubId());
            clubName = clubOpt.map(Club::getClubName).orElse("");
            clubCbu = clubOpt.map(Club::getCbu).orElse(null);
        }
        model.addAttribute("clubName", clubName);
        model.addAttribute("clubCbu", clubCbu);
        if(!model.containsAttribute("receiptForm")) {
            model.addAttribute("receiptForm", new ReceiptUploadForm());
        }

        model.addAttribute("booking", booking);

        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");

        ZonedDateTime startLocal = booking.getBookingStart().atZoneSameInstant(zone);
        String bookingStartLocal = formatDateEsEn(startLocal);

        ZonedDateTime endLocal = booking.getBookingEnd().atZoneSameInstant(zone);
        String bookingEndLocal = formatDateEsEn(endLocal);

        model.addAttribute("bookingStartLocal", bookingStartLocal);
        model.addAttribute("bookingEndLocal", bookingEndLocal);

        var rOpt = receiptService.findByBooking(bookingId);
        model.addAttribute("hasReceipt", rOpt.isPresent());
        rOpt.ifPresent(r -> model.addAttribute("receipt", r));

        if (errorKey != null && !errorKey.isEmpty()) model.addAttribute("errorKey", errorKey);
        if (error != null && !error.isEmpty())       model.addAttribute("error", error);
        if (success != null && !success.isEmpty())   model.addAttribute("success", success);
        if (info != null && !info.isEmpty())         model.addAttribute("info", info);

        model.addAttribute("activePage", "bookings");

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
    @PreAuthorize("isAuthenticated()")
    public String deleteReceiptForUser(@PathVariable UUID bookingId, RedirectAttributes ra, @AuthenticationPrincipal PawUserDetails userDetails) {
            boolean deleted;
            try {
                    deleted = receiptService.deleteByBookingForUser(bookingId, userDetails.getId());
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

    private static String formatDateEsEn(ZonedDateTime zdt) {
        var date = zdt.toLocalDate();
        if (date == null) return "";
        Locale locale = LocaleContextHolder.getLocale();
        boolean isEs = "es".equalsIgnoreCase(locale.getLanguage());

        Month m = date.getMonth();
        String monthName = m.getDisplayName(TextStyle.FULL, locale);
        if (isEs) {
            monthName = capitalize(monthName, locale); // Java devuelve "noviembre"; se capitaliza -> "Noviembre"
        }

        String connector = isEs ? " de " : " of ";
        return date.getDayOfMonth() + connector + monthName + ", " + date.getYear();
    }
    private static String capitalize(String s, Locale locale) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(locale) + s.substring(1);
    }
}
