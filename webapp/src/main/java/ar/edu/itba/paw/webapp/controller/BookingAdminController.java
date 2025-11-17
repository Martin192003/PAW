package ar.edu.itba.paw.webapp.controller;


import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.BookingNotFoundException;
import ar.edu.itba.paw.model.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class BookingAdminController {

    private final BookingService bookingService;
    private final ClubService clubService;
    private final TableService tableService;
    private final UserService userService;
    private final ReceiptService receiptService;
    private final PhotoService photoService;

    public BookingAdminController(BookingService bookingService, ClubService clubService, TableService tableService, UserService userService, ReceiptService receiptService, PhotoService photoService) {
        this.bookingService = bookingService;
        this.clubService = clubService;
        this.tableService = tableService;
        this.userService = userService;
        this.receiptService = receiptService;
        this.photoService = photoService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/activity")
    public String userClubs(@RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            Model model, @AuthenticationPrincipal PawUserDetails userDetails) {

        if (page < 1) page = 1;
        if (size < 1) size = 10;
        int offset = (page - 1) * size;


        List<Club> clubs = clubService.getClubsByUserPaginated(userDetails.getId(), size, offset);
        int totalClubs = clubService.countClubsByUser(userDetails.getId());
        int totalPages = clubService.countPagesForUser(userDetails.getId(), size);

        Map<UUID,String> photoUrlByClub = photoService.getCoverUrls(
                clubs.stream().map(Club::getClubId).collect(Collectors.toSet())
        );

        model.addAttribute("user", userDetails.getUser());
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

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/clubs/bookings", produces = "text/html;charset=UTF-8")
    public String bookingsOfMyClubs(@ModelAttribute("bookingSearchForm") BookingSearchForm form,
                                    BindingResult br, Model model, @AuthenticationPrincipal PawUserDetails userDetails) {

        final int size = 10;
        final int page = Math.max(form.getPage(), 1);
        final int offset = (page - 1) * size;


        UUID bookingUuid = bookingService.parseBookingId(form.getBookingId());
        if (form.getBookingId() != null && !form.getBookingId().isBlank() && bookingUuid == null) {
            br.rejectValue("bookingId", "bookingId.invalid", "bookingId.invalid");
        }

        LocalDate bookingDate = bookingService.parseBookingDate(form.getBookingDate());

        List<Booking> result = List.of();
        int totalBookings = 0;

        if (!br.hasErrors()) {
            result = bookingService.searchForAdminPaginated(
                    userDetails.getId(),
                    form.getClubId(),
                    bookingUuid,
                    form.getStatus(),
                    form.getSort(),
                    bookingDate,
                    form.getHasReceipt(),
                    size, offset
            );
            totalBookings = bookingService.countSearchForAdmin(
                    userDetails.getId(),
                    form.getClubId(),
                    bookingUuid,
                    form.getStatus(),
                    bookingDate,
                    form.getHasReceipt()
            );
        }

        int totalPages = (int) Math.ceil((double) totalBookings / size);

        Map<String, String> filters = new LinkedHashMap<>();

        if (form.getClubId() != null) filters.put("clubId", form.getClubId().toString());
        if (form.getBookingId() != null && !form.getBookingId().isBlank()) filters.put("bookingId", form.getBookingId());
        if (form.getStatus() != null) filters.put("status", form.getStatus().name());
        if (form.getSort() != null && !form.getSort().isBlank()) filters.put("sort", form.getSort());


        String queryParams = bookingService.buildQueryParams(filters);

        Map<UUID, Date> bookingStartDates = bookingService.mapBookingStartDates(result);
        model.addAttribute("bookingStartDates", bookingStartDates);

        Map<UUID, Boolean> hasReceiptMap = receiptService.buildHasReceiptMap(result);
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

        model.addAttribute("clubs", clubService.getClubsByUser(userDetails.getId()));

        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("activePage", "clubsBookings");
        model.addAttribute("bookingSearchForm", form);

        return "clubsBookings";
    }


    @GetMapping("/booking")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(@bookingServiceImpl.clubId(#bookingId))")
    public String bookingAdminHandler(@RequestParam UUID bookingId, Model model, @ModelAttribute("error") String error, @ModelAttribute("success") String success) {
        Optional<Booking> bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            model.addAttribute("error", "error.no_reservation");
            return "error/error";
        }
        Booking  booking = bookingOpt.get();

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

        model.addAttribute("bookingStartLocal", bookingService.getBookingDateStart(booking));
        model.addAttribute("bookingEndLocal", bookingService.getBookingDateEnd(booking));

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
