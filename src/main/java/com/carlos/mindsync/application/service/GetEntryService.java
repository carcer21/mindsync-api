package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.GetEntryUseCase;
import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.exception.JournalEntryNotFoundException;
import com.carlos.mindsync.domain.model.JournalEntry;

import java.util.UUID;

public class GetEntryService implements GetEntryUseCase {

	private final JournalEntryRepositoryPort entries;

	public GetEntryService(JournalEntryRepositoryPort entries) {
		this.entries = entries;
	}

	@Override
	public JournalEntry get(UUID userId, UUID entryId) {
		return entries.findByIdAndUserId(entryId, userId).orElseThrow(JournalEntryNotFoundException::new);
	}
}
