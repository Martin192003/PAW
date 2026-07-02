package ar.edu.itba.paw.webapp.rest.users;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.UserDto;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserMapperTest {

    @Test
    public void privateUserDtoIncludesMainNavigationLinks() {
        UUID userId = UUID.randomUUID();
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));

        UserDto dto = UserMapper.toDto(user(userId), uriInfo);
        Set<String> rels = dto.getLinks().stream()
                .map(link -> link.getRel())
                .collect(Collectors.toSet());

        assertTrue(rels.contains("self"));
        assertTrue(rels.contains("bookings"));
        assertTrue(rels.contains("reviews"));
        assertTrue(rels.contains("clubs"));
    }

    private static User user(UUID id) {
        User user = new User(
                id,
                "demo",
                "Demo",
                "User",
                "demo@example.com",
                "$2a$10$012345678901234567890uG0k34v3Ktb5sQ8kOLTNf5/4RswlD2oa",
                "1122334455",
                Role.USER,
                OffsetDateTime.now(),
                "es"
        );
        user.setVerified(true);
        return user;
    }
}
