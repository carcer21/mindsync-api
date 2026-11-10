package com.carlos.mindsync.domain.exception;

public class EmailAlreadyExistsException extends RuntimeException {

	public EmailAlreadyExistsException() {
		super("email is already registered");
	}
}
