package com.carlos.mindsync.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataJournalEntryRepository extends JpaRepository<JournalEntryEntity, UUID> {

	Optional<JournalEntryEntity> findByIdAndUserId(UUID id, UUID userId);

	Page<JournalEntryEntity> findByUserId(UUID userId, Pageable pageable);

	List<JournalEntryEntity> findByUserIdAndDateGreaterThanEqual(UUID userId, LocalDate since, Pageable pageable);

	@Transactional
	long deleteByIdAndUserId(UUID id, UUID userId);
}
