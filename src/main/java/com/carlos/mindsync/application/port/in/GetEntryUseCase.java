package com.carlos.mindsync.application.port.in;

import com.carlos.mindsync.domain.model.JournalEntry;

import java.util.UUID;

public interface GetEntryUseCase {

	JournalEntry get(UUID userId, UUID entryId);
}
