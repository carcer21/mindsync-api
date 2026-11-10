package com.carlos.mindsync.infrastructure.adapter.out.persistence;

import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.model.JournalEntry;
import com.carlos.mindsync.domain.model.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class JournalEntryPersistenceAdapter implements JournalEntryRepositoryPort {

	// The id is a tie-breaker so paging is stable when several entries share a date.
	private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"));

	private final SpringDataJournalEntryRepository repository;

	JournalEntryPersistenceAdapter(SpringDataJournalEntryRepository repository) {
		this.repository = repository;
	}

	@Override
	public JournalEntry save(JournalEntry entry) {
		return JournalEntryMapper.toDomain(repository.saveAndFlush(JournalEntryMapper.toEntity(entry)));
	}

	@Override
	public Optional<JournalEntry> findByIdAndUserId(UUID id, UUID userId) {
		return repository.findByIdAndUserId(id, userId).map(JournalEntryMapper::toDomain);
	}

	@Override
	public Page<JournalEntry> findByUserId(UUID userId, int page, int size) {
		var result = repository.findByUserId(userId, PageRequest.of(page, size, NEWEST_FIRST));
		return new Page<>(result.map(JournalEntryMapper::toDomain).getContent(), page, size,
				result.getTotalElements());
	}

	@Override
	public boolean deleteByIdAndUserId(UUID id, UUID userId) {
		return repository.deleteByIdAndUserId(id, userId) > 0;
	}
}
