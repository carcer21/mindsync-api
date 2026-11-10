CREATE TABLE journal_entries (
    id         UUID          PRIMARY KEY,
    user_id    UUID          NOT NULL,
    entry_date DATE          NOT NULL,
    energy     INTEGER       NOT NULL,
    emotion    VARCHAR(20)   NOT NULL,
    notes      VARCHAR(2000),
    CONSTRAINT fk_journal_entries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_journal_entries_energy CHECK (energy BETWEEN 1 AND 10)
);

-- Matches the main query: one user's entries, newest first.
CREATE INDEX ix_journal_entries_user_date ON journal_entries (user_id, entry_date DESC);

CREATE TABLE journal_entry_tags (
    entry_id UUID        NOT NULL,
    tag      VARCHAR(30) NOT NULL,
    CONSTRAINT pk_journal_entry_tags PRIMARY KEY (entry_id, tag),
    CONSTRAINT fk_journal_entry_tags_entry FOREIGN KEY (entry_id) REFERENCES journal_entries (id) ON DELETE CASCADE
);
