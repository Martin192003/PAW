package ar.edu.itba.paw.webapp.config;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;


public class StaticAssetsCacheFilter implements Filter {

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
            httpResp.setHeader("Cache-Control",
                    "public, max-age=" + ONE_YEAR_SECONDS + ", immutable");
        } else if (STATIC_EXTENSION.matcher(uri).matches()) {
            httpResp.setHeader("Cache-Control", "no-cache");
        } else if (isIndexHtml(uri)) {
            httpResp.setHeader("Cache-Control", "no-store");
            httpResp.setHeader("Pragma", "no-cache");
        }

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
