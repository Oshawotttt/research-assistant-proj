package com.researchassistant.storage.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings the service cannot run without. Validating them here means a missing
 * value fails the context at startup instead of surfacing as a null halfway
 * through a request.
 */
@Validated
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(

		/** Base64-encoded, shared with User Management. Decode before use. */
		@NotBlank String jwtSecret,

		@NotBlank String researchEvaluationBaseUrl,

		@NotBlank String grobidUrl,

		/** Directory the FileStore writes uploaded PDFs to. */
		@NotNull Path fileStoragePath) {
}
