package ar.edu.itba.paw.webapp.rest.products;

import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.dto.ProductDto;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
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

        Map<String, String> links = new HashMap<>();
        links.put("self", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/menu/{productId}")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("productId", product.getProductId())
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
