package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteAccountServiceTest {

	private static final UUID USER = UUID.randomUUID();

	@Mock
	private UserRepositoryPort users;
	@InjectMocks
	private DeleteAccountService service;

	@Test
	void delete_removesTheUser() {
		when(users.deleteById(USER)).thenReturn(true);

		assertThatCode(() -> service.delete(USER)).doesNotThrowAnyException();

		verify(users).deleteById(USER);
	}

	@Test
	void delete_whenNothingWasDeleted_throwsNotFound() {
		when(users.deleteById(USER)).thenReturn(false);

		assertThatThrownBy(() -> service.delete(USER)).isInstanceOf(UserNotFoundException.class);
	}
}
