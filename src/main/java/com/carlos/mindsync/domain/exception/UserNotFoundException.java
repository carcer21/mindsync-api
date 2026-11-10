package com.carlos.mindsync.domain.exception;

public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException() {
		super("user not found");
	}
}
