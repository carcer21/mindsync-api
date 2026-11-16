package com.carlos.mindsync.application.port.in;

import com.carlos.mindsync.domain.model.JournalEntry;

import java.util.UUID;

public interface UpdateEntryUseCase {

	// Replaces every field of the entry. The data is the same as when creating one, so the command is reused.
	JournalEntry update(UUID entryId, CreateEntryUseCase.Command data);
}
