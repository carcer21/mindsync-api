package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.domain.model.Emotion;
import com.carlos.mindsync.domain.model.JournalEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record EntryResponse(UUID id, LocalDate date, int energy, Emotion emotion, String notes, List<String> tags) {

	static EntryResponse from(JournalEntry entry) {
		return new EntryResponse(entry.id(), entry.date(), entry.energy(), entry.emotion(), entry.notes(),
				entry.tags().stream().sorted().toList());
	}
}
