package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.CreateEntryUseCase;
import com.carlos.mindsync.application.port.in.DeleteEntryUseCase;
import com.carlos.mindsync.application.port.in.GetEntryUseCase;
import com.carlos.mindsync.application.port.in.ListEntriesUseCase;
import com.carlos.mindsync.application.port.in.LoginUseCase;
import com.carlos.mindsync.application.port.in.RegisterUserUseCase;
import com.carlos.mindsync.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// One error contract for the whole API: every error is application/problem+json.
@WebMvcTest({AuthController.class, JournalEntryController.class})
@Import(SecurityConfig.class)
class ErrorResponsesTest {

	private static final String SECRET_PASSWORD = "pw-1234";
	private static final String PRIVATE_NOTE = "my private note";

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private RegisterUserUseCase registerUser;
	@MockitoBean
	private LoginUseCase login;
	@MockitoBean
	private CreateEntryUseCase createEntry;
	@MockitoBean
	private ListEntriesUseCase listEntries;
	@MockitoBean
	private GetEntryUseCase getEntry;
	@MockitoBean
	private DeleteEntryUseCase deleteEntry;
	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static RequestPostProcessor asUser() {
		return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()));
	}

	// ---- validation errors list every failing field

	@Test
	void validation_listsEachFailingField_withoutEchoingRejectedValues() throws Exception {
		String body = """
				{"name":"  ","email":"not-an-email","password":"%s"}""".formatted(SECRET_PASSWORD);

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("request validation failed"))
				.andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("name", "email", "password")))
				.andExpect(jsonPath("$.errors[*].message").isNotEmpty())
				.andExpect(content().string(not(containsString(SECRET_PASSWORD))))
				.andExpect(content().string(not(containsString("not-an-email"))));

		verifyNoInteractions(registerUser);
	}

	@Test
	void validation_nestedCollectionElementsReportTheirIndex() throws Exception {
		String body = """
				{"date":"2026-03-10","energy":6,"emotion":"CALM","notes":"%s","tags":["ok","  "]}""".formatted(PRIVATE_NOTE);

		mockMvc.perform(post("/api/entries").contentType(MediaType.APPLICATION_JSON).content(body).with(asUser()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.length()").value(1))
				.andExpect(jsonPath("$.errors[0].field").value(containsString("tags")))
				.andExpect(content().string(not(containsString(PRIVATE_NOTE))));
	}

	@Test
	void validation_errorsAreSortedByField_soTheResponseIsStable() throws Exception {
		String body = "{\"date\":null,\"energy\":99,\"emotion\":null}";

		mockMvc.perform(post("/api/entries").contentType(MediaType.APPLICATION_JSON).content(body).with(asUser()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("date"))
				.andExpect(jsonPath("$.errors[1].field").value("emotion"))
				.andExpect(jsonPath("$.errors[2].field").value("energy"));
	}

	// ---- malformed input never echoes what the client sent

	@Test
	void malformedJson_returnsProblemDetail_withoutEchoingTheBody() throws Exception {
		mockMvc.perform(post("/api/entries").contentType(MediaType.APPLICATION_JSON)
						.content("{\"notes\":\"" + PRIVATE_NOTE + "\",").with(asUser()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(content().string(not(containsString(PRIVATE_NOTE))));
	}

	@Test
	void unknownEnumValue_isABadRequest_withoutEchoingIt() throws Exception {
		String body = "{\"date\":\"2026-03-10\",\"energy\":6,\"emotion\":\"SECRET_FEELING\"}";

		mockMvc.perform(post("/api/entries").contentType(MediaType.APPLICATION_JSON).content(body).with(asUser()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(content().string(not(containsString("SECRET_FEELING"))));
	}

	@Test
	void malformedPathId_returnsProblemDetail_whoseDetailDoesNotEchoTheValue() throws Exception {
		mockMvc.perform(get("/api/entries/not-a-uuid-<script>").with(asUser()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("invalid value for 'id'"));
	}

	@Test
	void nonNumericQueryParam_returnsProblemDetail_withoutEchoingTheValue() throws Exception {
		mockMvc.perform(get("/api/entries").param("page", "abc-secret").with(asUser()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(content().string(not(containsString("abc-secret"))));
	}

	// ---- other framework errors keep the same shape

	@Test
	void unsupportedMethod_unsupportedMediaType_andUnknownPath_areProblemDetails() throws Exception {
		mockMvc.perform(post("/api/entries/" + UUID.randomUUID()).with(asUser()))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		mockMvc.perform(post("/api/entries").contentType(MediaType.TEXT_PLAIN).content("x").with(asUser()))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		mockMvc.perform(get("/api/does-not-exist").with(asUser()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	// ---- 401 is a ProblemDetail too, and keeps the WWW-Authenticate header

	@Test
	void noToken_returns401ProblemDetail_withBearerChallenge() throws Exception {
		mockMvc.perform(get("/api/entries"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.title").value("Unauthorized"))
				.andExpect(jsonPath("$.detail").value("authentication required"));
	}

	@Test
	void invalidToken_returnsTheSame401Body_withoutDecoderDetails() throws Exception {
		when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("internal decoder detail"));

		mockMvc.perform(get("/api/entries").header("Authorization", "Bearer garbage"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("authentication required"))
				.andExpect(content().string(not(containsString("internal decoder detail"))));
	}
}
