package com.carlos.mindsync.domain.model;

import com.carlos.mindsync.domain.exception.InvalidJournalEntryException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record JournalEntry(UUID id, UUID userId, LocalDate date, int energy, String emotion,
		String notes, Set<String> tags) {

	static final int ENERGY_MIN = 1;
	static final int ENERGY_MAX = 10;
	static final int EMOTION_MAX_LENGTH = 50;
	static final int NOTES_MAX_LENGTH = 2000;
	static final int MAX_TAGS = 10;
	static final int TAG_MAX_LENGTH = 30;

	public JournalEntry {
		if (id == null) {
			throw new InvalidJournalEntryException("id is required");
		}
		if (userId == null) {
			throw new InvalidJournalEntryException("userId is required");
		}
		if (date == null) {
			throw new InvalidJournalEntryException("date is required");
		}
		if (energy < ENERGY_MIN || energy > ENERGY_MAX) {
			throw new InvalidJournalEntryException(
					"energy must be between " + ENERGY_MIN + " and " + ENERGY_MAX);
		}
		if (emotion == null || emotion.isBlank()) {
			throw new InvalidJournalEntryException("emotion is required");
		}
		emotion = emotion.trim();
		if (emotion.length() > EMOTION_MAX_LENGTH) {
			throw new InvalidJournalEntryException(
					"emotion must be at most " + EMOTION_MAX_LENGTH + " characters");
		}
		// Notes are optional; blank means "no notes".
		notes = (notes == null || notes.isBlank()) ? null : notes.strip();
		if (notes != null && notes.length() > NOTES_MAX_LENGTH) {
			throw new InvalidJournalEntryException(
					"notes must be at most " + NOTES_MAX_LENGTH + " characters");
		}
		tags = normalizeTags(tags);
	}

	public static JournalEntry create(UUID userId, LocalDate date, int energy, String emotion,
			String notes, Collection<String> tags) {
		return new JournalEntry(UUID.randomUUID(), userId, date, energy, emotion, notes,
				tags == null ? null : new LinkedHashSet<>(tags));
	}

	private static Set<String> normalizeTags(Set<String> raw) {
		if (raw == null) {
			return Set.of();
		}
		Set<String> normalized = new LinkedHashSet<>();
		for (String tag : raw) {
			if (tag == null || tag.isBlank()) {
				throw new InvalidJournalEntryException("tags must not be blank");
			}
			String clean = tag.trim().toLowerCase(Locale.ROOT);
			if (clean.length() > TAG_MAX_LENGTH) {
				throw new InvalidJournalEntryException(
						"each tag must be at most " + TAG_MAX_LENGTH + " characters");
			}
			normalized.add(clean);
		}
		if (normalized.size() > MAX_TAGS) {
			throw new InvalidJournalEntryException("at most " + MAX_TAGS + " tags are allowed");
		}
		return Set.copyOf(normalized);
	}

	// Notes are health data: keep them out of logs.
	@Override
	public String toString() {
		return "JournalEntry[id=" + id + ", date=" + date + "]";
	}
}
