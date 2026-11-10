package com.carlos.mindsync.infrastructure.adapter.out.persistence;

import com.carlos.mindsync.TestcontainersConfiguration;
import com.carlos.mindsync.domain.model.Emotion;
import com.carlos.mindsync.domain.model.JournalEntry;
import com.carlos.mindsync.domain.model.Page;
import com.carlos.mindsync.domain.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, UserPersistenceAdapter.class, JournalEntryPersistenceAdapter.class})
class JournalEntryPersistenceAdapterTest {

	private static final LocalDate DAY = LocalDate.of(2026, 3, 10);

	@Autowired
	private JournalEntryPersistenceAdapter adapter;
	@Autowired
	private UserPersistenceAdapter users;
	@Autowired
	private EntityManager em;

	private UUID ana;
	private UUID ben;

	@BeforeEach
	void createUsers() {
		ana = users.save(User.register("Ana", "ana.journal@example.com", "hash-1")).id();
		ben = users.save(User.register("Ben", "ben.journal@example.com", "hash-2")).id();
	}

	private JournalEntry saved(UUID owner, LocalDate date) {
		return adapter.save(
				JournalEntry.create(owner, date, 6, Emotion.CALM, "invented note", List.of("work", "family")));
	}

	// Forces the next read to hit the database instead of the Hibernate first-level cache.
	private void flushAndClear() {
		em.flush();
		em.clear();
	}

	private long tagRows(UUID entryId) {
		return ((Number) em.createNativeQuery("SELECT count(*) FROM journal_entry_tags WHERE entry_id = :id")
				.setParameter("id", entryId).getSingleResult()).longValue();
	}

	@Test
	void save_thenFind_roundTripsEveryField() {
		JournalEntry entry = saved(ana, DAY);
		flushAndClear();

		assertThat(adapter.findByIdAndUserId(entry.id(), ana)).contains(entry);
		assertThat(tagRows(entry.id())).isEqualTo(2);
	}

	@Test
	void save_withoutNotesOrTags_roundTrips() {
		JournalEntry entry = adapter.save(JournalEntry.create(ana, DAY, 1, Emotion.FATIGUE, null, null));
		flushAndClear();

		JournalEntry found = adapter.findByIdAndUserId(entry.id(), ana).orElseThrow();
		assertThat(found.notes()).isNull();
		assertThat(found.tags()).isEmpty();
	}

	@Test
	void find_entryOfAnotherUser_isEmpty() {
		JournalEntry entry = saved(ana, DAY);
		flushAndClear();

		assertThat(adapter.findByIdAndUserId(entry.id(), ben)).isEmpty();
	}

	@Test
	void find_unknownId_isEmpty() {
		assertThat(adapter.findByIdAndUserId(UUID.randomUUID(), ana)).isEmpty();
	}

	@Test
	void list_returnsOnlyOwnEntries_newestFirst() {
		JournalEntry oldest = saved(ana, DAY.minusDays(2));
		JournalEntry newest = saved(ana, DAY);
		JournalEntry middle = saved(ana, DAY.minusDays(1));
		saved(ben, DAY);
		flushAndClear();

		Page<JournalEntry> page = adapter.findByUserId(ana, 0, 10);

		assertThat(page.content()).containsExactly(newest, middle, oldest);
		assertThat(page.totalElements()).isEqualTo(3);
	}

	@Test
	void list_paginates() {
		for (int i = 0; i < 5; i++) {
			saved(ana, DAY.minusDays(i));
		}
		flushAndClear();

		Page<JournalEntry> first = adapter.findByUserId(ana, 0, 2);
		Page<JournalEntry> last = adapter.findByUserId(ana, 2, 2);

		assertThat(first.content()).extracting(JournalEntry::date).containsExactly(DAY, DAY.minusDays(1));
		assertThat(first.content())
				.allSatisfy(e -> assertThat(e.tags()).containsExactlyInAnyOrder("work", "family"));
		assertThat(last.content()).extracting(JournalEntry::date).containsExactly(DAY.minusDays(4));
		assertThat(first.totalElements()).isEqualTo(5);
		assertThat(first.totalPages()).isEqualTo(3);
		assertThat(adapter.findByUserId(ana, 9, 2).content()).isEmpty();
	}

	@Test
	void list_userWithoutEntries_isEmpty() {
		saved(ana, DAY);

		assertThat(adapter.findByUserId(ben, 0, 10).content()).isEmpty();
	}

	@Test
	void delete_byOwner_removesEntryAndItsTags() {
		JournalEntry entry = saved(ana, DAY);
		flushAndClear();

		assertThat(adapter.deleteByIdAndUserId(entry.id(), ana)).isTrue();
		flushAndClear();

		assertThat(adapter.findByIdAndUserId(entry.id(), ana)).isEmpty();
		assertThat(tagRows(entry.id())).isZero();
	}

	@Test
	void delete_byAnotherUser_deletesNothing() {
		JournalEntry entry = saved(ana, DAY);
		flushAndClear();

		assertThat(adapter.deleteByIdAndUserId(entry.id(), ben)).isFalse();
		flushAndClear();

		assertThat(adapter.findByIdAndUserId(entry.id(), ana)).contains(entry);
	}

	@Test
	void delete_unknownId_returnsFalse() {
		assertThat(adapter.deleteByIdAndUserId(UUID.randomUUID(), ana)).isFalse();
	}
}
