package ar.edu.itba.paw.webapp.config;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Agrega headers de cache apropiados para los assets estaticos de Angular.
 *
 * Angular genera nombres con hash para todos sus assets (outputHashing: "all"),
 * por lo que esos archivos nunca cambian de contenido. Se les aplica cache
 * incondicional de un anio con la directiva "immutable".
 *
 * Politica:
 *   - Assets hasheados (*.js, *.css, *.woff2, *.png, etc. con segmento hash):
 *       Cache-Control: public, max-age=31536000, immutable
 *   - index.html, index.csr.html y noscript.html (entrypoints SPA):
 *       Cache-Control: no-store  (no guardar el shell de la SPA)
 */
public class StaticAssetsCacheFilter implements Filter {

    // Angular output hashes are alphanumeric, not only hexadecimal.
    // Examples: styles-L6HQ23U3.css, chunk-QASGRXRC.js.
    private static final Pattern HASHED_ASSET =
            Pattern.compile(".*[\\-\\.][0-9A-Za-z]{8,}\\.[a-zA-Z0-9]+$");

    private static final Pattern STATIC_EXTENSION =
            Pattern.compile(".*\\.(js|css|woff|woff2|ttf|otf|eot|svg|png|jpg|jpeg|webp|ico|map|json)$",
                    Pattern.CASE_INSENSITIVE);

    private static final long ONE_YEAR_SECONDS = 365L * 24 * 60 * 60; // 31 536 000

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String uri = httpReq.getRequestURI();

        if (isHashedStaticAsset(uri)) {
            // Cache incondicional: el contenido nunca cambia para este nombre de archivo.
            httpResp.setHeader("Cache-Control",
                    "public, max-age=" + ONE_YEAR_SECONDS + ", immutable");
        } else if (STATIC_EXTENSION.matcher(uri).matches()) {
            // Recursos estaticos no hasheados (favicon, public/, legacy resources)
            // no pueden recibir cache incondicional porque no tienen cache busting.
            httpResp.setHeader("Cache-Control", "no-cache");
        } else if (isIndexHtml(uri)) {
            // El index.html es el punto de entrada de la SPA; no debe cachearse.
            httpResp.setHeader("Cache-Control", "no-store");
            httpResp.setHeader("Pragma", "no-cache");
        }
        // Para cualquier otro recurso (API, etc.) no se toca el header.

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private boolean isHashedStaticAsset(String uri) {
        return STATIC_EXTENSION.matcher(uri).matches()
                && HASHED_ASSET.matcher(uri).matches();
    }

    private boolean isIndexHtml(String uri) {
        return uri.endsWith("/index.html")
                || uri.endsWith("/index.csr.html")
                || uri.endsWith("/noscript.html");
    }
}
