package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Real application and PostgreSQL; the Gemini key is forced empty so nothing external can be called.
@SpringBootTest(properties = { "app.gemini.api-key=", "app.gemini.base-url=http://127.0.0.1:1" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class StatsFlowIntegrationTest {

	private static final String PASSWORD = "s3cret-Passw0rd";
	private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private JdbcTemplate jdbc;

	private record Session(String userId, String token) {
	}

	@Test
	void stats_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/stats/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void stats_newUser_hasEmptyStats() throws Exception {
		Session ana = newUser();

		mockMvc.perform(get("/api/stats/me").header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodDays").value(90))
				.andExpect(jsonPath("$.currentStreak").value(0))
				.andExpect(jsonPath("$.averageEnergy").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.predominantEmotion").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.entriesThisMonth").value(0));
	}

	@Test
	void stats_areComputedFromTheCallersEntriesOfTheLast90DaysOnly() throws Exception {
		Session ana = newUser();
		Session ben = newUser();
		createEntry(ana, TODAY, 8, "JOY");
		createEntry(ana, TODAY.minusDays(1), 4, "JOY");
		createEntry(ana, TODAY.minusDays(2), 6, "STRESS");
		createEntry(ben, TODAY, 1, "ANGER"); // must not leak into Ana's numbers
		// Older than the window: inserted directly because it is outside what the stats must read.
		insertOldEntry(ana, TODAY.minusDays(120), 1, "ANGER");

		mockMvc.perform(get("/api/stats/me").param("userId", ben.userId())
						.header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentStreak").value(3))
				.andExpect(jsonPath("$.averageEnergy").value(6.0))
				.andExpect(jsonPath("$.predominantEmotion").value("JOY"))
				.andExpect(jsonPath("$.entriesThisMonth").value(
						(int) java.util.stream.Stream.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2))
								.filter(d -> d.getMonth() == TODAY.getMonth()).count()));
	}

	private Session newUser() throws Exception {
		String email = "user-" + UUID.randomUUID() + "@example.com";
		String registerBody = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Demo\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String loginBody = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return new Session(JsonPath.read(registerBody, "$.id"), JsonPath.read(loginBody, "$.accessToken"));
	}

	private void createEntry(Session session, LocalDate date, int energy, String emotion) throws Exception {
		mockMvc.perform(post("/api/entries").header("Authorization", "Bearer " + session.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"date\":\"%s\",\"energy\":%d,\"emotion\":\"%s\"}".formatted(date, energy, emotion)))
				.andExpect(status().isCreated());
	}

	private void insertOldEntry(Session session, LocalDate date, int energy, String emotion) {
		jdbc.update("insert into journal_entries (id, user_id, entry_date, energy, emotion) values (?, ?::uuid, ?, ?, ?)",
				UUID.randomUUID(), session.userId(), date, energy, emotion);
	}
}
