package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.LinkDto;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class CollectionLinks {

    private CollectionLinks() {
    }

    public static <T> List<LinkDto> items(Collection<T> resources, Function<T, URI> uriFactory) {
        return resources.stream()
                .map(resource -> LinkBuilder.link("item", uriFactory.apply(resource), "GET"))
                .toList();
    }

    public static URI uri(UriBuilder base, String path, Object... values) {
        return base.clone().path(path).build(values);
    }
}
