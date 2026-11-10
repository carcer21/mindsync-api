package com.carlos.mindsync.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalStatsTest {

	private static final UUID USER = UUID.randomUUID();
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

	private static JournalEntry entry(LocalDate date, int energy, Emotion emotion) {
		return JournalEntry.create(USER, date, energy, emotion, null, null);
	}

	private static JournalEntry entry(LocalDate date) {
		return entry(date, 5, Emotion.CALM);
	}

	private static JournalStats stats(LocalDate today, JournalEntry... entries) {
		return JournalStats.of(List.of(entries), today);
	}

	// ---- no data

	@Test
	void noEntries_everythingIsEmptyOrZero() {
		JournalStats s = stats(TODAY);

		assertThat(s.currentStreak()).isZero();
		assertThat(s.averageEnergy()).isNull();
		assertThat(s.predominantEmotion()).isNull();
		assertThat(s.entriesThisMonth()).isZero();
	}

	@Test
	void nullArguments_areRejected() {
		assertThatThrownBy(() -> JournalStats.of(null, TODAY)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> JournalStats.of(List.of(), null)).isInstanceOf(NullPointerException.class);
	}

	// ---- streak

	@Test
	void streak_oneEntryToday_isOne() {
		assertThat(stats(TODAY, entry(TODAY)).currentStreak()).isEqualTo(1);
	}

	@Test
	void streak_consecutiveDaysUpToToday() {
		assertThat(stats(TODAY, entry(TODAY), entry(TODAY.minusDays(1)), entry(TODAY.minusDays(2)))
				.currentStreak()).isEqualTo(3);
	}

	@Test
	void streak_stillAliveWhenOnlyYesterdayHasEntry_todayIsNotOver() {
		assertThat(stats(TODAY, entry(TODAY.minusDays(1)), entry(TODAY.minusDays(2))).currentStreak())
				.isEqualTo(2);
	}

	@Test
	void streak_isBrokenWhenYesterdayAndTodayAreEmpty() {
		assertThat(stats(TODAY, entry(TODAY.minusDays(2)), entry(TODAY.minusDays(3))).currentStreak()).isZero();
	}

	@Test
	void streak_skippedDayStopsTheCount() {
		// today, yesterday, (gap), 3 days ago, 4 days ago -> only today + yesterday count
		assertThat(stats(TODAY, entry(TODAY), entry(TODAY.minusDays(1)), entry(TODAY.minusDays(3)),
				entry(TODAY.minusDays(4))).currentStreak()).isEqualTo(2);
	}

	@Test
	void streak_severalEntriesOnTheSameDayCountOnce() {
		assertThat(stats(TODAY, entry(TODAY), entry(TODAY), entry(TODAY.minusDays(1))).currentStreak())
				.isEqualTo(2);
	}

	@Test
	void streak_entriesGivenInAnyOrder() {
		assertThat(stats(TODAY, entry(TODAY.minusDays(2)), entry(TODAY), entry(TODAY.minusDays(1)))
				.currentStreak()).isEqualTo(3);
	}

	@Test
	void streak_futureEntriesAreIgnored() {
		// A user a day ahead of the server may log "tomorrow": it must not create or extend a streak.
		assertThat(stats(TODAY, entry(TODAY.plusDays(1))).currentStreak()).isZero();
		assertThat(stats(TODAY, entry(TODAY.plusDays(1)), entry(TODAY)).currentStreak()).isEqualTo(1);
	}

	@Test
	void streak_crossesAMonthBoundary() {
		LocalDate firstOfMarch = LocalDate.of(2026, 3, 1);
		assertThat(stats(firstOfMarch, entry(firstOfMarch), entry(LocalDate.of(2026, 2, 28)),
				entry(LocalDate.of(2026, 2, 27))).currentStreak()).isEqualTo(3);
	}

	@Test
	void streak_crossesAYearBoundary() {
		LocalDate newYear = LocalDate.of(2026, 1, 1);
		assertThat(stats(newYear, entry(newYear), entry(LocalDate.of(2025, 12, 31))).currentStreak()).isEqualTo(2);
	}

	@Test
	void streak_handlesLeapDay() {
		LocalDate march1 = LocalDate.of(2028, 3, 1);
		assertThat(stats(march1, entry(march1), entry(LocalDate.of(2028, 2, 29)), entry(LocalDate.of(2028, 2, 28)))
				.currentStreak()).isEqualTo(3);
	}

	// ---- average energy

	@Test
	void averageEnergy_isRoundedToOneDecimal() {
		assertThat(stats(TODAY, entry(TODAY, 3, Emotion.CALM), entry(TODAY, 4, Emotion.CALM)).averageEnergy())
				.isEqualTo(3.5);
		assertThat(stats(TODAY, entry(TODAY, 1, Emotion.CALM), entry(TODAY, 1, Emotion.CALM),
				entry(TODAY, 2, Emotion.CALM)).averageEnergy()).isEqualTo(1.3); // 1.333...
		assertThat(stats(TODAY, entry(TODAY, 10, Emotion.CALM), entry(TODAY, 10, Emotion.CALM),
				entry(TODAY, 9, Emotion.CALM)).averageEnergy()).isEqualTo(9.7); // 9.666...
	}

	@Test
	void averageEnergy_singleEntryAndExtremes() {
		assertThat(stats(TODAY, entry(TODAY, 7, Emotion.CALM)).averageEnergy()).isEqualTo(7.0);
		assertThat(stats(TODAY, entry(TODAY, 1, Emotion.CALM), entry(TODAY, 10, Emotion.CALM)).averageEnergy())
				.isEqualTo(5.5);
	}

	@Test
	void averageEnergy_usesAllEntriesNotOnlyThisMonth() {
		assertThat(stats(TODAY, entry(TODAY, 8, Emotion.CALM), entry(TODAY.minusMonths(3), 2, Emotion.CALM))
				.averageEnergy()).isEqualTo(5.0);
	}

	// ---- predominant emotion

	@Test
	void predominantEmotion_isTheMostFrequent() {
		assertThat(stats(TODAY, entry(TODAY, 5, Emotion.JOY), entry(TODAY.minusDays(1), 5, Emotion.STRESS),
				entry(TODAY.minusDays(2), 5, Emotion.STRESS)).predominantEmotion()).isEqualTo(Emotion.STRESS);
	}

	@Test
	void predominantEmotion_tieGoesToTheMostRecent() {
		assertThat(stats(TODAY, entry(TODAY.minusDays(5), 5, Emotion.SADNESS), entry(TODAY.minusDays(1), 5, Emotion.JOY))
				.predominantEmotion()).isEqualTo(Emotion.JOY);
		assertThat(stats(TODAY, entry(TODAY.minusDays(1), 5, Emotion.SADNESS), entry(TODAY.minusDays(5), 5, Emotion.JOY))
				.predominantEmotion()).isEqualTo(Emotion.SADNESS);
	}

	@Test
	void predominantEmotion_tieOnSameDayIsDeterministic() {
		JournalStats a = stats(TODAY, entry(TODAY, 5, Emotion.JOY), entry(TODAY, 5, Emotion.SADNESS));
		JournalStats b = stats(TODAY, entry(TODAY, 5, Emotion.SADNESS), entry(TODAY, 5, Emotion.JOY));

		assertThat(a.predominantEmotion()).isEqualTo(b.predominantEmotion()).isEqualTo(Emotion.JOY);
	}

	@Test
	void predominantEmotion_singleEntry() {
		assertThat(stats(TODAY, entry(TODAY, 5, Emotion.ANGER)).predominantEmotion()).isEqualTo(Emotion.ANGER);
	}

	// ---- entries this month

	@Test
	void entriesThisMonth_countsOnlyTheMonthOfToday() {
		assertThat(stats(TODAY, entry(TODAY), entry(TODAY.minusDays(9)), entry(LocalDate.of(2026, 2, 28)),
				entry(LocalDate.of(2026, 4, 1))).entriesThisMonth()).isEqualTo(2);
	}

	@Test
	void entriesThisMonth_sameMonthOfAnotherYearDoesNotCount() {
		assertThat(stats(TODAY, entry(LocalDate.of(2025, 3, 10)), entry(TODAY)).entriesThisMonth()).isEqualTo(1);
	}

	@Test
	void entriesThisMonth_changeOfMonthResetsTheCount() {
		JournalEntry lastDayOfFeb = entry(LocalDate.of(2026, 2, 28));
		JournalEntry firstOfMarch = entry(LocalDate.of(2026, 3, 1));

		assertThat(stats(LocalDate.of(2026, 2, 28), lastDayOfFeb, firstOfMarch).entriesThisMonth()).isEqualTo(1);
		assertThat(stats(LocalDate.of(2026, 3, 1), lastDayOfFeb, firstOfMarch).entriesThisMonth()).isEqualTo(1);
	}

	@Test
	void entriesThisMonth_countsEveryEntryNotEveryDay() {
		assertThat(stats(TODAY, entry(TODAY), entry(TODAY), entry(TODAY)).entriesThisMonth()).isEqualTo(3);
	}
}
