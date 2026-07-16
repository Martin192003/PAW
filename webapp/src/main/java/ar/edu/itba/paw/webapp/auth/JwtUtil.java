package ar.edu.itba.paw.webapp.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMillis;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expirationMinutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secretBytes(secret));
        this.expirationMillis = expirationMinutes * 60L * 1000L;
    }

    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000L;
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] secretBytes(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("jwt.secret must be configured");
        }

        String trimmed = secret.trim();
        try {
            byte[] decoded = Decoders.BASE64.decode(trimmed);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (RuntimeException ignored) {
            // Plain-text secrets are accepted below for deploy configurations.
        }

        byte[] raw = trimmed.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 bytes for HS256");
        }
        return raw;
    }
}
