package com.researchassistant.usermanagement.dto;

import java.time.Instant;
import java.util.UUID;

/** Never carries passwordHash. */
public record UserResponse(UUID id, String email, Instant createdAt) {
}
