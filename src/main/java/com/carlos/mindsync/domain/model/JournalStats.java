package com.carlos.mindsync.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pure statistics over a user's entries. {@code averageEnergy} and {@code predominantEmotion}
 * are {@code null} when there are no entries.
 */
public record JournalStats(int currentStreak, Double averageEnergy, Emotion predominantEmotion,
		int entriesThisMonth) {

	public static JournalStats of(Collection<JournalEntry> entries, LocalDate today) {
		Objects.requireNonNull(entries, "entries");
		Objects.requireNonNull(today, "today");
		return new JournalStats(streak(entries, today), averageEnergy(entries), predominantEmotion(entries),
				entriesInMonth(entries, YearMonth.from(today)));
	}

	// Consecutive days with at least one entry, ending today. If nothing was written today yet,
	// the streak is still alive when yesterday has an entry (today is not over). Future dates are ignored.
	private static int streak(Collection<JournalEntry> entries, LocalDate today) {
		Set<LocalDate> days = new HashSet<>();
		for (JournalEntry entry : entries) {
			if (!entry.date().isAfter(today)) {
				days.add(entry.date());
			}
		}
		LocalDate day = days.contains(today) ? today : today.minusDays(1);
		int streak = 0;
		while (days.contains(day)) {
			streak++;
			day = day.minusDays(1);
		}
		return streak;
	}

	private static Double averageEnergy(Collection<JournalEntry> entries) {
		if (entries.isEmpty()) {
			return null;
		}
		double sum = entries.stream().mapToInt(JournalEntry::energy).sum();
		return BigDecimal.valueOf(sum / entries.size()).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}

	// Most frequent emotion; on a tie, the one seen most recently wins (then enum order, to stay deterministic).
	private static Emotion predominantEmotion(Collection<JournalEntry> entries) {
		Map<Emotion, Integer> counts = new EnumMap<>(Emotion.class);
		Map<Emotion, LocalDate> lastSeen = new EnumMap<>(Emotion.class);
		for (JournalEntry entry : entries) {
			counts.merge(entry.emotion(), 1, Integer::sum);
			lastSeen.merge(entry.emotion(), entry.date(), (a, b) -> a.isAfter(b) ? a : b);
		}
		return counts.keySet().stream()
				.max(Comparator.<Emotion>comparingInt(counts::get)
						.thenComparing(lastSeen::get)
						.thenComparing(Comparator.<Emotion>naturalOrder().reversed()))
				.orElse(null);
	}

	private static int entriesInMonth(Collection<JournalEntry> entries, YearMonth month) {
		return (int) entries.stream().filter(e -> YearMonth.from(e.date()).equals(month)).count();
	}
}
