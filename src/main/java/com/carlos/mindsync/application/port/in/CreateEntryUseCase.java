package com.carlos.mindsync.application.port.in;

import com.carlos.mindsync.domain.model.Emotion;
import com.carlos.mindsync.domain.model.JournalEntry;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public interface CreateEntryUseCase {

	JournalEntry create(Command command);

	record Command(UUID userId, LocalDate date, int energy, Emotion emotion, String notes,
			Set<String> tags) {

		// Notes are health data: keep them out of logs.
		@Override
		public String toString() {
			return "Command[userId=" + userId + ", date=" + date + "]";
		}
	}
}
