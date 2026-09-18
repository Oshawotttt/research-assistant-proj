package com.researchassistant.usermanagement.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Issues the token every other service trusts. The claim contract is documented
 * in README section 6 - changing it means edits in three other services.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder,
                        @Value("${jwt.issuer}") String issuer,
                        @Value("${jwt.ttl}") Duration ttl) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                // sub is the user UUID. Downstream services read jwt.getSubject().
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .build();

        // No JwsHeader: NimbusJwtEncoder.withSecretKey defaults to HS256.
        String value = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
