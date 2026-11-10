package com.carlos.mindsync.application.port.in;

import com.carlos.mindsync.domain.model.JournalEntry;
import com.carlos.mindsync.domain.model.Page;

import java.util.UUID;

public interface ListEntriesUseCase {

	Page<JournalEntry> list(UUID userId, int page, int size);
}
