package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Receipt;
import ar.edu.itba.paw.model.dto.ReceiptDto;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ReceiptDtoAssembler {

    public ReceiptDto toDto(UriInfo uriInfo, UUID clubId, Receipt receipt) {
        ReceiptDto dto = new ReceiptDto();
        dto.setId(receipt.getReceiptId());
        dto.setBookingId(receipt.getBookingId());
        dto.setUploadedBy(receipt.getUploadedBy());
        dto.setFilename(receipt.getFilename());
        dto.setContentType(receipt.getContentType());
        dto.setSizeBytes(receipt.getSizeBytes());
        dto.setCreatedAt(receipt.getCreatedAt());

        Map<String, String> links = new HashMap<>();
        links.put("self", uriInfo.getBaseUriBuilder()
                .path(BookingRestController.class)
                .path(BookingRestController.class, "getReceipt")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("bookingId", receipt.getBookingId())
                .build()
                .toString());
        links.put("booking", uriInfo.getBaseUriBuilder()
                .path(BookingRestController.class)
                .path(BookingRestController.class, "getBooking")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("bookingId", receipt.getBookingId())
                .build()
                .toString());
        dto.setLinks(links);

        return dto;
    }
}
