package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.LoginUseCase;
import com.carlos.mindsync.application.port.out.PasswordHasherPort;
import com.carlos.mindsync.application.port.out.TokenProviderPort;
import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.domain.exception.InvalidCredentialsException;
import com.carlos.mindsync.domain.model.User;

public class LoginService implements LoginUseCase {

	private final UserRepositoryPort users;
	private final PasswordHasherPort passwordHasher;
	private final TokenProviderPort tokenProvider;

	public LoginService(UserRepositoryPort users, PasswordHasherPort passwordHasher, TokenProviderPort tokenProvider) {
		this.users = users;
		this.passwordHasher = passwordHasher;
		this.tokenProvider = tokenProvider;
	}

	@Override
	public String login(Command command) {
		if (command.email() == null || command.password() == null) {
			throw new InvalidCredentialsException();
		}
		return users.findByEmail(User.normalizeEmail(command.email()))
				.filter(user -> passwordHasher.matches(command.password(), user.passwordHash()))
				.map(tokenProvider::generateToken)
				.orElseThrow(InvalidCredentialsException::new);
	}
}
