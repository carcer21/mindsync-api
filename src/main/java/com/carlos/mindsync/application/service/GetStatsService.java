package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.GetStatsUseCase;
import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.model.JournalStats;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

public class GetStatsService implements GetStatsUseCase {

	// Safety cap on the rows loaded in memory for one request.
	static final int MAX_ENTRIES = 1000;

	private final JournalEntryRepositoryPort entries;
	private final Clock clock;

	public GetStatsService(JournalEntryRepositoryPort entries, Clock clock) {
		this.entries = entries;
		this.clock = clock;
	}

	@Override
	public JournalStats getStats(UUID userId) {
		LocalDate today = LocalDate.now(clock);
		LocalDate since = today.minusDays(PERIOD_DAYS - 1L);
		return JournalStats.of(entries.findByUserIdSince(userId, since, MAX_ENTRIES), today);
	}
}
