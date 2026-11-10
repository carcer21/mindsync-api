package com.carlos.mindsync.infrastructure.security;

import com.carlos.mindsync.application.port.out.TokenProviderPort;
import com.carlos.mindsync.domain.model.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

class JwtTokenProvider implements TokenProviderPort {

	private final JwtEncoder encoder;
	private final Duration expiration;
	private final Clock clock;

	JwtTokenProvider(JwtEncoder encoder, Duration expiration, Clock clock) {
		this.encoder = encoder;
		this.expiration = expiration;
		this.clock = clock;
	}

	@Override
	public String generateToken(User user) {
		Instant now = clock.instant();
		// Only the user id travels in the token: no email, no name, nothing sensitive.
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(JwtConfig.ISSUER)
				.subject(user.id().toString())
				.issuedAt(now)
				.expiresAt(now.plus(expiration))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
