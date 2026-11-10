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
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

	private static final String PASSWORD = "s3cret-Passw0rd";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void register_login_me_fullFlow() throws Exception {
		String email = uniqueEmail();

		String id = JsonPath.read(register("Ana", email, PASSWORD).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");

		String token = loginAndGetToken(email, PASSWORD);

		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.name").value("Ana"))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void noResponseContainsThePasswordOrItsHash() throws Exception {
		String email = uniqueEmail();

		String registerBody = register("Ana", email, PASSWORD).andReturn().getResponse().getContentAsString();
		String loginBody = login(email, PASSWORD).andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(loginBody, "$.accessToken");
		String meBody = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
				.andReturn().getResponse().getContentAsString();
		String storedHash = jdbc.queryForObject("select password_hash from users where email = ?", String.class, email);

		assertThat(registerBody + loginBody + meBody).doesNotContain(PASSWORD).doesNotContain(storedHash);
	}

	@Test
	void passwordIsStoredAsBcryptHash_notAsPlainText() {
		String email = uniqueEmail();
		try {
			register("Ana", email, PASSWORD);
		} catch (Exception e) {
			throw new AssertionError(e);
		}

		String storedHash = jdbc.queryForObject("select password_hash from users where email = ?", String.class, email);

		assertThat(storedHash).isNotEqualTo(PASSWORD).startsWith("$2").hasSize(60);
	}

	@Test
	void eachUserSeesOnlyTheirOwnData() throws Exception {
		String anaEmail = uniqueEmail();
		String benEmail = uniqueEmail();
		register("Ana", anaEmail, PASSWORD);
		register("Ben", benEmail, PASSWORD);

		String anaToken = loginAndGetToken(anaEmail, PASSWORD);
		String benToken = loginAndGetToken(benEmail, PASSWORD);

		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + anaToken))
				.andExpect(jsonPath("$.email").value(anaEmail));
		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + benToken))
				.andExpect(jsonPath("$.email").value(benEmail));
	}

	@Test
	void register_withExistingEmail_returns409_evenWithDifferentCase() throws Exception {
		String email = uniqueEmail();
		register("Ana", email, PASSWORD).andExpect(status().isCreated());

		register("Impostor", email.toUpperCase(), PASSWORD).andExpect(status().isConflict());

		assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Integer.class, email)).isEqualTo(1);
	}

	@Test
	void login_withWrongPasswordOrUnknownEmail_returnsTheSame401() throws Exception {
		String email = uniqueEmail();
		register("Ana", email, PASSWORD);

		String wrongPassword = login(email, "wrong-password-1").andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		String unknownEmail = login(uniqueEmail(), PASSWORD).andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(wrongPassword, "$.detail")).isEqualTo(JsonPath.<String>read(unknownEmail, "$.detail"));
	}

	@Test
	void login_isCaseInsensitiveOnEmail() throws Exception {
		String email = uniqueEmail();
		register("Ana", email, PASSWORD);

		login(email.toUpperCase(), PASSWORD).andExpect(status().isOk());
	}

	@Test
	void me_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
	}

	private ResultActions register(String name, String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
	}

	private String loginAndGetToken(String email, String password) throws Exception {
		return JsonPath.read(login(email, password).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(), "$.accessToken");
	}

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}
}
