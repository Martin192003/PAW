package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Receipt;
import ar.edu.itba.paw.model.dto.ReceiptDto;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReceiptDtoAssemblerTest {

    @Test
    public void receiptLinksUseCanonicalBookingResource() {
        UUID clubId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Receipt receipt = new Receipt(
                UUID.randomUUID(),
                bookingId,
                UUID.randomUUID(),
                null,
                "receipt.png",
                "image/png",
                3L,
                OffsetDateTime.now(),
                new byte[]{1, 2, 3}
        );
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));

        ReceiptDto dto = new ReceiptDtoAssembler().toDto(uriInfo, clubId, receipt);

        Map<String, String> links = dto.getLinks().stream()
                .collect(Collectors.toMap(link -> link.getRel(), link -> link.getHref()));
        assertEquals(
                "http://localhost/api/bookings/" + bookingId + "/receipt",
                links.get("self")
        );
        assertEquals(
                "http://localhost/api/bookings/" + bookingId,
                links.get("booking")
        );
    }
}
