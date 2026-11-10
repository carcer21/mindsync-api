package com.carlos.mindsync.infrastructure.adapter.out.ai;

import com.carlos.mindsync.application.port.out.AiAdvisorPort;
import com.carlos.mindsync.domain.model.JournalEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

class GeminiAdapter implements AiAdvisorPort {

	static final String FALLBACK_ADVICE = "Ahora mismo no podemos generar tu análisis. Inténtalo de nuevo "
			+ "más tarde. Recuerda que MindSync es una herramienta de apoyo y en ningún caso sustituye el "
			+ "diagnóstico, consejo o tratamiento de un profesional de la salud; si estás pasando por un "
			+ "momento difícil, habla con un profesional.";

	private static final Logger log = LoggerFactory.getLogger(GeminiAdapter.class);

	private final RestClient client;
	private final GeminiProperties properties;

	GeminiAdapter(RestClient client, GeminiProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	@Override
	public String getAdvice(List<JournalEntry> entries) {
		if (entries.isEmpty()) {
			return FALLBACK_ADVICE;
		}
		if (!properties.hasApiKey()) {
			log.warn("Gemini API key is not configured; returning fallback advice");
			return FALLBACK_ADVICE;
		}
		long start = System.nanoTime();
		try {
			GeminiResponse response = client.post()
					.uri("/v1beta/models/{model}:generateContent", properties.model())
					.header("x-goog-api-key", properties.apiKey())
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody(entries))
					.retrieve()
					.body(GeminiResponse.class);
			String text = response == null ? null : response.firstText();
			if (text == null || text.isBlank()) {
				// Blocked by safety filters, no candidates, or an empty answer.
				log.warn("Gemini returned no usable text (entries={}); returning fallback advice", entries.size());
				return FALLBACK_ADVICE;
			}
			log.debug("Gemini advice generated (entries={}, ms={})", entries.size(), elapsedMs(start));
			return text.strip();
		} catch (RuntimeException e) {
			// Log only the exception type: messages and bodies could echo health data or the key.
			log.warn("Gemini call failed ({}, ms={}); returning fallback advice", e.getClass().getSimpleName(),
					elapsedMs(start));
			return FALLBACK_ADVICE;
		}
	}

	private static Map<String, Object> requestBody(List<JournalEntry> entries) {
		return Map.of(
				"systemInstruction", Map.of("parts", List.of(Map.of("text", AdvicePrompt.SYSTEM_INSTRUCTION))),
				"contents", List.of(Map.of("role", "user",
						"parts", List.of(Map.of("text", AdvicePrompt.userContent(entries))))),
				"generationConfig", Map.of("maxOutputTokens", 1024));
	}

	private static long elapsedMs(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}
}
