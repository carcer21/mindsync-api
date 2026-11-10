package com.carlos.mindsync.infrastructure.adapter.out.persistence;

import com.carlos.mindsync.TestcontainersConfiguration;
import com.carlos.mindsync.domain.exception.EmailAlreadyExistsException;
import com.carlos.mindsync.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, UserPersistenceAdapter.class})
class UserPersistenceAdapterTest {

	@Autowired
	private UserPersistenceAdapter adapter;

	@Test
	void save_thenFindByEmail_returnsSameUser() {
		User saved = adapter.save(User.register("Ana", "ana@example.com", "hash-1"));

		assertThat(adapter.findByEmail("ana@example.com")).contains(saved);
	}

	@Test
	void findByEmail_returnsEmptyWhenUnknown() {
		assertThat(adapter.findByEmail("nobody@example.com")).isEmpty();
	}

	@Test
	void existsByEmail_reflectsStoredUsers() {
		adapter.save(User.register("Ben", "ben@example.com", "hash-2"));

		assertThat(adapter.existsByEmail("ben@example.com")).isTrue();
		assertThat(adapter.existsByEmail("other@example.com")).isFalse();
	}

	@Test
	void save_duplicateEmail_isTranslatedToDomainException() {
		adapter.save(User.register("Cleo", "cleo@example.com", "hash-3"));

		assertThatThrownBy(() -> adapter.save(User.register("Other Cleo", "cleo@example.com", "hash-4")))
				.isInstanceOf(EmailAlreadyExistsException.class);
	}
}
