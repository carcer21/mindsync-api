package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.CreateEntryUseCase;
import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.exception.InvalidJournalEntryException;
import com.carlos.mindsync.domain.model.JournalEntry;

import java.time.Clock;
import java.time.LocalDate;

public class CreateEntryService implements CreateEntryUseCase {

	private final JournalEntryRepositoryPort entries;
	private final Clock clock;

	public CreateEntryService(JournalEntryRepositoryPort entries, Clock clock) {
		this.entries = entries;
		this.clock = clock;
	}

	@Override
	public JournalEntry create(Command command) {
		JournalEntry entry = JournalEntry.create(command.userId(), command.date(), command.energy(),
				command.emotion(), command.notes(), command.tags());
		// One day of tolerance for users in time zones ahead of the server.
		if (entry.date().isAfter(LocalDate.now(clock).plusDays(1))) {
			throw new InvalidJournalEntryException("date must not be in the future");
		}
		return entries.save(entry);
	}
}
