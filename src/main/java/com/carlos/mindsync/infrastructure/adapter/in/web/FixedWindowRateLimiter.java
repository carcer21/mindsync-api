package com.carlos.mindsync.infrastructure.adapter.in.web;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.HashMap;
import java.util.Map;

// Fixed window: every key may make `limit` calls per window. All counters are dropped together when the
// window ends, so memory cannot grow without bound. Trade-off: up to 2x the limit across a window boundary.
// In memory: counters are lost on restart and are not shared between instances.
final class FixedWindowRateLimiter {

	private final int limit;
	private final Duration window;
	private final InstantSource clock;
	private final Map<String, Integer> counts = new HashMap<>();
	private Instant windowEnd;

	FixedWindowRateLimiter(int limit, Duration window, InstantSource clock) {
		this.limit = limit;
		this.window = window;
		this.clock = clock;
		this.windowEnd = clock.instant();
	}

	synchronized boolean tryAcquire(String key) {
		Instant now = clock.instant();
		if (!now.isBefore(windowEnd)) {
			counts.clear();
			windowEnd = now.plus(window);
		}
		return counts.merge(key, 1, Integer::sum) <= limit;
	}

	// Rounded up, so the client never retries a moment too early.
	synchronized long secondsUntilReset() {
		return Duration.between(clock.instant(), windowEnd).toSeconds() + 1;
	}
}
