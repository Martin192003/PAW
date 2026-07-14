package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Receipt;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.dto.ReceiptDto;
import ar.edu.itba.paw.webapp.rest.LinkBuilder;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
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

        List<LinkDto> links = new ArrayList<>();
        links.add(LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                .path(BookingRestController.class)
                .path(BookingRestController.class, "getReceipt")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("bookingId", receipt.getBookingId())
                .build(), "GET"));
        links.add(LinkBuilder.link("booking", uriInfo.getBaseUriBuilder()
                .path("bookings/{bookingId}")
                .resolveTemplate("bookingId", receipt.getBookingId())
                .build(), "GET"));
        dto.setLinks(links);

        return dto;
    }
}
