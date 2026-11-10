package com.carlos.mindsync.domain.exception;

// Same exception for "does not exist" and "belongs to someone else": never reveal which.
public class JournalEntryNotFoundException extends RuntimeException {

	public JournalEntryNotFoundException() {
		super("journal entry not found");
	}
}
