package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.LoginUseCase.Command;
import com.carlos.mindsync.application.port.out.PasswordHasherPort;
import com.carlos.mindsync.application.port.out.TokenProviderPort;
import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.domain.exception.InvalidCredentialsException;
import com.carlos.mindsync.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

	private static final String PASSWORD = "s3cret-Passw0rd";
	private static final User ANA = User.register("Ana", "ana@example.com", "hashed-value");

	@Mock
	private UserRepositoryPort users;
	@Mock
	private PasswordHasherPort passwordHasher;
	@Mock
	private TokenProviderPort tokenProvider;
	@InjectMocks
	private LoginService service;

	@Test
	void login_returnsTokenForValidCredentials() {
		when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(ANA));
		when(passwordHasher.matches(PASSWORD, "hashed-value")).thenReturn(true);
		when(tokenProvider.generateToken(ANA)).thenReturn("signed-token");

		assertThat(service.login(new Command("ana@example.com", PASSWORD))).isEqualTo("signed-token");
	}

	@Test
	void login_normalizesEmail() {
		when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(ANA));
		when(passwordHasher.matches(PASSWORD, "hashed-value")).thenReturn(true);
		when(tokenProvider.generateToken(ANA)).thenReturn("signed-token");

		assertThat(service.login(new Command("  ANA@Example.com ", PASSWORD))).isEqualTo("signed-token");
	}

	@Test
	void login_rejectsWrongPassword() {
		when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(ANA));
		when(passwordHasher.matches("wrong-password", "hashed-value")).thenReturn(false);

		assertThatThrownBy(() -> service.login(new Command("ana@example.com", "wrong-password")))
				.isInstanceOf(InvalidCredentialsException.class);

		verifyNoInteractions(tokenProvider);
	}

	@Test
	void login_rejectsUnknownEmail_withSameErrorAsWrongPassword() {
		when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login(new Command("nobody@example.com", PASSWORD)))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage(new InvalidCredentialsException().getMessage());

		verifyNoInteractions(passwordHasher, tokenProvider);
	}

	@Test
	void login_rejectsNullEmailOrPassword() {
		assertThatThrownBy(() -> service.login(new Command(null, PASSWORD)))
				.isInstanceOf(InvalidCredentialsException.class);
		assertThatThrownBy(() -> service.login(new Command("ana@example.com", null)))
				.isInstanceOf(InvalidCredentialsException.class);

		verifyNoInteractions(users, passwordHasher, tokenProvider);
	}
}
