package com.carlos.mindsync.domain.exception;

public class InvalidJournalEntryException extends RuntimeException {

	public InvalidJournalEntryException(String message) {
		super(message);
	}
}
