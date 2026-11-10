package com.carlos.mindsync.application.port.in;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandToStringTest {

	private static final String PASSWORD = "s3cret-Passw0rd";

	@Test
	void registerCommand_doesNotLeakPassword() {
		var command = new RegisterUserUseCase.Command("Ana", "ana@example.com", PASSWORD);

		assertThat(command.toString()).doesNotContain(PASSWORD);
	}

	@Test
	void loginCommand_doesNotLeakPassword() {
		var command = new LoginUseCase.Command("ana@example.com", PASSWORD);

		assertThat(command.toString()).doesNotContain(PASSWORD);
	}
}
