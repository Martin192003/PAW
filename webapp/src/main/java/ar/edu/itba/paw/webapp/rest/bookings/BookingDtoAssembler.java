package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.dto.BookingDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.webapp.rest.LinkBuilder;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class BookingDtoAssembler {

    public BookingDto toDto(UriInfo uriInfo, UUID clubId, Booking booking) {
        final UUID tableId = booking.getTableId() != null
                ? booking.getTableId()
                : (booking.getTable() != null ? booking.getTable().getTableId() : null);

        BookingDto dto = new BookingDto();
        dto.setId(booking.getBookingId());
        dto.setClubId(clubId);
        dto.setUserId(booking.getUser() != null ? booking.getUser().getId() : null);
        dto.setTableId(tableId);
        dto.setBookingStart(booking.getBookingStart());
        dto.setBookingEnd(booking.getBookingEnd());
        dto.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setTotalAmount(booking.getTotalAmount());

        List<LinkDto> links = new ArrayList<>();
        links.add(LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                .path(BookingRestController.class)
                .path(BookingRestController.class, "getBooking")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("bookingId", booking.getBookingId())
                .build(), "GET"));
        links.add(LinkBuilder.link("club", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}")
                .resolveTemplate("clubId", clubId)
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
                    .resolveTemplate("clubId", clubId)
                    .resolveTemplate("tableId", tableId)
                    .build(), "GET"));
        }
        links.add(LinkBuilder.link("receipt", uriInfo.getBaseUriBuilder()
                .path(BookingRestController.class)
                .path(BookingRestController.class, "getReceipt")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("bookingId", booking.getBookingId())
                .build(), "GET"));
        links.add(LinkBuilder.link("cancel", uriInfo.getBaseUriBuilder()
                .path(BookingRestController.class)
                .path(BookingRestController.class, "cancelBooking")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("bookingId", booking.getBookingId())
                .build(), "DELETE"));
        links.add(LinkBuilder.link("confirm", uriInfo.getBaseUriBuilder()
                .path("managed-club-bookings/{bookingId}/confirmation")
                .resolveTemplate("bookingId", booking.getBookingId())
                .build(), "PUT"));
        dto.setLinks(links);

        return dto;
    }
}
