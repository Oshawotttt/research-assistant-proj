package com.researchassistant.usermanagement.user;

import com.researchassistant.usermanagement.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the cross-service claim contract in CONTRACTS.md. If one of these
 * fails, Storage Management, Research Evaluation and Updating break too.
 *
 * Plain JUnit with no Spring context, so it runs without a database.
 */
class TokenServiceTest {

    // base64, per CONTRACTS.md
    private static final String SECRET = "dXNlci1tYW5hZ2VtZW50LXRlc3Qtc2VjcmV0LTMyYiE=";
    private static final String OTHER_SECRET = "YS1jb21wbGV0ZWx5LWRpZmZlcmVudC1zZWNyZXQtMzI=";
    private static final String ISSUER = "user-management";

    /** Uses the production key derivation, so a change there breaks these tests. */
    private static SecretKey key(String base64Secret) {
        return new SecurityConfig().jwtSecretKey(base64Secret);
    }

    private static TokenService tokenService(SecretKey k) {
        JwtEncoder encoder = NimbusJwtEncoder.withSecretKey(k).build();
        return new TokenService(encoder, ISSUER, Duration.ofHours(2));
    }

    private static User userWithId(UUID id, String email) {
        User user = new User(email, "irrelevant-hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void subjectIsTheUserUuid() {
        SecretKey k = key(SECRET);
        UUID id = UUID.randomUUID();

        String token = tokenService(k).issue(userWithId(id, "a@example.com")).value();
        Jwt decoded = NimbusJwtDecoder.withSecretKey(k).build().decode(token);

        // Downstream services read exactly this.
        assertEquals(id.toString(), decoded.getSubject());
        assertEquals(ISSUER, decoded.getClaimAsString("iss"));
        assertEquals("a@example.com", decoded.getClaimAsString("email"));
    }

    @Test
    void tokenIsSignedHs256AndExpiresInTwoHours() {
        SecretKey k = key(SECRET);
        TokenService.IssuedToken issued =
                tokenService(k).issue(userWithId(UUID.randomUUID(), "b@example.com"));

        Jwt decoded = NimbusJwtDecoder.withSecretKey(k).build().decode(issued.value());
        assertEquals("HS256", decoded.getHeaders().get("alg").toString());

        long seconds = Duration.between(Instant.now(), issued.expiresAt()).toSeconds();
        assertTrue(seconds > 7000 && seconds <= 7200, "expected ~2h TTL, got " + seconds + "s");
    }

    @Test
    void tokenFromADifferentSecretIsRejected() {
        String token = tokenService(key(SECRET))
                .issue(userWithId(UUID.randomUUID(), "c@example.com")).value();

        JwtDecoder wrongKey = NimbusJwtDecoder.withSecretKey(key(OTHER_SECRET)).build();

        // Stops a service holding the wrong JWT_SECRET from accepting forged tokens.
        assertThrows(JwtException.class, () -> wrongKey.decode(token));
    }
}
