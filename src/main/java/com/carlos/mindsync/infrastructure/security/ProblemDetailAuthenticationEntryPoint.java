package com.carlos.mindsync.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Security filters run before MVC, so @RestControllerAdvice never sees a 401. This writes the same
// application/problem+json shape by hand. The generic detail hides why the token was rejected.
class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final String BODY =
			"{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
					+ "\"detail\":\"authentication required\"}";

	// Sets the 401 status and the standard WWW-Authenticate: Bearer challenge.
	private final BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		bearer.commence(request, response, authException);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(BODY);
	}
}
