package ar.edu.itba.paw.webapp.rest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

public final class ContentDispositionHeader {

    private ContentDispositionHeader() {
    }

    public static String inline(String filename, String fallbackFilename) {
        String original = usableFilename(filename, fallbackFilename);
        String asciiFallback = asciiFallback(original, fallbackFilename);
        return "inline; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodeFilename(original);
    }

    private static String usableFilename(String filename, String fallbackFilename) {
        if (filename != null && !filename.trim().isEmpty()) {
            return filename.trim();
        }
        return fallbackFilename;
    }

    private static String asciiFallback(String filename, String fallbackFilename) {
        String normalized = Normalizer.normalize(usableFilename(filename, fallbackFilename), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\x20-\\x7E]", " ")
                .replace("\"", "")
                .replace("\\", "")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.isEmpty() ? fallbackFilename : normalized;
    }

    private static String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
