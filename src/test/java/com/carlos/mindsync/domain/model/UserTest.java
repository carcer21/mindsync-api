package com.carlos.mindsync.domain.model;

import com.carlos.mindsync.domain.exception.InvalidUserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

	private static final String HASH = "$2a$10$fakehashfortestsonly";

	@Test
	void register_generatesIdAndKeepsData() {
		User user = User.register("Ana", "ana@example.com", HASH);

		assertThat(user.id()).isNotNull();
		assertThat(user.name()).isEqualTo("Ana");
		assertThat(user.email()).isEqualTo("ana@example.com");
		assertThat(user.passwordHash()).isEqualTo(HASH);
	}

	@Test
	void register_trimsNameAndNormalizesEmail() {
		User user = User.register("  Ana  ", "  Ana@Example.COM ", HASH);

		assertThat(user.name()).isEqualTo("Ana");
		assertThat(user.email()).isEqualTo("ana@example.com");
	}

	@Test
	void register_generatesDifferentIds() {
		assertThat(User.register("Ana", "a@example.com", HASH).id())
				.isNotEqualTo(User.register("Ana", "a@example.com", HASH).id());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   "})
	void rejectsBlankName(String name) {
		assertThatThrownBy(() -> User.register(name, "ana@example.com", HASH))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("name");
	}

	@Test
	void rejectsTooLongName() {
		String name = "a".repeat(User.NAME_MAX_LENGTH + 1);

		assertThatThrownBy(() -> User.register(name, "ana@example.com", HASH))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("name");
	}

	@Test
	void acceptsNameAtMaxLength() {
		assertThat(User.register("a".repeat(User.NAME_MAX_LENGTH), "ana@example.com", HASH).name())
				.hasSize(User.NAME_MAX_LENGTH);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "ana", "ana@", "@example.com", "ana@example", "ana @example.com", "a@@example.com"})
	void rejectsInvalidEmail(String email) {
		assertThatThrownBy(() -> User.register("Ana", email, HASH))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("email");
	}

	@Test
	void rejectsTooLongEmail() {
		String email = "a".repeat(User.EMAIL_MAX_LENGTH) + "@example.com";

		assertThatThrownBy(() -> User.register("Ana", email, HASH))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("email");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   "})
	void rejectsBlankPasswordHash(String hash) {
		assertThatThrownBy(() -> User.register("Ana", "ana@example.com", hash))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("passwordHash");
	}

	@Test
	void rejectsNullId() {
		assertThatThrownBy(() -> new User(null, "Ana", "ana@example.com", HASH))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("id");
	}

	@Test
	void toString_doesNotLeakPasswordHash() {
		User user = new User(UUID.randomUUID(), "Ana", "ana@example.com", HASH);

		assertThat(user.toString()).doesNotContain(HASH).doesNotContain("passwordHash");
	}
}
