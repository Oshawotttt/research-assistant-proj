package com.researchassistant.usermanagement.user;

import com.researchassistant.usermanagement.dto.AuthResponse;
import com.researchassistant.usermanagement.dto.LoginRequest;
import com.researchassistant.usermanagement.dto.RegisterRequest;
import com.researchassistant.usermanagement.dto.UserResponse;
import com.researchassistant.usermanagement.error.EmailAlreadyUsedException;
import com.researchassistant.usermanagement.error.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalise(request.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        User saved = users.save(new User(email, passwordEncoder.encode(request.password())));
        return toAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalise(request.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Same exception, same message as an unknown email: the endpoint must
            // not reveal which addresses are registered.
            throw new InvalidCredentialsException();
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        TokenService.IssuedToken token = tokens.issue(user);
        return new AuthResponse(token.value(), token.expiresAt(), toUserResponse(user));
    }

    static UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }

    /** Stored lowercase so the unique index on the column is effectively case-insensitive. */
    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
