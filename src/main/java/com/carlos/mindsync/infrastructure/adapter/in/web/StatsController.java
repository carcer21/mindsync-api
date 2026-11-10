package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.GetStatsUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/stats")
class StatsController {

	private final GetStatsUseCase getStats;

	StatsController(GetStatsUseCase getStats) {
		this.getStats = getStats;
	}

	// "me": the statistics are always the token owner's, there is no way to ask for someone else's.
	@GetMapping("/me")
	StatsResponse me(@AuthenticationPrincipal Jwt jwt) {
		return StatsResponse.from(getStats.getStats(UUID.fromString(jwt.getSubject())),
				GetStatsUseCase.PERIOD_DAYS);
	}
}
