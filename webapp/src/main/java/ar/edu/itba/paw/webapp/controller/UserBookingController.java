package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping
public class UserBookingController {

    private final BookingService bookingService;
    private final ClubService clubService;
    private final ReceiptService receiptService;

    public UserBookingController(BookingService bookingService, ClubService clubService, ReceiptService receiptService) {
        this.bookingService = bookingService;
        this.clubService = clubService;
        this.receiptService = receiptService;
    }

    @PostMapping("/clubs/{clubId}/reserve")
    @PreAuthorize("isAuthenticated()")
    public String reserve(@PathVariable UUID clubId,
                          @RequestParam UUID tableId,
                          @RequestParam String date,
                          Principal principal,
                          RedirectAttributes ra) {

        final String username = Optional.ofNullable(principal)
                .map(Principal::getName)
                .orElseThrow(() -> new IllegalArgumentException("error.user.notAuthenticated"));

        try {
            Booking booking = bookingService.createForClub(clubId, tableId, LocalDate.parse(date), username);
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

        List<Booking> bookings = List.of();
        int totalBookings = 0;

        if (!br.hasErrors()) {
            bookings = bookingService.searchPaginated(
                    form.getUserId(),
                    form.getClubId(),
                    form.getBookingId(),
                    form.getStatus(),
                    form.getSort(),
                    form.getBookingDate(),
                    form.getHasReceipt(),
                    size,
                    offset
            );

            totalBookings = bookingService.countSearch(
                    form.getUserId(),
                    form.getClubId(),
                    form.getBookingId(),
                    form.getStatus(),
                    form.getBookingDate(),
                    form.getHasReceipt()
            );
        }
        int totalPages = bookingService.getTotalPages(form.getUserId(), form.getClubId(), form.getBookingId(), form.getStatus(), form.getBookingDate(), form.getHasReceipt(), size)  ;

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("userId", form.getUserId().toString());
        if (form.getClubId() != null) filters.put("clubId", form.getClubId().toString());
        if (form.getBookingId() != null && !form.getBookingId().isBlank()) filters.put("bookingId", form.getBookingId());
        if (form.getStatus() != null) filters.put("status", form.getStatus().name());
        if (form.getSort() != null && !form.getSort().isBlank()) filters.put("sort", form.getSort());

        String queryParams = filters.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
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
                                Model model,
                                @AuthenticationPrincipal PawUserDetails userDetails) {

        if (form.getFile() == null || form.getFile().isEmpty()) {
            errors.rejectValue("file", "receipt.file.required");
        }

        if (errors.hasErrors()) {
            final Booking booking = bookingService.getForUser(bookingId, userDetails.getId());

            model.addAttribute("booking", booking);

            model.addAttribute("user", bookingService.getBookingUser(bookingId, userDetails.getId()));

            ClubTable table = bookingService.getBookingTable(bookingId, userDetails.getId());
            model.addAttribute("table", table);

            Club club = bookingService.getBookingClub(bookingId, userDetails.getId());
            model.addAttribute("clubName", club != null ? club.getClubName() : "");
            model.addAttribute("clubCbu",  club != null ? club.getCbu() : null);

            model.addAttribute("bookingStartLocal", bookingService.getDateStart(bookingId, userDetails.getId()));
            model.addAttribute("bookingEndLocal",   bookingService.getDateEnd(bookingId, userDetails.getId()));

            var rOpt = receiptService.getReceiptForBooking(bookingId);
            model.addAttribute("hasReceipt", rOpt.isPresent());
            rOpt.ifPresent(r -> model.addAttribute("receipt", r));

            model.addAttribute("activePage", "bookings");

            return "bookingUserHandler";
        }

        try {
            try {
                receiptService.deleteByBookingForUser(bookingId, userDetails.getId());
            } catch (Exception e) {
                // Ignorar errores al eliminar recibo previo
            }

            String filename = Optional.ofNullable(form.getFile().getOriginalFilename())
                    .orElse("receipt");

            receiptService.uploadForBooking(
                    bookingId,
                    userDetails.getId(),
                    filename,
                    form.getFile().getContentType(),
                    form.getFile().getBytes()
            );

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

    @GetMapping("/booking")
    @PreAuthorize("isAuthenticated()")
    public String bookingUserHandler(@RequestParam UUID bookingId,
                                     Model model,
                                     @ModelAttribute("error") String error,
                                     @ModelAttribute("errorKey") String errorKey,
                                     @ModelAttribute("success") String success,
                                     @ModelAttribute("info") String info,
                                     @AuthenticationPrincipal PawUserDetails userDetails) {

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


        model.addAttribute("booking", booking);

        User bookingUser = bookingService.getBookingUser(bookingId, userDetails.getId());
        model.addAttribute("user", bookingUser);

        ClubTable table = bookingService.getBookingTable(bookingId, userDetails.getId());
        model.addAttribute("table", table);

        Club club = bookingService.getBookingClub(bookingId, userDetails.getId());
        model.addAttribute("clubName", club != null ? club.getClubName() : "");
        model.addAttribute("clubCbu",  club != null ? club.getCbu() : null);

        model.addAttribute("bookingStartLocal", bookingService.getDateStart(bookingId, userDetails.getId()));
        model.addAttribute("bookingEndLocal",   bookingService.getDateEnd(bookingId, userDetails.getId()));

        var receiptOpt = receiptService.getReceiptForBooking(bookingId);
        model.addAttribute("hasReceipt", receiptOpt.isPresent());
        receiptOpt.ifPresent(r -> model.addAttribute("receipt", r));



        if (!model.containsAttribute("receiptForm")) {
            model.addAttribute("receiptForm", new ReceiptUploadForm());
        }
        if (errorKey != null && !errorKey.isEmpty()) {
            model.addAttribute("errorKey", errorKey);
        }
        if (error != null && !error.isEmpty()) {
            model.addAttribute("error", error);
        }
        if (success != null && !success.isEmpty()) {
            model.addAttribute("success", success);
        }
        if (info != null && !info.isEmpty()) {
            model.addAttribute("info", info);
        }

        model.addAttribute("activePage", "bookings");

        return "bookingUserHandler";
    }

    @GetMapping("/bookings/{bookingId}/receipt")
    @ResponseBody
    public ResponseEntity<byte[]> getReceipt(@PathVariable UUID bookingId) {

        return receiptService.getReceiptForBooking(bookingId)
                .map(r -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + r.getFilename() + "\"")
                        .contentType(MediaType.parseMediaType(r.getContentType()))
                        .body(r.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
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


    private String safeRedirect(String referer, UUID bookingId) {
        if (referer != null && referer.contains("/booking")) {
            return "redirect:" + referer;
        }
        return "redirect:/booking?bookingId=" + bookingId;
    }


}
