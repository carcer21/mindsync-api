package com.carlos.mindsync.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {
			"/api/auth/**",
			"/actuator/health", "/actuator/health/**",
			"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
	};

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				// No cookies or sessions: every request carries its own token, so CSRF does not apply.
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_PATHS).permitAll()
						.anyRequest().authenticated())
				// Validates the "Authorization: Bearer <token>" header with the JwtDecoder bean.
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
				.build();
	}
}
