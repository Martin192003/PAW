package ar.edu.itba.paw.webapp.rest.tables;

import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.dto.TableDto;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class TableDtoAssembler {

    public TableDto toDto(final UriInfo uriInfo, final UUID clubId, final ClubTable table) {
        TableDto dto = new TableDto();
        dto.setTableId(table.getTableId());
        dto.setClubId(clubId);
        dto.setTableNumber(table.getTableNumber());
        dto.setCapacity(table.getCapacity());
        dto.setTableState(table.getTableState());
        dto.setPrice(table.getPrice());
        dto.setDiscount(table.getDiscount());

        Map<String, String> links = new HashMap<>();
        links.put("self", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/tables/{tableId}")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("tableId", table.getTableId())
                .build()
                .toString());
        links.put("club", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}")
                .resolveTemplate("clubId", clubId)
                .build()
                .toString());
        dto.setLinks(links);

        return dto;
    }
}
