package com.carlos.mindsync.infrastructure.config;

import com.carlos.mindsync.application.port.in.GetUserUseCase;
import com.carlos.mindsync.application.port.in.LoginUseCase;
import com.carlos.mindsync.application.port.in.RegisterUserUseCase;
import com.carlos.mindsync.application.port.out.PasswordHasherPort;
import com.carlos.mindsync.application.port.out.TokenProviderPort;
import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.application.service.GetUserService;
import com.carlos.mindsync.application.service.LoginService;
import com.carlos.mindsync.application.service.RegisterUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
