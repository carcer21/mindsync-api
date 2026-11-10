package com.carlos.mindsync.application.port.in;

import com.carlos.mindsync.domain.model.JournalStats;

import java.util.UUID;

public interface GetStatsUseCase {

	// Statistics over the last PERIOD_DAYS days (today included) of that user's entries.
	int PERIOD_DAYS = 90;

	JournalStats getStats(UUID userId);
}
