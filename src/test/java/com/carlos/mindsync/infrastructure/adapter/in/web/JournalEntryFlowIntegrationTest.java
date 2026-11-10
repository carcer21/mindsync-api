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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Whole application, real PostgreSQL, real tokens obtained through register + login.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class JournalEntryFlowIntegrationTest {

	private static final String PASSWORD = "s3cret-Passw0rd";
	private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private JdbcTemplate jdbc;

	private record Session(String userId, String token) {
	}

	// ---- happy path

	@Test
	void create_get_list_delete_fullFlow() throws Exception {
		Session ana = newUser("Ana");

		MvcResult created = createEntry(ana, entryJson(TODAY, 7, "JOY", "invented note", "\"work\",\"Family\""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").doesNotExist())
				.andReturn();
		String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/entries/" + id);

		mockMvc.perform(get("/api/entries/" + id).header("Authorization", bearer(ana)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.date").value(TODAY.toString()))
				.andExpect(jsonPath("$.energy").value(7))
				.andExpect(jsonPath("$.emotion").value("JOY"))
				.andExpect(jsonPath("$.notes").value("invented note"))
				.andExpect(jsonPath("$.tags[0]").value("family"))
				.andExpect(jsonPath("$.tags[1]").value("work"));

		mockMvc.perform(get("/api/entries").header("Authorization", bearer(ana)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(id));

		mockMvc.perform(delete("/api/entries/" + id).header("Authorization", bearer(ana)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/entries/" + id).header("Authorization", bearer(ana)))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/entries/" + id).header("Authorization", bearer(ana)))
				.andExpect(status().isNotFound());
		assertThat(entryRows(id)).isZero();
		assertThat(tagRows(id)).isZero();
	}

	@Test
	void list_isPaginated_newestFirst() throws Exception {
		Session ana = newUser("Ana");
		createEntry(ana, entryJson(TODAY.minusDays(2), 3, "SADNESS", null, ""));
		createEntry(ana, entryJson(TODAY, 8, "JOY", null, ""));
		createEntry(ana, entryJson(TODAY.minusDays(1), 5, "NEUTRAL", null, ""));

		mockMvc.perform(get("/api/entries").param("size", "2").header("Authorization", bearer(ana)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].date").value(TODAY.toString()))
				.andExpect(jsonPath("$.content[1].date").value(TODAY.minusDays(1).toString()))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));

		mockMvc.perform(get("/api/entries").param("size", "2").param("page", "1").header("Authorization", bearer(ana)))
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].date").value(TODAY.minusDays(2).toString()));

		// A huge page size is clamped by the server.
		mockMvc.perform(get("/api/entries").param("size", "100000").header("Authorization", bearer(ana)))
				.andExpect(jsonPath("$.size").value(50));
	}

	// ---- IDOR / BOLA: one user must never reach another user's entries

	@Test
	void idor_userCannotReadAnotherUsersEntry() throws Exception {
		Session ana = newUser("Ana");
		Session ben = newUser("Ben");
		String anaEntry = createAndGetId(ana, entryJson(TODAY, 2, "ANXIETY", "private invented note", "\"health\""));

		String body = mockMvc.perform(get("/api/entries/" + anaEntry).header("Authorization", bearer(ben)))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain("private invented note").doesNotContain("ANXIETY").doesNotContain("health");
	}

	@Test
	void idor_userCannotDeleteAnotherUsersEntry() throws Exception {
		Session ana = newUser("Ana");
		Session ben = newUser("Ben");
		String anaEntry = createAndGetId(ana, entryJson(TODAY, 2, "ANXIETY", "private invented note", "\"health\""));

		mockMvc.perform(delete("/api/entries/" + anaEntry).header("Authorization", bearer(ben)))
				.andExpect(status().isNotFound());

		// The entry and its tags are still in the database, and Ana still sees it.
		assertThat(entryRows(anaEntry)).isEqualTo(1);
		assertThat(tagRows(anaEntry)).isEqualTo(1);
		mockMvc.perform(get("/api/entries/" + anaEntry).header("Authorization", bearer(ana)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notes").value("private invented note"));
	}

	@Test
	void idor_listNeverContainsAnotherUsersEntries() throws Exception {
		Session ana = newUser("Ana");
		Session ben = newUser("Ben");
		createEntry(ana, entryJson(TODAY, 2, "ANXIETY", "private invented note", ""));
		String benEntry = createAndGetId(ben, entryJson(TODAY, 9, "JOY", null, ""));

		String benList = mockMvc.perform(get("/api/entries").param("userId", ana.userId())
						.header("X-User-Id", ana.userId()).header("Authorization", bearer(ben)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(benEntry))
				.andReturn().getResponse().getContentAsString();

		assertThat(benList).doesNotContain("private invented note");
	}

	@Test
	void idor_foreignEntryLooksExactlyLikeAMissingOne() throws Exception {
		Session ana = newUser("Ana");
		Session ben = newUser("Ben");
		String anaEntry = createAndGetId(ana, entryJson(TODAY, 2, "ANXIETY", null, ""));

		String foreign = mockMvc.perform(get("/api/entries/" + anaEntry).header("Authorization", bearer(ben)))
				.andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
		String missing = mockMvc.perform(get("/api/entries/" + UUID.randomUUID()).header("Authorization", bearer(ben)))
				.andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();

		// Same status, title and detail: the response does not reveal that the entry exists.
		assertThat(JsonPath.<String>read(foreign, "$.detail")).isEqualTo(JsonPath.<String>read(missing, "$.detail"));
		assertThat(JsonPath.<String>read(foreign, "$.title")).isEqualTo(JsonPath.<String>read(missing, "$.title"));
	}

	@Test
	void idor_userCannotCreateAnEntryForAnotherUser() throws Exception {
		Session ana = newUser("Ana");
		Session ben = newUser("Ben");
		String body = "{\"userId\":\"" + ana.userId() + "\",\"date\":\"" + TODAY + "\",\"energy\":5,\"emotion\":\"CALM\"}";

		String id = createAndGetId(ben, body);

		assertThat(jdbc.queryForObject("select user_id::text from journal_entries where id = ?::uuid", String.class, id))
				.isEqualTo(ben.userId());
		mockMvc.perform(get("/api/entries").header("Authorization", bearer(ana)))
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	// ---- other rules through the whole stack

	@Test
	void withoutToken_everyEndpointReturns401() throws Exception {
		Session ana = newUser("Ana");
		String id = createAndGetId(ana, entryJson(TODAY, 5, "CALM", null, ""));

		mockMvc.perform(get("/api/entries")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/entries/" + id)).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/entries/" + id)).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/entries").contentType(MediaType.APPLICATION_JSON)
				.content(entryJson(TODAY, 5, "CALM", null, ""))).andExpect(status().isUnauthorized());
		assertThat(entryRows(id)).isEqualTo(1);
	}

	@Test
	void create_futureDateOrInvalidEnergy_returns400_andStoresNothing() throws Exception {
		Session ana = newUser("Ana");

		createEntry(ana, entryJson(TODAY.plusDays(5), 5, "CALM", null, "")).andExpect(status().isBadRequest());
		createEntry(ana, entryJson(TODAY, 11, "CALM", null, "")).andExpect(status().isBadRequest());

		assertThat(jdbc.queryForObject("select count(*) from journal_entries where user_id = ?::uuid", Integer.class,
				ana.userId())).isZero();
	}

	// ---- helpers

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

	private static String bearer(Session session) {
		return "Bearer " + session.token();
	}

	private static String entryJson(LocalDate date, int energy, String emotion, String notes, String tags) {
		return "{\"date\":\"%s\",\"energy\":%d,\"emotion\":\"%s\",\"notes\":%s,\"tags\":[%s]}".formatted(date, energy,
				emotion, notes == null ? "null" : "\"" + notes + "\"", tags);
	}

	private ResultActions createEntry(Session session, String body) throws Exception {
		return mockMvc.perform(post("/api/entries").header("Authorization", bearer(session))
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String createAndGetId(Session session, String body) throws Exception {
		return JsonPath.read(createEntry(session, body).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private int entryRows(String id) {
		return jdbc.queryForObject("select count(*) from journal_entries where id = ?::uuid", Integer.class, id);
	}

	private int tagRows(String id) {
		return jdbc.queryForObject("select count(*) from journal_entry_tags where entry_id = ?::uuid", Integer.class, id);
	}
}
