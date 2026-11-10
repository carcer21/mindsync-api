package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.GetStatsUseCase;
import com.carlos.mindsync.domain.model.Emotion;
import com.carlos.mindsync.domain.model.JournalStats;
import com.carlos.mindsync.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
@Import(SecurityConfig.class)
class StatsControllerTest {

	private static final UUID ANA = UUID.randomUUID();
	private static final UUID SOMEONE_ELSE = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private GetStatsUseCase getStats;
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void me_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/stats/me")).andExpect(status().isUnauthorized());

		verifyNoInteractions(getStats);
	}

	@Test
	void me_returnsTheStatsOfTheTokenOwner() throws Exception {
		when(getStats.getStats(ANA)).thenReturn(new JournalStats(4, 6.5, Emotion.CALM, 9));

		mockMvc.perform(get("/api/stats/me").with(jwt().jwt(token -> token.subject(ANA.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodDays").value(90))
				.andExpect(jsonPath("$.currentStreak").value(4))
				.andExpect(jsonPath("$.averageEnergy").value(6.5))
				.andExpect(jsonPath("$.predominantEmotion").value("CALM"))
				.andExpect(jsonPath("$.entriesThisMonth").value(9));
	}

	@Test
	void me_userWithoutEntries_returnsNullsAndZeros() throws Exception {
		when(getStats.getStats(ANA)).thenReturn(new JournalStats(0, null, null, 0));

		mockMvc.perform(get("/api/stats/me").with(jwt().jwt(token -> token.subject(ANA.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentStreak").value(0))
				.andExpect(jsonPath("$.averageEnergy").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.predominantEmotion").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.entriesThisMonth").value(0));
	}

	@Test
	void me_ignoresAnyUserIdSentByTheClient() throws Exception {
		when(getStats.getStats(ANA)).thenReturn(new JournalStats(0, null, null, 0));

		mockMvc.perform(get("/api/stats/me").param("userId", SOMEONE_ELSE.toString())
						.header("X-User-Id", SOMEONE_ELSE.toString())
						.with(jwt().jwt(token -> token.subject(ANA.toString()))))
				.andExpect(status().isOk());

		verify(getStats).getStats(ANA);
		verify(getStats, never()).getStats(SOMEONE_ELSE);
	}

	@Test
	void me_unexpectedError_returnsGeneric500() throws Exception {
		when(getStats.getStats(any())).thenThrow(new IllegalStateException("db down"));

		mockMvc.perform(get("/api/stats/me").with(jwt().jwt(token -> token.subject(ANA.toString()))))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value("unexpected error"));
	}

	@Test
	void me_onlyAcceptsGet() throws Exception {
		mockMvc.perform(post("/api/stats/me").with(jwt().jwt(token -> token.subject(ANA.toString()))))
				.andExpect(status().isMethodNotAllowed());
	}
}
