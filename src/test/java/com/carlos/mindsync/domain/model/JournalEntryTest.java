package com.carlos.mindsync.domain.model;

import com.carlos.mindsync.domain.exception.InvalidJournalEntryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryTest {

	private static final UUID USER = UUID.randomUUID();
	private static final LocalDate DATE = LocalDate.of(2026, 1, 15);

	private static JournalEntry entry(int energy, Emotion emotion, String notes, List<String> tags) {
		return JournalEntry.create(USER, DATE, energy, emotion, notes, tags);
	}

	@Test
	void create_validData_generatesIdAndKeepsFields() {
		JournalEntry e = entry(7, Emotion.CALM, "good day", List.of("work"));

		assertThat(e.id()).isNotNull();
		assertThat(e.userId()).isEqualTo(USER);
		assertThat(e.date()).isEqualTo(DATE);
		assertThat(e.energy()).isEqualTo(7);
		assertThat(e.emotion()).isEqualTo(Emotion.CALM);
		assertThat(e.notes()).isEqualTo("good day");
		assertThat(e.tags()).containsExactly("work");
	}

	@Test
	void create_twice_generatesDifferentIds() {
		assertThat(entry(5, Emotion.CALM, null, null).id()).isNotEqualTo(entry(5, Emotion.CALM, null, null).id());
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 10 })
	void energy_boundaries_areAccepted(int energy) {
		assertThat(entry(energy, Emotion.CALM, null, null).energy()).isEqualTo(energy);
	}

	@ParameterizedTest
	@ValueSource(ints = { Integer.MIN_VALUE, -1, 0, 11, 100, Integer.MAX_VALUE })
	void energy_outOfRange_isRejected(int energy) {
		assertThatThrownBy(() -> entry(energy, Emotion.CALM, null, null))
				.isInstanceOf(InvalidJournalEntryException.class)
				.hasMessageContaining("energy");
	}

	@Test
	void emotion_null_isRejected() {
		assertThatThrownBy(() -> entry(5, null, null, null))
				.isInstanceOf(InvalidJournalEntryException.class)
				.hasMessageContaining("emotion");
	}

	@Test
	void notes_areOptional_blankBecomesNull() {
		assertThat(entry(5, Emotion.CALM, null, null).notes()).isNull();
		assertThat(entry(5, Emotion.CALM, "   ", null).notes()).isNull();
	}

	@Test
	void notes_lengthLimit() {
		assertThat(entry(5, Emotion.CALM, "a".repeat(2000), null).notes()).hasSize(2000);
		assertThatThrownBy(() -> entry(5, Emotion.CALM, "a".repeat(2001), null))
				.isInstanceOf(InvalidJournalEntryException.class)
				.hasMessageContaining("notes");
	}

	@Test
	void tags_nullBecomesEmpty() {
		assertThat(entry(5, Emotion.CALM, null, null).tags()).isEmpty();
	}

	@Test
	void tags_areNormalized_andDeduplicated() {
		assertThat(entry(5, Emotion.CALM, null, List.of(" Work ", "WORK", "family")).tags())
				.containsExactlyInAnyOrder("work", "family");
	}

	@Test
	void tags_blankOrNull_isRejected() {
		assertThatThrownBy(() -> entry(5, Emotion.CALM, null, List.of("   ")))
				.isInstanceOf(InvalidJournalEntryException.class);
		assertThatThrownBy(() -> entry(5, Emotion.CALM, null, java.util.Arrays.asList("ok", null)))
				.isInstanceOf(InvalidJournalEntryException.class);
	}

	@Test
	void tags_lengthLimit() {
		assertThat(entry(5, Emotion.CALM, null, List.of("a".repeat(30))).tags()).hasSize(1);
		assertThatThrownBy(() -> entry(5, Emotion.CALM, null, List.of("a".repeat(31))))
				.isInstanceOf(InvalidJournalEntryException.class);
	}

	@Test
	void tags_maxCount() {
		assertThat(entry(5, Emotion.CALM, null, tags(10)).tags()).hasSize(10);
		assertThatThrownBy(() -> entry(5, Emotion.CALM, null, tags(11)))
				.isInstanceOf(InvalidJournalEntryException.class)
				.hasMessageContaining("tags");
	}

	@Test
	void tags_areImmutable() {
		Set<String> source = new HashSet<>(Set.of("work"));
		JournalEntry e = new JournalEntry(UUID.randomUUID(), USER, DATE, 5, Emotion.CALM, null, source);
		source.add("later");

		assertThat(e.tags()).containsExactly("work");
		assertThatThrownBy(() -> e.tags().add("x")).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void requiredFields_areEnforced() {
		UUID id = UUID.randomUUID();
		assertThatThrownBy(() -> new JournalEntry(null, USER, DATE, 5, Emotion.CALM, null, null))
				.isInstanceOf(InvalidJournalEntryException.class).hasMessageContaining("id");
		assertThatThrownBy(() -> new JournalEntry(id, null, DATE, 5, Emotion.CALM, null, null))
				.isInstanceOf(InvalidJournalEntryException.class).hasMessageContaining("userId");
		assertThatThrownBy(() -> new JournalEntry(id, USER, null, 5, Emotion.CALM, null, null))
				.isInstanceOf(InvalidJournalEntryException.class).hasMessageContaining("date");
	}

	@Test
	void toString_doesNotLeakNotes() {
		String text = entry(5, Emotion.SADNESS, "very private note", List.of("health")).toString();

		assertThat(text).doesNotContain("very private note").doesNotContain("SADNESS");
	}

	private static List<String> tags(int n) {
		return IntStream.range(0, n).mapToObj(i -> "tag" + i).toList();
	}
}
