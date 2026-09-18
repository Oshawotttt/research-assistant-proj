package com.researchassistant.usermanagement.user;

import com.researchassistant.usermanagement.config.SecurityConfig;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CONTRACTS.md: "JWT_SECRET is shared as base64 of 32+ random bytes. Every
 * service base64-decodes it before use - a mismatched encoding is the classic
 * Java/Python JWT bug."
 *
 * These tests exist so nobody quietly changes this back to raw getBytes(),
 * which would silently break every cross-service token.
 */
class JwtSecretKeyTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void base64SecretIsDecodedToRawBytes() {
        // 32 ASCII bytes, base64-encoded
        SecretKey key = config.jwtSecretKey("dXNlci1tYW5hZ2VtZW50LXRlc3Qtc2VjcmV0LTMyYiE=");

        // 32 bytes decoded, NOT the 44 characters of the base64 text.
        assertEquals(32, key.getEncoded().length,
                "secret must be base64-DECODED, not used as raw text");
        assertEquals("user-management-test-secret-32b!",
                new String(key.getEncoded()));
    }

    @Test
    void nonBase64SecretIsRejectedWithAHelpfulMessage() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> config.jwtSecretKey("not valid base64 !!!"));
        assertTrue(ex.getMessage().contains("base64"), ex.getMessage());
    }

    @Test
    void secretShorterThan32BytesIsRejected() {
        String tooShort = java.util.Base64.getEncoder()
                .encodeToString("only-16-bytes!!!".getBytes());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> config.jwtSecretKey(tooShort));
        assertTrue(ex.getMessage().contains("32 bytes"), ex.getMessage());
    }
}
