package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.domain.model.Emotion;
import com.carlos.mindsync.domain.model.JournalStats;

// averageEnergy and predominantEmotion are null when the user has no entries in the period.
record StatsResponse(int periodDays, int currentStreak, Double averageEnergy, Emotion predominantEmotion,
		int entriesThisMonth) {

	static StatsResponse from(JournalStats stats, int periodDays) {
		return new StatsResponse(periodDays, stats.currentStreak(), stats.averageEnergy(),
				stats.predominantEmotion(), stats.entriesThisMonth());
	}
}
