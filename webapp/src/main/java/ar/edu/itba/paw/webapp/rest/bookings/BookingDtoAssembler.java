package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.dto.BookingDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.service.ReceiptService;
import ar.edu.itba.paw.webapp.rest.LinkBuilder;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class BookingDtoAssembler {

    private final ReceiptService receiptService;

    public BookingDtoAssembler(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    public BookingDto toDto(UriInfo uriInfo, UUID clubId, Booking booking) {
        return toDto(uriInfo, clubId, booking, false, false);
    }

    public BookingDto toDto(UriInfo uriInfo, UUID clubId, Booking booking, boolean canManage, boolean isOwner) {
        return toDto(uriInfo, clubId, booking, canManage, isOwner, null, false);
    }

    public BookingDto toDetailDto(UriInfo uriInfo,
                                  UUID clubId,
                                  Booking booking,
                                  boolean canManage,
                                  boolean isOwner,
                                  String clubName) {
        final boolean hasReceipt = receiptService.buildHasReceiptMap(List.of(booking))
                .getOrDefault(booking.getBookingId(), false);
        final BookingDto dto = toDto(uriInfo, clubId, booking, canManage, isOwner, hasReceipt, true);
        dto.setClubName(clubName);
        return dto;
    }

    public List<BookingDto> toDtos(UriInfo uriInfo,
                                   List<Booking> bookings,
                                   boolean canManage,
                                   boolean isOwner,
                                   Map<UUID, String> clubNamesByBooking) {
        final Map<UUID, Boolean> hasReceiptByBooking = receiptService.buildHasReceiptMap(bookings);
        return bookings.stream()
                .map(booking -> {
                    final UUID clubId = resolveClubId(booking);
                    final BookingDto dto = toDto(uriInfo, clubId, booking, canManage, isOwner,
                            hasReceiptByBooking.getOrDefault(booking.getBookingId(), false),
                            true);
                    if (clubNamesByBooking != null) {
                        dto.setClubName(clubNamesByBooking.get(booking.getBookingId()));
                    }
                    return dto;
                })
                .toList();
    }

    private BookingDto toDto(UriInfo uriInfo,
                             UUID clubId,
                             Booking booking,
                             boolean canManage,
                             boolean isOwner,
                             Boolean hasReceipt) {
        return toDto(uriInfo, clubId, booking, canManage, isOwner, hasReceipt, false);
    }

    private BookingDto toDto(UriInfo uriInfo,
                             UUID clubId,
                             Booking booking,
                             boolean canManage,
                             boolean isOwner,
                             Boolean hasReceipt,
                             boolean includeInlineDetails) {
        final UUID tableId = booking.getTableId() != null
                ? booking.getTableId()
                : (booking.getTable() != null ? booking.getTable().getTableId() : null);
        final UUID resolvedClubId = clubId != null ? clubId : resolveClubId(booking);

        BookingDto dto = new BookingDto();
        dto.setId(booking.getBookingId());
        dto.setClubId(resolvedClubId);
        dto.setUserId(booking.getUser() != null ? booking.getUser().getId() : null);
        dto.setTableId(tableId);
        dto.setBookingStart(booking.getBookingStart());
        dto.setBookingEnd(booking.getBookingEnd());
        dto.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setHasReceipt(hasReceipt != null
                ? hasReceipt
                : receiptService.findByBooking(booking.getBookingId()).isPresent());
        if (includeInlineDetails) {
            fillInlineDetails(dto, booking);
        }

        List<LinkDto> links = new ArrayList<>();
        links.add(LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                .path("bookings/{bookingId}")
                .resolveTemplate("bookingId", booking.getBookingId())
                .build(), "GET"));
        links.add(LinkBuilder.link("club", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}")
                .resolveTemplate("clubId", resolvedClubId)
                .build(), "GET"));
        if (dto.getUserId() != null) {
            links.add(LinkBuilder.link("user", uriInfo.getBaseUriBuilder()
                    .path("users/{userId}")
                    .resolveTemplate("userId", dto.getUserId())
                    .build(), "GET"));
        }
        if (tableId != null) {
            links.add(LinkBuilder.link("table", uriInfo.getBaseUriBuilder()
                    .path("clubs/{clubId}/tables/{tableId}")
                    .resolveTemplate("clubId", resolvedClubId)
                    .resolveTemplate("tableId", tableId)
                    .build(), "GET"));
        }
        links.add(LinkBuilder.link("receipt", uriInfo.getBaseUriBuilder()
                .path(BookingRestController.class)
                .path(BookingRestController.class, "getReceipt")
                .resolveTemplate("clubId", resolvedClubId)
                .resolveTemplate("bookingId", booking.getBookingId())
                .build(), "GET"));

        final boolean cancellable = booking.getStatus() == Status.PENDIENTE
                || booking.getStatus() == Status.CONFIRMADA;
        if (isOwner && cancellable) {
            links.add(LinkBuilder.link("cancel", uriInfo.getBaseUriBuilder()
                    .path("bookings/{bookingId}")
                    .resolveTemplate("bookingId", booking.getBookingId())
                    .build(), "DELETE"));
        }
        if (canManage && booking.getStatus() == Status.PENDIENTE && dto.isHasReceipt()) {
            links.add(LinkBuilder.link("confirm", uriInfo.getBaseUriBuilder()
                    .path("bookings/{bookingId}")
                    .resolveTemplate("bookingId", booking.getBookingId())
                    .build(), "PUT"));
        }
        if (canManage && cancellable) {
            links.add(LinkBuilder.link("managed-cancel", uriInfo.getBaseUriBuilder()
                    .path("bookings/{bookingId}")
                    .resolveTemplate("bookingId", booking.getBookingId())
                    .build(), "PUT"));
        }
        dto.setLinks(links);

        return dto;
    }

    private UUID resolveClubId(Booking booking) {
        return booking.getTable() != null ? booking.getTable().getClubId() : null;
    }

    private void fillInlineDetails(BookingDto dto, Booking booking) {
        if (booking.getUser() != null) {
            dto.setUserUsername(booking.getUser().getUsername());
            dto.setUserName(booking.getUser().getName());
            dto.setUserSurname(booking.getUser().getSurname());
        }
        if (booking.getTable() != null) {
            dto.setTableNumber(booking.getTable().getTableNumber());
            dto.setTablePrice(booking.getTable().getPrice());
            dto.setTableDiscount(booking.getTable().getDiscount());
        }
    }
}
