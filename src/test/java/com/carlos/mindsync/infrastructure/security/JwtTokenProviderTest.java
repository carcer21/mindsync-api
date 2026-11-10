package com.carlos.mindsync.infrastructure.security;

import com.carlos.mindsync.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

	private static final User ANA = User.register("Ana", "ana@example.com", "hashed-value");

	private static final Clock ONE_HOUR_AGO = Clock.offset(Clock.systemUTC(), Duration.ofHours(-1));

	private final JwtConfig config = new JwtConfig();
	private final SecretKey key = randomKey();
	private final JwtDecoder decoder = config.jwtDecoder(key);

	@Test
	void token_carriesUserIdIssuerAndExpiry() {
		String token = providerWith(key, Duration.ofMinutes(30)).generateToken(ANA);

		Jwt jwt = decoder.decode(token);

		assertThat(jwt.getSubject()).isEqualTo(ANA.id().toString());
		assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtConfig.ISSUER);
		assertThat(jwt.getExpiresAt()).isBetween(Instant.now().plus(Duration.ofMinutes(29)),
				Instant.now().plus(Duration.ofMinutes(31)));
	}

	@Test
	void token_containsNoPersonalData() {
		String token = providerWith(key, Duration.ofMinutes(30)).generateToken(ANA);

		assertThat(decoder.decode(token).getClaims()).containsOnlyKeys("iss", "sub", "iat", "exp");
	}

	@Test
	void expiredToken_isRejected() {
		String token = new JwtTokenProvider(config.jwtEncoder(key), Duration.ofMinutes(30), ONE_HOUR_AGO)
				.generateToken(ANA);

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void tokenSignedWithAnotherKey_isRejected() {
		String token = providerWith(randomKey(), Duration.ofMinutes(30)).generateToken(ANA);

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void tamperedToken_isRejected() {
		String token = providerWith(key, Duration.ofMinutes(30)).generateToken(ANA);
		String[] parts = token.split("\\.");
		String otherPayload = providerWith(key, Duration.ofMinutes(30))
				.generateToken(User.register("Eve", "eve@example.com", "hashed-value")).split("\\.")[1];

		// Ana's signature with Eve's payload.
		String tampered = parts[0] + "." + otherPayload + "." + parts[2];

		assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);
	}

	@Test
	void shortSecret_isRejectedAtStartup() {
		assertThatThrownBy(() -> config.jwtSecretKey("too-short"))
				.isInstanceOf(IllegalStateException.class);
	}

	private JwtTokenProvider providerWith(SecretKey signingKey, Duration expiration) {
		return new JwtTokenProvider(config.jwtEncoder(signingKey), expiration, Clock.systemUTC());
	}

	// Random per run: no fixed key lives in the repository.
	private SecretKey randomKey() {
		return config.jwtSecretKey(UUID.randomUUID().toString());
	}
}
