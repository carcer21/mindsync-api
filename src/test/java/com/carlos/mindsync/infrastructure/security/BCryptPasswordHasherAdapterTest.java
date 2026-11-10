package com.carlos.mindsync.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherAdapterTest {

	private static final String PASSWORD = "s3cret-Passw0rd";

	private final BCryptPasswordHasherAdapter hasher = new BCryptPasswordHasherAdapter();

	@Test
	void hash_isNotThePlainPassword() {
		assertThat(hasher.hash(PASSWORD)).isNotEqualTo(PASSWORD).startsWith("$2");
	}

	@Test
	void hash_usesRandomSalt_soSamePasswordGivesDifferentHashes() {
		assertThat(hasher.hash(PASSWORD)).isNotEqualTo(hasher.hash(PASSWORD));
	}

	@Test
	void matches_acceptsRightPasswordAndRejectsWrongOne() {
		String hash = hasher.hash(PASSWORD);

		assertThat(hasher.matches(PASSWORD, hash)).isTrue();
		assertThat(hasher.matches("wrong-password", hash)).isFalse();
	}

	@Test
	void matches_rejectsOverlongPasswordWithoutFailing() {
		String hash = hasher.hash(PASSWORD);

		assertThat(hasher.matches("a".repeat(500), hash)).isFalse();
	}

	@Test
	void matches_rejectsValueThatIsNotAHash() {
		assertThat(hasher.matches(PASSWORD, PASSWORD)).isFalse();
	}
}
