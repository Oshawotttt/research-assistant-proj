package com.researchassistant.usermanagement.dto;

import java.time.Instant;
import java.util.UUID;

public record FolderResponse(UUID id, String name, Instant createdAt) {
}
