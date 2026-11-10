package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.DeleteEntryUseCase;
import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.exception.JournalEntryNotFoundException;

import java.util.UUID;

public class DeleteEntryService implements DeleteEntryUseCase {

	private final JournalEntryRepositoryPort entries;

	public DeleteEntryService(JournalEntryRepositoryPort entries) {
		this.entries = entries;
	}

	@Override
	public void delete(UUID userId, UUID entryId) {
		if (!entries.deleteByIdAndUserId(entryId, userId)) {
			throw new JournalEntryNotFoundException();
		}
	}
}
