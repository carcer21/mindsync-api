package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.CreateEntryUseCase;
import com.carlos.mindsync.application.port.in.UpdateEntryUseCase;
import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.exception.JournalEntryNotFoundException;
import com.carlos.mindsync.domain.model.JournalEntry;

import java.time.Clock;
import java.util.UUID;

public class UpdateEntryService implements UpdateEntryUseCase {

	private final JournalEntryRepositoryPort entries;
	private final Clock clock;

	public UpdateEntryService(JournalEntryRepositoryPort entries, Clock clock) {
		this.entries = entries;
		this.clock = clock;
	}

	@Override
	public JournalEntry update(UUID entryId, CreateEntryUseCase.Command data) {
		// save() overwrites whichever row has this id, so the ownership check must come first.
		// Without it, a user could take over someone else's entry.
		if (entries.findByIdAndUserId(entryId, data.userId()).isEmpty()) {
			throw new JournalEntryNotFoundException();
		}
		// A new object with the same id: the domain validation runs again.
		JournalEntry entry = new JournalEntry(entryId, data.userId(), data.date(), data.energy(),
				data.emotion(), data.notes(), data.tags());
		CreateEntryService.requireNotInTheFuture(entry, clock);
		return entries.save(entry);
	}
}
