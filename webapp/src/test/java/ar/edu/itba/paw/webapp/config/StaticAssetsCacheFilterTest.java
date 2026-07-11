package ar.edu.itba.paw.webapp.config;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;

public class StaticAssetsCacheFilterTest {

    @Test
    public void hashedAngularAssetsGetImmutableCacheHeader() throws Exception {
        StaticAssetsCacheFilter filter = new StaticAssetsCacheFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assets/styles-L6HQ23U3.css");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("public, max-age=31536000, immutable", response.getHeader("Cache-Control"));
    }

    @Test
    public void indexCsrHtmlIsNotCachedAggressively() throws Exception {
        StaticAssetsCacheFilter filter = new StaticAssetsCacheFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.csr.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("no-cache", response.getHeader("Cache-Control"));
    }
}
