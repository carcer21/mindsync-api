package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.CreateEntryUseCase;
import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.exception.InvalidJournalEntryException;
import com.carlos.mindsync.domain.exception.JournalEntryNotFoundException;
import com.carlos.mindsync.domain.model.Emotion;
import com.carlos.mindsync.domain.model.JournalEntry;
import com.carlos.mindsync.domain.model.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalEntryServicesTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC);
	private static final UUID OWNER = UUID.randomUUID();
	private static final UUID OTHER = UUID.randomUUID();

	@Mock
	private JournalEntryRepositoryPort entries;

	private CreateEntryService create;
	private ListEntriesService list;
	private GetEntryService get;
	private DeleteEntryService delete;

	@BeforeEach
	void setUp() {
		create = new CreateEntryService(entries, CLOCK);
		list = new ListEntriesService(entries);
		get = new GetEntryService(entries);
		delete = new DeleteEntryService(entries);
	}

	private static CreateEntryUseCase.Command command(LocalDate date) {
		return new CreateEntryUseCase.Command(OWNER, date, 6, Emotion.CALM, "note", Set.of("work"));
	}

	// ---- create

	@Test
	void create_savesEntryOwnedByTheCommandUser() {
		when(entries.save(any())).thenAnswer(i -> i.getArgument(0));

		JournalEntry saved = create.create(command(TODAY));

		assertThat(saved.userId()).isEqualTo(OWNER);
		assertThat(saved.id()).isNotNull();
		verify(entries).save(saved);
	}

	@Test
	void create_allowsTomorrow_forTimeZoneTolerance() {
		when(entries.save(any())).thenAnswer(i -> i.getArgument(0));

		assertThat(create.create(command(TODAY.plusDays(1))).date()).isEqualTo(TODAY.plusDays(1));
	}

	@Test
	void create_rejectsDatesBeyondTomorrow_andDoesNotSave() {
		assertThatThrownBy(() -> create.create(command(TODAY.plusDays(2))))
				.isInstanceOf(InvalidJournalEntryException.class)
				.hasMessageContaining("future");
		verify(entries, never()).save(any());
	}

	@Test
	void create_invalidDomainData_isRejectedAndNotSaved() {
		var bad = new CreateEntryUseCase.Command(OWNER, TODAY, 11, Emotion.CALM, null, null);

		assertThatThrownBy(() -> create.create(bad)).isInstanceOf(InvalidJournalEntryException.class);
		verify(entries, never()).save(any());
	}

	@Test
	void command_toString_hidesNotes() {
		assertThat(command(TODAY).toString()).doesNotContain("note").doesNotContain("CALM");
	}

	// ---- list

	@ParameterizedTest
	@CsvSource({ "0,10,0,10", "2,20,2,20", "-5,10,0,10", "0,0,0,1", "0,-3,0,1", "0,500,0,50", "1,50,1,50" })
	void list_clampsPaging(int page, int size, int expectedPage, int expectedSize) {
		Page<JournalEntry> result = new Page<>(List.of(), expectedPage, expectedSize, 0);
		when(entries.findByUserId(OWNER, expectedPage, expectedSize)).thenReturn(result);

		assertThat(list.list(OWNER, page, size)).isSameAs(result);
	}

	@Test
	void page_totalPages() {
		assertThat(new Page<String>(List.of(), 0, 10, 0).totalPages()).isZero();
		assertThat(new Page<String>(List.of(), 0, 10, 10).totalPages()).isEqualTo(1);
		assertThat(new Page<String>(List.of(), 0, 10, 11).totalPages()).isEqualTo(2);
	}

	// ---- get

	@Test
	void get_returnsEntryOfTheOwner() {
		JournalEntry entry = JournalEntry.create(OWNER, TODAY, 5, Emotion.JOY, null, null);
		when(entries.findByIdAndUserId(entry.id(), OWNER)).thenReturn(Optional.of(entry));

		assertThat(get.get(OWNER, entry.id())).isEqualTo(entry);
	}

	@Test
	void get_entryOfAnotherUser_looksExactlyLikeMissing() {
		UUID id = UUID.randomUUID();
		when(entries.findByIdAndUserId(id, OTHER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> get.get(OTHER, id))
				.isInstanceOf(JournalEntryNotFoundException.class)
				.hasMessage("journal entry not found");
	}

	// ---- delete

	@Test
	void delete_ownEntry_succeeds() {
		UUID id = UUID.randomUUID();
		when(entries.deleteByIdAndUserId(id, OWNER)).thenReturn(true);

		delete.delete(OWNER, id);

		verify(entries).deleteByIdAndUserId(id, OWNER);
	}

	@Test
	void delete_nothingDeleted_throwsNotFound() {
		UUID id = UUID.randomUUID();
		when(entries.deleteByIdAndUserId(id, OTHER)).thenReturn(false);

		assertThatThrownBy(() -> delete.delete(OTHER, id)).isInstanceOf(JournalEntryNotFoundException.class);
	}
}
