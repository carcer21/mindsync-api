package com.carlos.mindsync.infrastructure.adapter.out.persistence;

import com.carlos.mindsync.domain.model.Emotion;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
class JournalEntryEntity {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "entry_date", nullable = false)
	private LocalDate date;

	@Column(nullable = false)
	private int energy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Emotion emotion;

	@Column(length = 2000)
	private String notes;

	// Eager because open-in-view is off; batched so a page of entries loads its tags in one query.
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "journal_entry_tags", joinColumns = @JoinColumn(name = "entry_id"))
	@Column(name = "tag", nullable = false, length = 30)
	@BatchSize(size = 50)
	private Set<String> tags = new HashSet<>();

	protected JournalEntryEntity() {
		// required by JPA
	}

	JournalEntryEntity(UUID id, UUID userId, LocalDate date, int energy, Emotion emotion, String notes,
			Set<String> tags) {
		this.id = id;
		this.userId = userId;
		this.date = date;
		this.energy = energy;
		this.emotion = emotion;
		this.notes = notes;
		this.tags = new HashSet<>(tags);
	}

	UUID getId() {
		return id;
	}

	UUID getUserId() {
		return userId;
	}

	LocalDate getDate() {
		return date;
	}

	int getEnergy() {
		return energy;
	}

	Emotion getEmotion() {
		return emotion;
	}

	String getNotes() {
		return notes;
	}

	Set<String> getTags() {
		return tags;
	}
}
