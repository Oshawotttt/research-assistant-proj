package com.researchassistant.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** No ownerId: the owner always comes from the JWT, never the request body. */
public record FolderRequest(@NotBlank @Size(max = 255) String name) {
}
