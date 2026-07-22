package ar.edu.itba.paw.webapp.rest.tables;

import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.dto.TableDto;
import ar.edu.itba.paw.webapp.rest.LinkBuilder;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TableDtoAssembler {

    public TableDto toDto(final UriInfo uriInfo, final UUID clubId, final ClubTable table) {
        return toDto(uriInfo, clubId, table, false);
    }

    public TableDto toDto(final UriInfo uriInfo, final UUID clubId, final ClubTable table, final boolean canManage) {
        TableDto dto = new TableDto();
        dto.setTableId(table.getTableId());
        dto.setClubId(clubId);
        dto.setTableNumber(table.getTableNumber());
        dto.setCapacity(table.getCapacity());
        dto.setTableState(table.getTableState());
        dto.setPrice(table.getPrice());
        dto.setDiscount(table.getDiscount());

        List<LinkDto> links = new ArrayList<>();
        var self = uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/tables/{tableId}")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("tableId", table.getTableId())
                .build();
        links.add(LinkBuilder.link("self", self, "GET"));
        links.add(LinkBuilder.link("club", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}")
                .resolveTemplate("clubId", clubId)
                .build(), "GET"));
        links.add(LinkBuilder.link("bookings", uriInfo.getBaseUriBuilder()
                .path("bookings")
                .queryParam("clubId", clubId)
                .build(), "GET"));
        if (canManage) {
            links.add(LinkBuilder.link("update", self, "PUT"));
            links.add(LinkBuilder.link("delete", self, "DELETE"));
        }
        dto.setLinks(links);

        return dto;
    }
}
