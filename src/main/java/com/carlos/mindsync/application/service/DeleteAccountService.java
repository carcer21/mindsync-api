package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.DeleteAccountUseCase;
import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.domain.exception.UserNotFoundException;

import java.util.UUID;

public class DeleteAccountService implements DeleteAccountUseCase {

	private final UserRepositoryPort users;

	public DeleteAccountService(UserRepositoryPort users) {
		this.users = users;
	}

	@Override
	public void delete(UUID userId) {
		if (!users.deleteById(userId)) {
			throw new UserNotFoundException();
		}
	}
}
