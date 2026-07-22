package ar.edu.itba.paw.webapp.config;

import org.junit.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WebAuthConfigPasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new WebAuthConfig().passwordEncoder();

    @Test
    public void legacyPlaintextMatchesExactlyAndRequiresUpgrade() {
        assertTrue(passwordEncoder.matches("old-password", "old-password"));
        assertFalse(passwordEncoder.matches("wrong-password", "old-password"));
        assertTrue(passwordEncoder.upgradeEncoding("old-password"));
    }

    @Test
    public void newlyEncodedPasswordUsesBcryptAndDoesNotRequireUpgrade() {
        String encoded = passwordEncoder.encode("new-password");

        assertTrue(encoded.startsWith("$2"));
        assertTrue(passwordEncoder.matches("new-password", encoded));
        assertFalse(passwordEncoder.matches("wrong-password", encoded));
        assertFalse(passwordEncoder.upgradeEncoding(encoded));
    }
}
