package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.RegisterUserUseCase.Command;
import com.carlos.mindsync.application.port.out.PasswordHasherPort;
import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.domain.exception.EmailAlreadyExistsException;
import com.carlos.mindsync.domain.exception.InvalidUserException;
import com.carlos.mindsync.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

	private static final String PASSWORD = "s3cret-Passw0rd";
	private static final String HASH = "hashed-value";

	@Mock
	private UserRepositoryPort users;
	@Mock
	private PasswordHasherPort passwordHasher;
	@InjectMocks
	private RegisterUserService service;

	@Test
	void register_hashesPasswordAndSavesUser() {
		when(passwordHasher.hash(PASSWORD)).thenReturn(HASH);
		when(users.existsByEmail("ana@example.com")).thenReturn(false);
		when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User user = service.register(new Command("Ana", "Ana@Example.com", PASSWORD));

		assertThat(user.id()).isNotNull();
		assertThat(user.name()).isEqualTo("Ana");
		assertThat(user.email()).isEqualTo("ana@example.com");
		assertThat(user.passwordHash()).isEqualTo(HASH).isNotEqualTo(PASSWORD);
		verify(users).save(user);
	}

	@Test
	void register_rejectsDuplicateEmail_ignoringCaseAndSpaces() {
		when(passwordHasher.hash(PASSWORD)).thenReturn(HASH);
		when(users.existsByEmail("ana@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.register(new Command("Ana", " ANA@example.com ", PASSWORD)))
				.isInstanceOf(EmailAlreadyExistsException.class);

		verify(users, never()).save(any());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "1234567"})
	void register_rejectsTooShortPassword(String password) {
		assertThatThrownBy(() -> service.register(new Command("Ana", "ana@example.com", password)))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("password");

		verifyNoInteractions(passwordHasher, users);
	}

	@Test
	void register_acceptsPasswordAtMinLength() {
		when(passwordHasher.hash("12345678")).thenReturn(HASH);
		when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.register(new Command("Ana", "ana@example.com", "12345678"))).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(ints = {73, 200})
	void register_rejectsPasswordOver72Bytes(int length) {
		assertThatThrownBy(() -> service.register(new Command("Ana", "ana@example.com", "a".repeat(length))))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("password");

		verifyNoInteractions(passwordHasher, users);
	}

	@Test
	void register_countsBytesNotCharacters() {
		// 40 characters, but 80 bytes in UTF-8.
		assertThatThrownBy(() -> service.register(new Command("Ana", "ana@example.com", "ñ".repeat(40))))
				.isInstanceOf(InvalidUserException.class);
	}

	@Test
	void register_rejectsInvalidUserData_withoutSaving() {
		when(passwordHasher.hash(PASSWORD)).thenReturn(HASH);

		assertThatThrownBy(() -> service.register(new Command("Ana", "not-an-email", PASSWORD)))
				.isInstanceOf(InvalidUserException.class);

		verifyNoInteractions(users);
	}
}
