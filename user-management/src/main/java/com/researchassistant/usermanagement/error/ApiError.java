package com.researchassistant.usermanagement.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent error shape for every endpoint. The frontend renders `message`
 * directly, so it must always be human-readable.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, Map<String, String> fieldErrors, Instant timestamp) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, null, Instant.now());
    }

    public static ApiError of(int status, String message, Map<String, String> fieldErrors) {
        return new ApiError(status, message, fieldErrors, Instant.now());
    }
}
