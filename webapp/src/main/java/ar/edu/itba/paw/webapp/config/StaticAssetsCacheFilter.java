package ar.edu.itba.paw.webapp.config;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Agrega headers de cache apropiados para los assets estáticos de Angular.
 *
 * Angular genera nombres con hash para todos sus assets (outputHashing: "all"),
 * por lo que esos archivos nunca cambian de contenido. Se les aplica cache
 * incondicional de un año con la directiva "immutable".
 *
 * Política:
 *   - Assets hasheados (*.js, *.css, *.woff2, *.png, etc. con segmento hash):
 *       Cache-Control: public, max-age=31536000, immutable
 *   - index.html y noscript.html (no hasheados, punto de entrada de la SPA):
 *       Cache-Control: no-cache  (siempre revalidar)
 */
public class StaticAssetsCacheFilter implements Filter {

    // Patrón que detecta un segmento de hash típico de Angular (8+ hex chars entre guiones o puntos)
    // Ejemplos: main.A1B2C3D4.js, styles.deadbeef1234.css, chunk-ABCD1234.js
    private static final Pattern HASHED_ASSET =
            Pattern.compile(".*[\\-\\.][0-9A-Fa-f]{8,}\\.[a-zA-Z0-9]+$");

    // Extensiones de assets estáticos servidos por el WAR
    private static final Pattern STATIC_EXTENSION =
            Pattern.compile(".*\\.(js|css|woff|woff2|ttf|eot|svg|png|jpg|jpeg|webp|ico|map)$",
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
        } else if (isIndexHtml(uri)) {
            // El index.html debe revalidarse siempre; es el punto de entrada de la SPA.
            httpResp.setHeader("Cache-Control", "no-cache");
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
        return uri.endsWith("/index.html") || uri.endsWith("/noscript.html");
    }
}
