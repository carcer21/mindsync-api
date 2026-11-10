package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "app.gemini.api-key=", "app.gemini.base-url=http://127.0.0.1:1" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OpenApiDocsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private static String docs;

	private String docs() throws Exception {
		if (docs == null) {
			docs = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
		}
		return docs;
	}

	@Test
	void docsAndSwaggerUi_arePublic() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
	}

	@Test
	void documentsEveryEndpoint() throws Exception {
		Map<String, Object> paths = JsonPath.read(docs(), "$.paths");

		assertThat(paths.keySet()).containsExactlyInAnyOrder("/api/auth/register", "/api/auth/login",
				"/api/users/me", "/api/entries", "/api/entries/{id}", "/api/stats/me", "/api/ai/advice");
		assertThat(JsonPath.<Map<String, Object>>read(docs(), "$.paths['/api/entries']").keySet())
				.containsExactlyInAnyOrder("get", "post");
		assertThat(JsonPath.<Map<String, Object>>read(docs(), "$.paths['/api/entries/{id}']").keySet())
				.containsExactlyInAnyOrder("get", "delete");
	}

	@Test
	void declaresTheBearerJwtSecurityScheme_requiredByDefault() throws Exception {
		assertThat(JsonPath.<String>read(docs(), "$.components.securitySchemes.bearerAuth.type")).isEqualTo("http");
		assertThat(JsonPath.<String>read(docs(), "$.components.securitySchemes.bearerAuth.scheme")).isEqualTo("bearer");
		assertThat(JsonPath.<String>read(docs(), "$.components.securitySchemes.bearerAuth.bearerFormat")).isEqualTo("JWT");
		assertThat(JsonPath.<List<Map<String, Object>>>read(docs(), "$.security")).hasSize(1);
		assertThat(JsonPath.<List<Map<String, Object>>>read(docs(), "$.security")).first()
				.satisfies(requirement -> assertThat(requirement).containsKey("bearerAuth"));
	}

	@Test
	void loginAndRegister_areMarkedPublic_theOthersInheritTheTokenRequirement() throws Exception {
		assertThat(JsonPath.<List<Object>>read(docs(), "$.paths['/api/auth/login'].post.security")).isEmpty();
		assertThat(JsonPath.<List<Object>>read(docs(), "$.paths['/api/auth/register'].post.security")).isEmpty();
		// Operations without their own "security" use the global requirement (token needed).
		assertThat(JsonPath.<Map<String, Object>>read(docs(), "$.paths['/api/entries'].get")).doesNotContainKey("security");
		assertThat(JsonPath.<Map<String, Object>>read(docs(), "$.paths['/api/ai/advice'].get")).doesNotContainKey("security");
	}

	@Test
	void theAuthenticatedPrincipalIsNotExposedAsAClientParameter() throws Exception {
		// The owner comes from the token, so no endpoint may ask the client for a user id or a Jwt.
		assertThat(docs()).doesNotContain("\"name\":\"jwt\"").doesNotContain("\"name\":\"userId\"");
		assertThat(JsonPath.<Map<String, Object>>read(docs(), "$.paths['/api/users/me'].get")).doesNotContainKey("parameters");
		assertThat(JsonPath.<Map<String, Object>>read(docs(), "$.paths['/api/stats/me'].get")).doesNotContainKey("parameters");
	}

	@Test
	void schemasNeverExposeSecretsOrTheOwnerId() throws Exception {
		assertThat(docs()).doesNotContain("passwordHash");
		assertThat(JsonPath.<Map<String, Object>>read(docs(), "$.components.schemas.UserResponse.properties").keySet())
				.containsExactlyInAnyOrder("id", "name", "email");
		assertThat(JsonPath.<Map<String, Object>>read(docs(), "$.components.schemas.EntryRequest.properties").keySet())
				.doesNotContain("userId", "id");
		assertThat(JsonPath.<Map<String, Object>>read(docs(), "$.components.schemas.EntryResponse.properties").keySet())
				.doesNotContain("userId");
	}

	@Test
	void everyOperationHasASummary() throws Exception {
		for (String path : JsonPath.<Map<String, Object>>read(docs(), "$.paths").keySet()) {
			Map<String, Map<String, Object>> operations = JsonPath.read(docs(), "$.paths['" + path + "']");
			operations.forEach((method, operation) ->
					assertThat(operation.get("summary")).as(method + " " + path).isNotNull());
		}
	}
}
