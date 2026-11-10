package com.carlos.mindsync.infrastructure.adapter.out.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Only the fields we use; anything else Gemini adds is ignored.
@JsonIgnoreProperties(ignoreUnknown = true)
record GeminiResponse(List<Candidate> candidates) {

	String firstText() {
		if (candidates == null || candidates.isEmpty() || candidates.get(0) == null
				|| candidates.get(0).content() == null || candidates.get(0).content().parts() == null) {
			return null;
		}
		StringBuilder text = new StringBuilder();
		candidates.get(0).content().parts().forEach(part -> {
			if (part != null && part.text() != null) {
				text.append(part.text());
			}
		});
		return text.toString();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Candidate(Content content) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Content(List<Part> parts) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Part(String text) {
	}
}
