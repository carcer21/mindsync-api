package com.carlos.mindsync.infrastructure.adapter.out.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.gemini")
public record GeminiProperties(String apiKey, String model, String baseUrl, Duration connectTimeout,
		Duration readTimeout) {

	boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}

	// Never print the key.
	@Override
	public String toString() {
		return "GeminiProperties[model=" + model + ", baseUrl=" + baseUrl + "]";
	}
}
