package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.GetAdviceUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
class AiController {

	private final GetAdviceUseCase getAdvice;

	AiController(GetAdviceUseCase getAdvice) {
		this.getAdvice = getAdvice;
	}

	// The analysed entries are always the token owner's; the client cannot choose whose.
	@GetMapping("/advice")
	AdviceResponse advice(@AuthenticationPrincipal Jwt jwt) {
		return new AdviceResponse(getAdvice.getAdvice(UUID.fromString(jwt.getSubject())));
	}
}
