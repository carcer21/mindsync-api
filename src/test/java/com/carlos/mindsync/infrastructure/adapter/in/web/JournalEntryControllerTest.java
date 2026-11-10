package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.CreateEntryUseCase;
import com.carlos.mindsync.application.port.in.DeleteEntryUseCase;
import com.carlos.mindsync.application.port.in.GetEntryUseCase;
import com.carlos.mindsync.application.port.in.ListEntriesUseCase;
import com.carlos.mindsync.domain.exception.InvalidJournalEntryException;
import com.carlos.mindsync.domain.exception.JournalEntryNotFoundException;
import com.carlos.mindsync.domain.model.Emotion;
import com.carlos.mindsync.domain.model.JournalEntry;
import com.carlos.mindsync.domain.model.Page;
import com.carlos.mindsync.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JournalEntryController.class)
@Import(SecurityConfig.class)
class JournalEntryControllerTest {

	private static final UUID ANA = UUID.randomUUID();
	private static final UUID SOMEONE_ELSE = UUID.randomUUID();
	private static final LocalDate DAY = LocalDate.of(2026, 3, 10);
	private static final JournalEntry ENTRY =
			JournalEntry.create(ANA, DAY, 6, Emotion.CALM, "invented note", List.of("work", "family"));
	private static final String VALID_BODY = """
			{"date":"2026-03-10","energy":6,"emotion":"CALM","notes":"invented note","tags":["work","family"]}""";

	@Autowired
	private MockMvc mockMvc;
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

	private static RequestPostProcessor asAna() {
		return jwt().jwt(token -> token.subject(ANA.toString()));
	}

	private static MockHttpServletRequestBuilder postEntry(String body) {
		return post("/api/entries").contentType(MediaType.APPLICATION_JSON).content(body);
	}

	// ---- authentication

	@Test
	void everyEndpoint_withoutToken_returns401() throws Exception {
		mockMvc.perform(postEntry(VALID_BODY)).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/entries")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/entries/" + ENTRY.id())).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/entries/" + ENTRY.id())).andExpect(status().isUnauthorized());

		verifyNoInteractions(createEntry, listEntries, getEntry, deleteEntry);
	}

	// ---- create

	@Test
	void create_returns201WithLocationAndBody_withoutOwnerId() throws Exception {
		when(createEntry.create(any())).thenReturn(ENTRY);

		mockMvc.perform(postEntry(VALID_BODY).with(asAna()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/entries/" + ENTRY.id()))
				.andExpect(jsonPath("$.id").value(ENTRY.id().toString()))
				.andExpect(jsonPath("$.date").value("2026-03-10"))
				.andExpect(jsonPath("$.energy").value(6))
				.andExpect(jsonPath("$.emotion").value("CALM"))
				.andExpect(jsonPath("$.notes").value("invented note"))
				.andExpect(jsonPath("$.tags[0]").value("family"))
				.andExpect(jsonPath("$.tags[1]").value("work"))
				.andExpect(jsonPath("$.userId").doesNotExist());
	}

	@Test
	void create_takesTheOwnerFromTheToken_andIgnoresOwnerFieldsInTheBody() throws Exception {
		when(createEntry.create(any())).thenReturn(ENTRY);
		String body = """
				{"userId":"%s","id":"%s","date":"2026-03-10","energy":6,"emotion":"CALM"}"""
				.formatted(SOMEONE_ELSE, SOMEONE_ELSE);

		mockMvc.perform(postEntry(body).param("userId", SOMEONE_ELSE.toString())
						.header("X-User-Id", SOMEONE_ELSE.toString()).with(asAna()))
				.andExpect(status().isCreated());

		ArgumentCaptor<CreateEntryUseCase.Command> command = ArgumentCaptor.forClass(CreateEntryUseCase.Command.class);
		verify(createEntry).create(command.capture());
		assertThat(command.getValue().userId()).isEqualTo(ANA);
		assertThat(command.getValue().notes()).isNull();
		assertThat(command.getValue().tags()).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"energy\":6,\"emotion\":\"CALM\"}",
			"{\"date\":\"2026-03-10\",\"emotion\":\"CALM\"}",
			"{\"date\":\"2026-03-10\",\"energy\":6}",
			"{\"date\":\"2026-03-10\",\"energy\":0,\"emotion\":\"CALM\"}",
			"{\"date\":\"2026-03-10\",\"energy\":11,\"emotion\":\"CALM\"}",
			"{\"date\":\"2026-03-10\",\"energy\":\"high\",\"emotion\":\"CALM\"}",
			"{\"date\":\"2026-03-10\",\"energy\":6,\"emotion\":\"HUNGRY\"}",
			"{\"date\":\"10/03/2026\",\"energy\":6,\"emotion\":\"CALM\"}",
			"{\"date\":\"2026-03-10\",\"energy\":6,\"emotion\":\"CALM\",\"tags\":[\"  \"]}",
			"{\"date\":\"2026-03-10\",\"energy\":6,\"emotion\":\"CALM\",\"tags\":[\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"]}",
			"{\"date\":\"2026-03-10\",\"energy\":6,\"emotion\":\"CALM\",\"tags\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\",\"10\",\"11\"]}",
			"{not json",
			"" })
	void create_invalidBody_returns400_andUseCaseIsNotCalled(String body) throws Exception {
		mockMvc.perform(postEntry(body).with(asAna())).andExpect(status().isBadRequest());

		verifyNoInteractions(createEntry);
	}

	@Test
	void create_notesTooLong_returns400() throws Exception {
		String body = "{\"date\":\"2026-03-10\",\"energy\":6,\"emotion\":\"CALM\",\"notes\":\"" + "a".repeat(2001) + "\"}";

		mockMvc.perform(postEntry(body).with(asAna())).andExpect(status().isBadRequest());

		verifyNoInteractions(createEntry);
	}

	@Test
	void create_domainRejection_returns400WithItsMessage() throws Exception {
		when(createEntry.create(any())).thenThrow(new InvalidJournalEntryException("date must not be in the future"));

		mockMvc.perform(postEntry(VALID_BODY).with(asAna()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("date must not be in the future"));
	}

	@Test
	void create_unexpectedError_returnsGeneric500_withoutEchoingTheNotes() throws Exception {
		when(createEntry.create(any())).thenThrow(new IllegalStateException("db down: invented note"));

		mockMvc.perform(postEntry(VALID_BODY).with(asAna()))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value("unexpected error"))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("invented note"))));
	}

	// ---- list

	@Test
	void list_usesTokenOwnerAndDefaultPaging() throws Exception {
		when(listEntries.list(ANA, 0, 20)).thenReturn(new Page<>(List.of(ENTRY), 0, 20, 1));

		mockMvc.perform(get("/api/entries").param("userId", SOMEONE_ELSE.toString()).with(asAna()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].id").value(ENTRY.id().toString()))
				.andExpect(jsonPath("$.content[0].userId").doesNotExist())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));

		verify(listEntries).list(ANA, 0, 20);
	}

	@Test
	void list_passesRequestedPaging() throws Exception {
		when(listEntries.list(ANA, 2, 5)).thenReturn(new Page<>(List.of(), 2, 5, 7));

		mockMvc.perform(get("/api/entries").param("page", "2").param("size", "5").with(asAna()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(0))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void list_nonNumericPaging_returns400() throws Exception {
		mockMvc.perform(get("/api/entries").param("page", "abc").with(asAna())).andExpect(status().isBadRequest());

		verify(listEntries, org.mockito.Mockito.never()).list(any(), anyInt(), anyInt());
	}

	// ---- get

	@Test
	void get_returnsTheEntry_askingWithTheTokenOwner() throws Exception {
		when(getEntry.get(ANA, ENTRY.id())).thenReturn(ENTRY);

		mockMvc.perform(get("/api/entries/" + ENTRY.id()).param("userId", SOMEONE_ELSE.toString()).with(asAna()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ENTRY.id().toString()))
				.andExpect(jsonPath("$.userId").doesNotExist());

		verify(getEntry).get(ANA, ENTRY.id());
	}

	@Test
	void get_missingOrForeignEntry_returns404() throws Exception {
		when(getEntry.get(ANA, ENTRY.id())).thenThrow(new JournalEntryNotFoundException());

		mockMvc.perform(get("/api/entries/" + ENTRY.id()).with(asAna()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("journal entry not found"));
	}

	@Test
	void get_malformedId_returns400() throws Exception {
		mockMvc.perform(get("/api/entries/not-a-uuid").with(asAna())).andExpect(status().isBadRequest());

		verifyNoInteractions(getEntry);
	}

	// ---- delete

	@Test
	void delete_returns204_deletingWithTheTokenOwner() throws Exception {
		mockMvc.perform(delete("/api/entries/" + ENTRY.id()).param("userId", SOMEONE_ELSE.toString()).with(asAna()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(deleteEntry).delete(ANA, ENTRY.id());
	}

	@Test
	void delete_missingOrForeignEntry_returns404() throws Exception {
		doThrow(new JournalEntryNotFoundException()).when(deleteEntry).delete(ANA, ENTRY.id());

		mockMvc.perform(delete("/api/entries/" + ENTRY.id()).with(asAna())).andExpect(status().isNotFound());
	}

	@Test
	void requestToString_hidesNotes() {
		assertThat(new EntryRequest(DAY, 6, Emotion.SADNESS, "invented note", null).toString())
				.doesNotContain("invented note").doesNotContain("SADNESS");
	}
}
