package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.RegisterUserUseCase;
import com.carlos.mindsync.application.port.out.PasswordHasherPort;
import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.domain.exception.EmailAlreadyExistsException;
import com.carlos.mindsync.domain.exception.InvalidUserException;
import com.carlos.mindsync.domain.model.User;

import java.nio.charset.StandardCharsets;

public class RegisterUserService implements RegisterUserUseCase {

	static final int PASSWORD_MIN_LENGTH = 8;
	// BCrypt only uses the first 72 bytes of a password.
	static final int PASSWORD_MAX_BYTES = 72;

	private final UserRepositoryPort users;
	private final PasswordHasherPort passwordHasher;

	public RegisterUserService(UserRepositoryPort users, PasswordHasherPort passwordHasher) {
		this.users = users;
		this.passwordHasher = passwordHasher;
	}

	@Override
	public User register(Command command) {
		validatePassword(command.password());
		User user = User.register(command.name(), command.email(), passwordHasher.hash(command.password()));
		if (users.existsByEmail(user.email())) {
			throw new EmailAlreadyExistsException();
		}
		return users.save(user);
	}

	private static void validatePassword(String password) {
		if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
			throw new InvalidUserException("password must be at least " + PASSWORD_MIN_LENGTH + " characters");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
			throw new InvalidUserException("password must be at most " + PASSWORD_MAX_BYTES + " bytes");
		}
	}
}
