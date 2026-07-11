package ar.edu.itba.paw.webapp.config;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpaFallbackFilterTest {

    @Test
    public void deepSpaRouteUsesContextPathAsBaseHref() throws Exception {
        SpaFallbackFilter filter = new SpaFallbackFilter();
        ReflectionTestUtils.setField(filter, "indexHtml",
                "<!doctype html><html><head><base href=\"/\"></head><body><app-root></app-root></body></html>");

        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/webapp_war_exploded/reset-password");
        request.setContextPath("/webapp_war_exploded");
        request.addHeader("Accept", "text/html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertEquals("no-cache", response.getHeader("Cache-Control"));
        assertTrue(response.getContentAsString().contains("<base href=\"/webapp_war_exploded/\">"));
    }
}
