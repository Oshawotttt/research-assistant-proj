package com.researchassistant.usermanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the bearer scheme so Swagger UI shows an Authorize button.
 * Without it every protected endpoint returns 401 from the UI and the demo stalls.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearer-jwt";

    @Bean
    OpenAPI userManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Management API")
                        .version("v1")
                        .description("Accounts, research folders and JWT issuance. "
                                + "Log in via /auth/login, then paste the token into Authorize."))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
