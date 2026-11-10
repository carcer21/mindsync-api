package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.TestcontainersConfiguration;
import com.carlos.mindsync.application.port.out.AiAdvisorPort;
import com.carlos.mindsync.domain.model.JournalEntry;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Real app + real PostgreSQL. The AI port is a mock and the key is forced empty, so even a real
// GEMINI_API_KEY in the developer's .env can never be used by a test.
@SpringBootTest(properties = { "app.gemini.api-key=", "app.gemini.base-url=http://127.0.0.1:1" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AdviceFlowIntegrationTest {

	private static final String PASSWORD = "s3cret-Passw0rd";
	private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private AiAdvisorPort advisor;

	private record Session(String userId, String token) {
	}

	@Test
	void advice_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/ai/advice")).andExpect(status().isUnauthorized());

		verify(advisor, never()).getAdvice(any());
	}

	@Test
	void advice_userWithoutEntries_getsFriendlyMessage_andAiIsNotCalled() throws Exception {
		Session ana = newUser("Ana");

		mockMvc.perform(get("/api/ai/advice").header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.advice").value(org.hamcrest.Matchers.containsString("Todavía no tienes entradas")));

		verify(advisor, never()).getAdvice(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void advice_sendsOnlyTheLatest14EntriesOfTheCaller_newestFirst() throws Exception {
		Session ana = newUser("Ana");
		Session ben = newUser("Ben");
		for (int i = 0; i < 16; i++) {
			createEntry(ana, TODAY.minusDays(i), "ana-note-" + i);
		}
		createEntry(ben, TODAY, "ben-private-note");
		when(advisor.getAdvice(any())).thenReturn("fake advice");

		mockMvc.perform(get("/api/ai/advice").param("userId", ben.userId())
						.header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.advice").value("fake advice"));

		ArgumentCaptor<List<JournalEntry>> sent = ArgumentCaptor.forClass(List.class);
		verify(advisor).getAdvice(sent.capture());
		assertThat(sent.getValue()).hasSize(14);
		assertThat(sent.getValue()).allSatisfy(e -> assertThat(e.userId()).hasToString(ana.userId()));
		assertThat(sent.getValue()).extracting(JournalEntry::notes).doesNotContain("ben-private-note");
		assertThat(sent.getValue().get(0).date()).isEqualTo(TODAY);
		assertThat(sent.getValue().get(13).date()).isEqualTo(TODAY.minusDays(13));
	}

	private Session newUser(String name) throws Exception {
		String email = "user-" + UUID.randomUUID() + "@example.com";
		String registerBody = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(name, email, PASSWORD)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String loginBody = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return new Session(JsonPath.read(registerBody, "$.id"), JsonPath.read(loginBody, "$.accessToken"));
	}

	private void createEntry(Session session, LocalDate date, String notes) throws Exception {
		mockMvc.perform(post("/api/entries").header("Authorization", "Bearer " + session.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"date\":\"%s\",\"energy\":5,\"emotion\":\"CALM\",\"notes\":\"%s\"}".formatted(date, notes)))
				.andExpect(status().isCreated());
	}
}
