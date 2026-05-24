package ar.edu.itba.paw.webapp.rest.products;

import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.dto.ProductDto;
import ar.edu.itba.paw.webapp.rest.LinkBuilder;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ProductDtoAssembler {

    public ProductDto toDto(final UriInfo uriInfo, final UUID clubId, final Product product) {
        ProductDto dto = new ProductDto();
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setDescription(product.getDescription());
        dto.setBasePrice(product.getBasePrice());
        dto.setProductType(product.getProductType());
        dto.setIsAvailable(product.getIsAvailable());
        dto.setCreatedAt(product.getCreatedAt());

        List<LinkDto> links = new ArrayList<>();
        links.add(LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/menu/{productId}")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("productId", product.getProductId())
                .build(), "GET"));
        links.add(LinkBuilder.link("club", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}")
                .resolveTemplate("clubId", clubId)
                .build(), "GET"));
        links.add(LinkBuilder.link("menu", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/menu")
                .resolveTemplate("clubId", clubId)
                .build(), "GET"));
        dto.setLinks(links);

        return dto;
    }
}
