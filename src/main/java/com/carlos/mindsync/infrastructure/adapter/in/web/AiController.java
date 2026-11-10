package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.GetAdviceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "AI advice")
@RestController
@RequestMapping("/api/ai")
class AiController {

	private final GetAdviceUseCase getAdvice;

	AiController(GetAdviceUseCase getAdvice) {
		this.getAdvice = getAdvice;
	}

	// The analysed entries are always the token owner's; the client cannot choose whose.
	@Operation(summary = "AI advice based on my latest 14 entries")
	@GetMapping("/advice")
	AdviceResponse advice(@AuthenticationPrincipal Jwt jwt) {
		return new AdviceResponse(getAdvice.getAdvice(UUID.fromString(jwt.getSubject())));
	}
}
