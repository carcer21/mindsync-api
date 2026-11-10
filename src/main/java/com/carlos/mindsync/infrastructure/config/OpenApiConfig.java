package com.carlos.mindsync.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

	static final String BEARER_SCHEME = "bearerAuth";

	@Bean
	OpenAPI mindSyncOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("MindSync API")
						.version("2.0")
						.description("Emotional journal with AI-generated advice. Log in at POST /api/auth/login, "
								+ "then press Authorize and paste the accessToken (without the word Bearer)."))
				.components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")))
				// Every endpoint needs the token unless it declares an empty @SecurityRequirements (see AuthController).
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
	}
}
