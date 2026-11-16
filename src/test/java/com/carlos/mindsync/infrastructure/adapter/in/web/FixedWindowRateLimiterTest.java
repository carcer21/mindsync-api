package com.carlos.mindsync.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FixedWindowRateLimiterTest {

	// A clock the test moves by hand: no sleeping, no flaky timing.
	private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-16T10:00:00Z"));
	private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(2, Duration.ofMinutes(1), now::get);

	@Test
	void allowsUpToTheLimit_thenRejects() {
		assertThat(limiter.tryAcquire("a")).isTrue();
		assertThat(limiter.tryAcquire("a")).isTrue();
		assertThat(limiter.tryAcquire("a")).isFalse();
		assertThat(limiter.tryAcquire("a")).isFalse();
	}

	@Test
	void eachKeyHasItsOwnCounter() {
		limiter.tryAcquire("a");
		limiter.tryAcquire("a");

		assertThat(limiter.tryAcquire("a")).isFalse();
		assertThat(limiter.tryAcquire("b")).isTrue();
	}

	@Test
	void staysBlockedUntilTheWindowEnds_thenStartsAgain() {
		limiter.tryAcquire("a");
		limiter.tryAcquire("a");

		now.set(now.get().plusSeconds(59));
		assertThat(limiter.tryAcquire("a")).isFalse();

		now.set(now.get().plusSeconds(1));
		assertThat(limiter.tryAcquire("a")).isTrue();
	}

	@Test
	void secondsUntilReset_countsDownAndNeverReturnsZero() {
		limiter.tryAcquire("a");
		assertThat(limiter.secondsUntilReset()).isEqualTo(61);

		now.set(now.get().plusSeconds(45));
		assertThat(limiter.secondsUntilReset()).isEqualTo(16);

		now.set(now.get().plusMillis(14_999));
		assertThat(limiter.secondsUntilReset()).isEqualTo(1);
	}
}
