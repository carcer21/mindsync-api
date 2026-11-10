package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.out.AiAdvisorPort;
import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.model.Emotion;
import com.carlos.mindsync.domain.model.JournalEntry;
import com.carlos.mindsync.domain.model.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// The AI port is a mock: no test here (or anywhere) calls Gemini.
@ExtendWith(MockitoExtension.class)
class GetAdviceServiceTest {

	private static final UUID ANA = UUID.randomUUID();

	@Mock
	private JournalEntryRepositoryPort entries;
	@Mock
	private AiAdvisorPort advisor;
	@InjectMocks
	private GetAdviceService service;

	private static JournalEntry entry() {
		return JournalEntry.create(ANA, LocalDate.of(2026, 3, 10), 5, Emotion.CALM, null, null);
	}

	@Test
	void getAdvice_asksForTheLatest14EntriesOfThatUser_andReturnsTheAdvice() {
		List<JournalEntry> latest = List.of(entry(), entry());
		when(entries.findByUserId(ANA, 0, 14)).thenReturn(new Page<>(latest, 0, 14, 2));
		when(advisor.getAdvice(latest)).thenReturn("some advice");

		assertThat(service.getAdvice(ANA)).isEqualTo("some advice");

		verify(entries).findByUserId(ANA, 0, 14);
		verify(advisor).getAdvice(latest);
	}

	@Test
	void getAdvice_withoutEntries_doesNotCallTheAi() {
		when(entries.findByUserId(ANA, 0, 14)).thenReturn(new Page<>(List.of(), 0, 14, 0));

		assertThat(service.getAdvice(ANA)).isEqualTo(GetAdviceService.NO_ENTRIES_MESSAGE);

		verifyNoInteractions(advisor);
	}

	@Test
	void getAdvice_neverSendsEntriesOfAnotherUser() {
		UUID ben = UUID.randomUUID();
		when(entries.findByUserId(ben, 0, 14)).thenReturn(new Page<>(List.of(), 0, 14, 0));

		service.getAdvice(ben);

		verify(entries).findByUserId(ben, 0, 14);
		verifyNoInteractions(advisor);
	}

	@Test
	void getAdvice_databaseFailure_propagates_andTheAiIsNotCalled() {
		when(entries.findByUserId(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
				.thenThrow(new IllegalStateException("db down"));

		assertThatThrownBy(() -> service.getAdvice(ANA)).isInstanceOf(IllegalStateException.class);

		verifyNoInteractions(advisor);
	}
}
