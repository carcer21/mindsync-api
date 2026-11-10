package com.carlos.mindsync.infrastructure.adapter.out.ai;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carlos.mindsync.domain.model.Emotion;
import com.carlos.mindsync.domain.model.JournalEntry;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// No real call to Gemini: a fake HTTP server (MockRestServiceServer) plays its role.
class GeminiAdapterTest {

	private static final String API_KEY = "fake-test-key-123";
	private static final String URL =
			"https://gemini.test/v1beta/models/gemini-test-model:generateContent";
	private static final GeminiProperties PROPERTIES = new GeminiProperties(API_KEY, "gemini-test-model",
			"https://gemini.test", Duration.ofSeconds(1), Duration.ofSeconds(1));
	private static final String OK_BODY = """
			{"candidates":[{"content":{"role":"model","parts":[{"text":"  Buen trabajo registrando tu día.  "}]},
			"finishReason":"STOP"}],"usageMetadata":{"totalTokenCount":42},"modelVersion":"x"}""";

	private static final JournalEntry ENTRY_1 = JournalEntry.create(java.util.UUID.randomUUID(),
			LocalDate.of(2026, 3, 9), 3, Emotion.SADNESS, "invented private note", List.of("work"));
	private static final JournalEntry ENTRY_2 = JournalEntry.create(ENTRY_1.userId(),
			LocalDate.of(2026, 3, 10), 7, Emotion.JOY, null, null);

	private MockRestServiceServer server;
	private GeminiAdapter adapter;
	private ListAppender<ILoggingEvent> logs;
	private Logger adapterLogger;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new GeminiAdapter(builder.build(), PROPERTIES);
		adapterLogger = (Logger) LoggerFactory.getLogger(GeminiAdapter.class);
		logs = new ListAppender<>();
		logs.start();
		adapterLogger.addAppender(logs);
	}

	@AfterEach
	void tearDown() {
		adapterLogger.detachAppender(logs);
	}

	@Test
	void getAdvice_sendsKeyInHeader_notInUrl_andReturnsTrimmedText() {
		server.expect(requestTo(URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-goog-api-key", API_KEY))
				.andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
				.andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

		String advice = adapter.getAdvice(List.of(ENTRY_1, ENTRY_2));

		assertThat(advice).isEqualTo("Buen trabajo registrando tu día.");
		server.verify();
	}

	@Test
	void getAdvice_sendsSafetyRules_entriesInOrder_andNoIds() {
		server.expect(requestTo(URL))
				.andExpect(content().string(containsString("systemInstruction")))
				.andExpect(content().string(containsString("REGLA ESTRICTA E INQUEBRANTABLE")))
				.andExpect(content().string(containsString("sustituye el diagn")))
				.andExpect(content().string(containsString("Fecha: 2026-03-09")))
				.andExpect(content().string(containsString("invented private note")))
				.andExpect(content().string(containsString("maxOutputTokens")))
				.andExpect(content().string(not(containsString(ENTRY_1.id().toString()))))
				.andExpect(content().string(not(containsString(ENTRY_1.userId().toString()))))
				.andExpect(content().string(not(containsString(API_KEY))))
				.andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

		adapter.getAdvice(List.of(ENTRY_2, ENTRY_1));

		server.verify();
	}

	@Test
	void getAdvice_ignoresUnknownFieldsInTheResponse() {
		server.expect(requestTo(URL)).andRespond(withSuccess(
				"{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\",\"thought\":false}]},\"index\":0}],"
						+ "\"somethingNew\":{\"a\":1}}", MediaType.APPLICATION_JSON));

		assertThat(adapter.getAdvice(List.of(ENTRY_1))).isEqualTo("ok");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{}",
			"{\"candidates\":[]}",
			"{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}",
			"{\"candidates\":[{\"content\":{\"parts\":[]}}]}",
			"{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"   \"}]}}]}",
			"{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}",
			"not json at all",
			"" })
	void getAdvice_unusableResponse_returnsFallback(String body) {
		server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

		assertThat(adapter.getAdvice(List.of(ENTRY_1))).isEqualTo(GeminiAdapter.FALLBACK_ADVICE);
	}

	@ParameterizedTest
	@ValueSource(ints = { 400, 401, 403, 404, 429, 500, 503 })
	void getAdvice_httpError_returnsFallback(int status) {
		server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status)));

		assertThat(adapter.getAdvice(List.of(ENTRY_1))).isEqualTo(GeminiAdapter.FALLBACK_ADVICE);
	}

	@Test
	void getAdvice_withoutApiKey_returnsFallback_andMakesNoRequest() {
		GeminiProperties noKey = new GeminiProperties("  ", "m", "https://gemini.test", Duration.ofSeconds(1),
				Duration.ofSeconds(1));
		RestClient.Builder builder = RestClient.builder().baseUrl(noKey.baseUrl());
		MockRestServiceServer strict = MockRestServiceServer.bindTo(builder).build();

		assertThat(new GeminiAdapter(builder.build(), noKey).getAdvice(List.of(ENTRY_1)))
				.isEqualTo(GeminiAdapter.FALLBACK_ADVICE);

		strict.verify(); // no expectations were set, so any request would have failed
	}

	@Test
	void getAdvice_noEntries_returnsFallback_andMakesNoRequest() {
		assertThat(adapter.getAdvice(List.of())).isEqualTo(GeminiAdapter.FALLBACK_ADVICE);

		server.verify();
	}

	@Test
	void fallback_remindsThatMindSyncDoesNotReplaceAProfessional() {
		assertThat(GeminiAdapter.FALLBACK_ADVICE).contains("en ningún caso sustituye").doesNotContain("paciente");
	}

	@Test
	void failures_areLogged_withoutKeyNotesOrBody() {
		server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("echo invented private note " + API_KEY));

		adapter.getAdvice(List.of(ENTRY_1));

		assertThat(logs.list).isNotEmpty();
		assertThat(logs.list).allSatisfy(event -> assertThat(event.getFormattedMessage())
				.doesNotContain(API_KEY).doesNotContain("invented private note"));
		assertThat(logs.list).noneMatch(event -> event.getThrowableProxy() != null);
	}

	@Test
	void properties_toString_hidesTheKey() {
		assertThat(PROPERTIES.toString()).doesNotContain(API_KEY);
	}

	// ---- timeouts, through the real RestClient built by GeminiConfig

	@Test
	void slowServer_hitsTheReadTimeout_andReturnsFallbackQuickly() throws Exception {
		HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		slow.createContext("/", exchange -> {
			try {
				Thread.sleep(3000);
				exchange.sendResponseHeaders(200, -1);
			} catch (Exception ignored) {
				// client already gave up
			} finally {
				exchange.close();
			}
		});
		slow.start();
		try {
			GeminiProperties props = new GeminiProperties(API_KEY, "m",
					"http://127.0.0.1:" + slow.getAddress().getPort(), Duration.ofMillis(500), Duration.ofMillis(300));
			var port = new GeminiConfig().aiAdvisorPort(props);

			long start = System.nanoTime();
			String advice = port.getAdvice(List.of(ENTRY_1));
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;

			assertThat(advice).isEqualTo(GeminiAdapter.FALLBACK_ADVICE);
			assertThat(elapsedMs).isLessThan(2500);
		} finally {
			slow.stop(0);
		}
	}
}
