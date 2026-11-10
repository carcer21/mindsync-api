package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.GetUserUseCase;
import com.carlos.mindsync.application.port.in.LoginUseCase;
import com.carlos.mindsync.application.port.in.RegisterUserUseCase;
import com.carlos.mindsync.domain.exception.EmailAlreadyExistsException;
import com.carlos.mindsync.domain.exception.InvalidCredentialsException;
import com.carlos.mindsync.domain.exception.InvalidUserException;
import com.carlos.mindsync.domain.model.User;
import com.carlos.mindsync.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, UserController.class})
@Import(SecurityConfig.class)
class AuthControllerTest {

	private static final String VALID_REGISTER = """
			{"name":"Ana","email":"ana@example.com","password":"s3cret-Passw0rd"}""";

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private RegisterUserUseCase registerUser;
	@MockitoBean
	private LoginUseCase login;
	@MockitoBean
	private GetUserUseCase getUser;
	// Required by the resource-server configuration; the real one is tested in SecurityConfigTest.
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void register_returns201WithUserAndNoPassword() throws Exception {
		User ana = User.register("Ana", "ana@example.com", "hashed-value");
		when(registerUser.register(any())).thenReturn(ana);

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTER))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(ana.id().toString()))
				.andExpect(jsonPath("$.name").value("Ana"))
				.andExpect(jsonPath("$.email").value("ana@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"name\":\"\",\"email\":\"ana@example.com\",\"password\":\"s3cret-Passw0rd\"}",
			"{\"email\":\"ana@example.com\",\"password\":\"s3cret-Passw0rd\"}",
			"{\"name\":\"Ana\",\"email\":\"not-an-email\",\"password\":\"s3cret-Passw0rd\"}",
			"{\"name\":\"Ana\",\"email\":\"\",\"password\":\"s3cret-Passw0rd\"}",
			"{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"short\"}",
			"{\"name\":\"Ana\",\"email\":\"ana@example.com\"}",
			"{}",
			"not json at all",
			""
	})
	void register_withInvalidBody_returns400_andNeverReachesTheUseCase(String body) throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

		verifyNoInteractions(registerUser);
	}

	@Test
	void register_withPasswordOver72Characters_returns400() throws Exception {
		String body = "{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"" + "a".repeat(73) + "\"}";

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(registerUser);
	}

	@Test
	void register_withoutContentType_returns415() throws Exception {
		mockMvc.perform(post("/api/auth/register").content(VALID_REGISTER)).andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void register_whenDomainRejectsData_returns400() throws Exception {
		when(registerUser.register(any())).thenThrow(new InvalidUserException("name is required"));

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTER))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("name is required"));
	}

	@Test
	void register_withExistingEmail_returns409() throws Exception {
		when(registerUser.register(any())).thenThrow(new EmailAlreadyExistsException());

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTER))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void register_withUnexpectedFailure_returns500_withoutLeakingDetails() throws Exception {
		when(registerUser.register(any())).thenThrow(new IllegalStateException("jdbc:postgresql://secret-host password=hunter2"));

		String body = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
						.content(VALID_REGISTER))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value("unexpected error"))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain("hunter2").doesNotContain("secret-host");
	}

	@Test
	void login_returns200WithBearerToken() throws Exception {
		when(login.login(any())).thenReturn("signed-token");

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"ana@example.com\",\"password\":\"s3cret-Passw0rd\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("signed-token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"));
	}

	@Test
	void login_withWrongCredentials_returns401() throws Exception {
		when(login.login(any())).thenThrow(new InvalidCredentialsException());

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"ana@example.com\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("invalid email or password"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"email\":\"\",\"password\":\"s3cret-Passw0rd\"}",
			"{\"email\":\"ana@example.com\",\"password\":\"\"}",
			"{\"password\":\"s3cret-Passw0rd\"}",
			"{}"
	})
	void login_withMissingFields_returns400(String body) throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(login);
	}
}
