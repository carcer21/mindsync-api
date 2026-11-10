package com.carlos.mindsync.infrastructure.adapter.out.persistence;

import com.carlos.mindsync.domain.model.JournalEntry;

final class JournalEntryMapper {

	private JournalEntryMapper() {
	}

	static JournalEntryEntity toEntity(JournalEntry entry) {
		return new JournalEntryEntity(entry.id(), entry.userId(), entry.date(), entry.energy(),
				entry.emotion(), entry.notes(), entry.tags());
	}

	static JournalEntry toDomain(JournalEntryEntity entity) {
		return new JournalEntry(entity.getId(), entity.getUserId(), entity.getDate(), entity.getEnergy(),
				entity.getEmotion(), entity.getNotes(), entity.getTags());
	}
}
