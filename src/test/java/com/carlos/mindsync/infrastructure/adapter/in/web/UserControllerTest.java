package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.GetUserUseCase;
import com.carlos.mindsync.application.port.in.LoginUseCase;
import com.carlos.mindsync.application.port.in.RegisterUserUseCase;
import com.carlos.mindsync.domain.exception.UserNotFoundException;
import com.carlos.mindsync.domain.model.User;
import com.carlos.mindsync.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, UserController.class})
@Import(SecurityConfig.class)
class UserControllerTest {

	private static final User ANA = User.register("Ana", "ana@example.com", "hashed-value");

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private GetUserUseCase getUser;
	@MockitoBean
	private RegisterUserUseCase registerUser;
	@MockitoBean
	private LoginUseCase login;
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void me_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());

		verifyNoInteractions(getUser);
	}

	@Test
	void me_returnsTheUserFromTheTokenSubject_withoutPassword() throws Exception {
		when(getUser.getById(ANA.id())).thenReturn(ANA);

		mockMvc.perform(get("/api/users/me").with(jwt().jwt(token -> token.subject(ANA.id().toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ANA.id().toString()))
				.andExpect(jsonPath("$.email").value("ana@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void me_ignoresAnyIdSentByTheClient() throws Exception {
		UUID someoneElse = UUID.randomUUID();
		when(getUser.getById(ANA.id())).thenReturn(ANA);

		mockMvc.perform(get("/api/users/me").param("id", someoneElse.toString())
						.header("X-User-Id", someoneElse.toString())
						.with(jwt().jwt(token -> token.subject(ANA.id().toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ANA.id().toString()));

		verify(getUser, never()).getById(someoneElse);
	}

	@Test
	void me_whenTheUserNoLongerExists_returns404() throws Exception {
		UUID deleted = UUID.randomUUID();
		when(getUser.getById(deleted)).thenThrow(new UserNotFoundException());

		mockMvc.perform(get("/api/users/me").with(jwt().jwt(token -> token.subject(deleted.toString()))))
				.andExpect(status().isNotFound());
	}

	@Test
	void me_isNotReachableWithPostOrDelete() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/users/me")
						.with(jwt().jwt(token -> token.subject(ANA.id().toString()))))
				.andExpect(status().isMethodNotAllowed());

		verify(getUser, never()).getById(any());
	}
}
