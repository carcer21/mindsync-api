package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.GetAdviceUseCase;
import com.carlos.mindsync.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@Import(SecurityConfig.class)
class AiControllerTest {

	private static final UUID ANA = UUID.randomUUID();
	private static final UUID SOMEONE_ELSE = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private GetAdviceUseCase getAdvice;
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void advice_withoutToken_returns401_andNothingIsAnalysed() throws Exception {
		mockMvc.perform(get("/api/ai/advice")).andExpect(status().isUnauthorized());

		verifyNoInteractions(getAdvice);
	}

	@Test
	void advice_returnsTheTextForTheTokenOwner() throws Exception {
		when(getAdvice.getAdvice(ANA)).thenReturn("Take care of your rest.");

		mockMvc.perform(get("/api/ai/advice").with(jwt().jwt(token -> token.subject(ANA.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.advice").value("Take care of your rest."));
	}

	@Test
	void advice_ignoresAnyUserIdSentByTheClient() throws Exception {
		when(getAdvice.getAdvice(ANA)).thenReturn("ok");

		mockMvc.perform(get("/api/ai/advice").param("userId", SOMEONE_ELSE.toString())
						.header("X-User-Id", SOMEONE_ELSE.toString())
						.with(jwt().jwt(token -> token.subject(ANA.toString()))))
				.andExpect(status().isOk());

		verify(getAdvice).getAdvice(ANA);
		verify(getAdvice, never()).getAdvice(SOMEONE_ELSE);
	}

	@Test
	void advice_unexpectedError_returnsGeneric500() throws Exception {
		when(getAdvice.getAdvice(any())).thenThrow(new IllegalStateException("db down"));

		mockMvc.perform(get("/api/ai/advice").with(jwt().jwt(token -> token.subject(ANA.toString()))))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value("unexpected error"));
	}

	@Test
	void advice_onlyAcceptsGet() throws Exception {
		mockMvc.perform(post("/api/ai/advice").with(jwt().jwt(token -> token.subject(ANA.toString()))))
				.andExpect(status().isMethodNotAllowed());

		verifyNoInteractions(getAdvice);
	}
}
