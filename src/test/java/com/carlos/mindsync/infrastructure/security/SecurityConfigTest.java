package com.carlos.mindsync.infrastructure.security;

import com.carlos.mindsync.TestcontainersConfiguration;
import com.carlos.mindsync.application.port.out.TokenProviderPort;
import com.carlos.mindsync.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SecurityConfigTest {

	// A path with no controller: with a valid token the request passes security and ends in 404.
	private static final String PROTECTED_PATH = "/api/protected-path-without-controller";
	private static final User ANA = User.register("Ana", "ana@example.com", "hashed-value");

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private TokenProviderPort tokenProvider;
	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void health_isPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void protectedPath_withoutToken_returns401() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH)).andExpect(status().isUnauthorized());
	}

	@Test
	void otherActuatorEndpoints_areNotPublic() throws Exception {
		mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
	}

	@Test
	void protectedPath_withValidToken_passesSecurity() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + tokenProvider.generateToken(ANA)))
				.andExpect(status().isNotFound());
	}

	@Test
	void protectedPath_withGarbageToken_returns401() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer not-a-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedPath_withExpiredToken_returns401() throws Exception {
		String expired = new JwtTokenProvider(jwtEncoder, Duration.ofMinutes(30),
				Clock.offset(Clock.systemUTC(), Duration.ofHours(-1))).generateToken(ANA);

		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + expired))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedPath_withTokenSignedByAnotherKey_returns401() throws Exception {
		JwtConfig config = new JwtConfig();
		JwtEncoder attackerEncoder = config.jwtEncoder(config.jwtSecretKey(UUID.randomUUID().toString()));
		String forged = new JwtTokenProvider(attackerEncoder, Duration.ofMinutes(30), Clock.systemUTC())
				.generateToken(ANA);

		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + forged))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedPath_withUnsignedToken_returns401() throws Exception {
		// Classic attack: a token that declares "alg: none" and carries no signature.
		String header = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
		String payload = base64Url("{\"iss\":\"mindsync\",\"sub\":\"" + ANA.id() + "\",\"exp\":"
				+ Instant.now().plusSeconds(600).getEpochSecond() + "}");

		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + header + "." + payload + "."))
				.andExpect(status().isUnauthorized());
	}

	private static String base64Url(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}
}
