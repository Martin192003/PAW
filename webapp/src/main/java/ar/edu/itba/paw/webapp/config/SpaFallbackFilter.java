package ar.edu.itba.paw.webapp.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SpaFallbackFilter implements Filter {

    private static final String INDEX_HTML = "/index.csr.html";
    private String indexHtml;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try (InputStream input = filterConfig.getServletContext().getResourceAsStream(INDEX_HTML)) {
            if (input == null) {
                throw new ServletException("Missing SPA entrypoint " + INDEX_HTML);
            }
            this.indexHtml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ServletException("Could not read SPA entrypoint " + INDEX_HTML, e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = getApplicationPath(httpRequest);

        if (shouldServeIndex(httpRequest, path)) {
            serveIndex(httpRequest, httpResponse);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean shouldServeIndex(HttpServletRequest request, String path) {
        String method = request.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return false;
        }
        if (INDEX_HTML.equals(path)) {
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

    private void serveIndex(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getContextPath();
        String baseHref = (contextPath == null || contextPath.isBlank()) ? "/" : contextPath + "/";
        String html = indexHtml.replace("<base href=\"/\">", "<base href=\"" + baseHref + "\">");

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        if (!"HEAD".equalsIgnoreCase(request.getMethod())) {
            response.getWriter().write(html);
        }
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
