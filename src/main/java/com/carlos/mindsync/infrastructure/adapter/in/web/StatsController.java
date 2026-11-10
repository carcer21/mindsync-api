package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.GetStatsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Statistics")
@RestController
@RequestMapping("/api/stats")
class StatsController {

	private final GetStatsUseCase getStats;

	StatsController(GetStatsUseCase getStats) {
		this.getStats = getStats;
	}

	// "me": the statistics are always the token owner's, there is no way to ask for someone else's.
	@Operation(summary = "My statistics over the last 90 days")
	@GetMapping("/me")
	StatsResponse me(@AuthenticationPrincipal Jwt jwt) {
		return StatsResponse.from(getStats.getStats(UUID.fromString(jwt.getSubject())),
				GetStatsUseCase.PERIOD_DAYS);
	}
}
