package com.carlos.mindsync.application.port.out;

import com.carlos.mindsync.domain.model.JournalEntry;
import com.carlos.mindsync.domain.model.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Every read/delete takes the owner id, so ownership is enforced in the query itself.
public interface JournalEntryRepositoryPort {

	JournalEntry save(JournalEntry entry);

	Optional<JournalEntry> findByIdAndUserId(UUID id, UUID userId);

	// Newest date first.
	Page<JournalEntry> findByUserId(UUID userId, int page, int size);

	// Entries dated on or after "since", newest first, at most maxResults.
	List<JournalEntry> findByUserIdSince(UUID userId, LocalDate since, int maxResults);

	// Returns true if something was deleted.
	boolean deleteByIdAndUserId(UUID id, UUID userId);
}
