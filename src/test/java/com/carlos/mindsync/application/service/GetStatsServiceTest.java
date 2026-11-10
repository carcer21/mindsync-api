package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.model.Emotion;
import com.carlos.mindsync.domain.model.JournalEntry;
import com.carlos.mindsync.domain.model.JournalStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStatsServiceTest {

	private static final UUID ANA = UUID.randomUUID();
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T23:30:00Z"), ZoneOffset.UTC);

	@Mock
	private JournalEntryRepositoryPort entries;

	@Test
	void getStats_asksForTheLast90DaysOfThatUser_withASafetyCap() {
		when(entries.findByUserIdSince(eq(ANA), any(), eq(1000))).thenReturn(List.of());

		new GetStatsService(entries, CLOCK).getStats(ANA);

		ArgumentCaptor<LocalDate> since = ArgumentCaptor.forClass(LocalDate.class);
		verify(entries).findByUserIdSince(eq(ANA), since.capture(), eq(1000));
		// The window is exactly 90 days long, counting today.
		assertThat(ChronoUnit.DAYS.between(since.getValue(), TODAY) + 1).isEqualTo(90);
		assertThat(since.getValue()).isEqualTo(LocalDate.of(2025, 12, 11));
	}

	@Test
	void getStats_computesFromTheReturnedEntries_usingTodayFromTheClock() {
		List<JournalEntry> found = List.of(
				JournalEntry.create(ANA, TODAY, 8, Emotion.JOY, null, null),
				JournalEntry.create(ANA, TODAY.minusDays(1), 4, Emotion.JOY, null, null),
				JournalEntry.create(ANA, TODAY.minusDays(5), 6, Emotion.STRESS, null, null));
		when(entries.findByUserIdSince(ANA, TODAY.minusDays(89), 1000)).thenReturn(found);

		JournalStats stats = new GetStatsService(entries, CLOCK).getStats(ANA);

		assertThat(stats.currentStreak()).isEqualTo(2);
		assertThat(stats.averageEnergy()).isEqualTo(6.0);
		assertThat(stats.predominantEmotion()).isEqualTo(Emotion.JOY);
		assertThat(stats.entriesThisMonth()).isEqualTo(3);
	}

	@Test
	void getStats_userWithoutEntries_returnsEmptyStats() {
		when(entries.findByUserIdSince(ANA, TODAY.minusDays(89), 1000)).thenReturn(List.of());

		JournalStats stats = new GetStatsService(entries, CLOCK).getStats(ANA);

		assertThat(stats).isEqualTo(new JournalStats(0, null, null, 0));
	}
}
