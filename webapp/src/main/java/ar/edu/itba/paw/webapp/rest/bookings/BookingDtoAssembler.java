package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.dto.BookingDto;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
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

        Map<String, String> links = new HashMap<>();
        links.put("self", uriInfo.getBaseUriBuilder()
                .path(BookingRestController.class)
                .path(BookingRestController.class, "getBooking")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("bookingId", booking.getBookingId())
                .build()
                .toString());
        links.put("club", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}")
                .resolveTemplate("clubId", clubId)
                .build()
                .toString());
        links.put("table", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/tables/{tableId}")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("tableId", tableId)
                .build()
                .toString());
        links.put("receipt", uriInfo.getBaseUriBuilder()
                .path(BookingRestController.class)
                .path(BookingRestController.class, "getReceipt")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("bookingId", booking.getBookingId())
                .build()
                .toString());
        dto.setLinks(links);

        return dto;
    }
}
