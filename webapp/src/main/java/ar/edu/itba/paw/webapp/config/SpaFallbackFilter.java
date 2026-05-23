package ar.edu.itba.paw.webapp.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class SpaFallbackFilter implements Filter {

    private static final String INDEX_HTML = "/index.csr.html";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = getApplicationPath(httpRequest);

        if (shouldServeIndex(httpRequest, path)) {
            request.getRequestDispatcher(INDEX_HTML).forward(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean shouldServeIndex(HttpServletRequest request, String path) {
        String method = request.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return false;
        }
        if (path == null || path.isBlank() || INDEX_HTML.equals(path)) {
            return false;
        }
        if (path.equals("/api") || path.startsWith("/api/")) {
            return false;
        }
        if (path.startsWith("/assets/") || path.startsWith("/resources/")) {
            return false;
        }
        if (hasFileExtension(path)) {
            return false;
        }

        String accept = request.getHeader("Accept");
        return accept == null || accept.contains("text/html") || accept.contains("*/*");
    }

    private String getApplicationPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private boolean hasFileExtension(String path) {
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        return lastDot > lastSlash;
    }
}
