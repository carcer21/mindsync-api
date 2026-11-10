package com.carlos.mindsync.domain.exception;

public class InvalidCredentialsException extends RuntimeException {

	// Same message for unknown email and wrong password: do not reveal which one failed.
	public InvalidCredentialsException() {
		super("invalid email or password");
	}
}
