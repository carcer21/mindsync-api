package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.domain.model.Emotion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

// No owner field on purpose: the owner always comes from the token.
record EntryRequest(
		@NotNull LocalDate date,
		@NotNull @Min(1) @Max(10) Integer energy,
		@NotNull Emotion emotion,
		@Size(max = 2000) String notes,
		@Size(max = 10) Set<@NotBlank @Size(max = 30) String> tags) {

	// Notes are health data: keep them out of logs.
	@Override
	public String toString() {
		return "EntryRequest[date=" + date + "]";
	}
}
