package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.domain.exception.UserNotFoundException;
import com.carlos.mindsync.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserServiceTest {

	private static final User ANA = User.register("Ana", "ana@example.com", "hashed-value");

	@Mock
	private UserRepositoryPort users;
	@InjectMocks
	private GetUserService service;

	@Test
	void getById_returnsUser() {
		when(users.findById(ANA.id())).thenReturn(Optional.of(ANA));

		assertThat(service.getById(ANA.id())).isEqualTo(ANA);
	}

	@Test
	void getById_throwsWhenUserDoesNotExist() {
		UUID unknownId = UUID.randomUUID();
		when(users.findById(unknownId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(unknownId)).isInstanceOf(UserNotFoundException.class);
	}
}
