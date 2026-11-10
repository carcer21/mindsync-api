package com.carlos.mindsync.domain.model;

import com.carlos.mindsync.domain.exception.InvalidUserException;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public record User(UUID id, String name, String email, String passwordHash) {

	static final int NAME_MAX_LENGTH = 100;
	static final int EMAIL_MAX_LENGTH = 254;
	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	public User {
		if (id == null) {
			throw new InvalidUserException("id is required");
		}
		if (name == null || name.isBlank()) {
			throw new InvalidUserException("name is required");
		}
		name = name.trim();
		if (name.length() > NAME_MAX_LENGTH) {
			throw new InvalidUserException("name must be at most " + NAME_MAX_LENGTH + " characters");
		}
		if (email == null || email.isBlank()) {
			throw new InvalidUserException("email is required");
		}
		email = email.trim().toLowerCase(Locale.ROOT);
		if (email.length() > EMAIL_MAX_LENGTH || !EMAIL.matcher(email).matches()) {
			throw new InvalidUserException("email is not valid");
		}
		if (passwordHash == null || passwordHash.isBlank()) {
			throw new InvalidUserException("passwordHash is required");
		}
	}

	public static User register(String name, String email, String passwordHash) {
		return new User(UUID.randomUUID(), name, email, passwordHash);
	}

	// Records print every field by default; never leak the hash into logs.
	@Override
	public String toString() {
		return "User[id=" + id + ", email=" + email + "]";
	}
}
