package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.GetAdviceUseCase;
import com.carlos.mindsync.application.port.in.LoginUseCase;
import com.carlos.mindsync.application.port.in.RegisterUserUseCase;
import com.carlos.mindsync.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Limit of 2 and a long window, so the window never ends in the middle of a test.
// Counters live as long as the Spring context: every test uses its own IP or user.
@WebMvcTest(controllers = {AuthController.class, AiController.class}, properties = {
		"app.rate-limit.auth.limit=2", "app.rate-limit.auth.window=1h",
		"app.rate-limit.ai.limit=2", "app.rate-limit.ai.window=1h"})
@Import(SecurityConfig.class)
class RateLimitTest {

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private LoginUseCase login;
	@MockitoBean
	private RegisterUserUseCase registerUser;
	@MockitoBean
	private GetAdviceUseCase getAdvice;
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void login_overTheLimit_returns429ProblemDetail_andThePasswordIsNotEvenChecked() throws Exception {
		when(login.login(any())).thenReturn("token");

		mockMvc.perform(loginFrom("10.0.0.1")).andExpect(status().isOk());
		mockMvc.perform(loginFrom("10.0.0.1")).andExpect(status().isOk());

		mockMvc.perform(loginFrom("10.0.0.1"))
				.andExpect(status().isTooManyRequests())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.detail").value("too many requests, try again later"))
				.andExpect(header().exists("Retry-After"));

		verify(login, times(2)).login(any());
	}

	@Test
	void login_limitIsPerClientAddress() throws Exception {
		when(login.login(any())).thenReturn("token");
		mockMvc.perform(loginFrom("10.0.0.2"));
		mockMvc.perform(loginFrom("10.0.0.2"));

		mockMvc.perform(loginFrom("10.0.0.2")).andExpect(status().isTooManyRequests());
		mockMvc.perform(loginFrom("10.0.0.3")).andExpect(status().isOk());
	}

	@Test
	void login_aForgedForwardedHeaderDoesNotGiveANewCounter() throws Exception {
		when(login.login(any())).thenReturn("token");
		mockMvc.perform(loginFrom("10.0.0.4"));
		mockMvc.perform(loginFrom("10.0.0.4"));

		mockMvc.perform(loginFrom("10.0.0.4").header("X-Forwarded-For", "203.0.113.7"))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void register_sharesTheCounterWithLogin() throws Exception {
		when(login.login(any())).thenReturn("token");
		mockMvc.perform(loginFrom("10.0.0.5"));
		mockMvc.perform(loginFrom("10.0.0.5"));

		mockMvc.perform(post("/api/auth/register").with(remoteAddr("10.0.0.5"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"s3cret-Passw0rd\"}"))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void advice_limitIsPerUser_andTheAiIsNotCalledOverTheLimit() throws Exception {
		UUID ana = UUID.randomUUID();
		UUID luis = UUID.randomUUID();
		when(getAdvice.getAdvice(any())).thenReturn("ok");

		mockMvc.perform(adviceAs(ana)).andExpect(status().isOk());
		mockMvc.perform(adviceAs(ana)).andExpect(status().isOk());

		mockMvc.perform(adviceAs(ana))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));
		mockMvc.perform(adviceAs(luis)).andExpect(status().isOk());

		verify(getAdvice, times(2)).getAdvice(ana);
	}

	private static MockHttpServletRequestBuilder loginFrom(String address) {
		return post("/api/auth/login").with(remoteAddr(address))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ana@example.com\",\"password\":\"s3cret-Passw0rd\"}");
	}

	private static MockHttpServletRequestBuilder adviceAs(UUID userId) {
		return get("/api/ai/advice").with(jwt().jwt(token -> token.subject(userId.toString())));
	}

	private static RequestPostProcessor remoteAddr(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}
}
