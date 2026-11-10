package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.ListEntriesUseCase;
import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.model.JournalEntry;
import com.carlos.mindsync.domain.model.Page;

import java.util.UUID;

public class ListEntriesService implements ListEntriesUseCase {

	static final int MAX_PAGE_SIZE = 50;

	private final JournalEntryRepositoryPort entries;

	public ListEntriesService(JournalEntryRepositoryPort entries) {
		this.entries = entries;
	}

	// Out-of-range paging values are clamped, so a client can't request a huge page.
	@Override
	public Page<JournalEntry> list(UUID userId, int page, int size) {
		return entries.findByUserId(userId, Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
	}
}
