package com.carlos.mindsync.infrastructure.config;

import com.carlos.mindsync.application.port.in.CreateEntryUseCase;
import com.carlos.mindsync.application.port.in.DeleteEntryUseCase;
import com.carlos.mindsync.application.port.in.GetEntryUseCase;
import com.carlos.mindsync.application.port.in.GetUserUseCase;
import com.carlos.mindsync.application.port.in.ListEntriesUseCase;
import com.carlos.mindsync.application.port.in.LoginUseCase;
import com.carlos.mindsync.application.port.in.RegisterUserUseCase;
import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.application.port.out.PasswordHasherPort;
import com.carlos.mindsync.application.port.out.TokenProviderPort;
import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.application.service.CreateEntryService;
import com.carlos.mindsync.application.service.DeleteEntryService;
import com.carlos.mindsync.application.service.GetEntryService;
import com.carlos.mindsync.application.service.GetUserService;
import com.carlos.mindsync.application.service.ListEntriesService;
import com.carlos.mindsync.application.service.LoginService;
import com.carlos.mindsync.application.service.RegisterUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// The application layer has no Spring annotations, so its services are wired here.
@Configuration
class UseCaseConfig {

	@Bean
	RegisterUserUseCase registerUserUseCase(UserRepositoryPort users, PasswordHasherPort passwordHasher) {
		return new RegisterUserService(users, passwordHasher);
	}

	@Bean
	GetUserUseCase getUserUseCase(UserRepositoryPort users) {
		return new GetUserService(users);
	}

	@Bean
	LoginUseCase loginUseCase(UserRepositoryPort users, PasswordHasherPort passwordHasher,
			TokenProviderPort tokenProvider) {
		return new LoginService(users, passwordHasher, tokenProvider);
	}

	@Bean
	CreateEntryUseCase createEntryUseCase(JournalEntryRepositoryPort entries) {
		return new CreateEntryService(entries, Clock.systemUTC());
	}

	@Bean
	ListEntriesUseCase listEntriesUseCase(JournalEntryRepositoryPort entries) {
		return new ListEntriesService(entries);
	}

	@Bean
	GetEntryUseCase getEntryUseCase(JournalEntryRepositoryPort entries) {
		return new GetEntryService(entries);
	}

	@Bean
	DeleteEntryUseCase deleteEntryUseCase(JournalEntryRepositoryPort entries) {
		return new DeleteEntryService(entries);
	}
}
