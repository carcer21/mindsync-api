package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.DeleteAccountUseCase;
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
import org.springframework.test.context.ActiveProfiles;
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
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, UserController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

	private static final User ANA = User.register("Ana", "ana@example.com", "hashed-value");

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private GetUserUseCase getUser;
	@MockitoBean
	private DeleteAccountUseCase deleteAccount;
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
	void me_isNotReachableWithPostOrPut() throws Exception {
		mockMvc.perform(post("/api/users/me").with(jwt().jwt(token -> token.subject(ANA.id().toString()))))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(put("/api/users/me").with(jwt().jwt(token -> token.subject(ANA.id().toString()))))
				.andExpect(status().isMethodNotAllowed());

		verify(getUser, never()).getById(any());
		verifyNoInteractions(deleteAccount);
	}

	// ---- delete my account

	@Test
	void deleteMe_withoutToken_returns401_andNothingIsDeleted() throws Exception {
		mockMvc.perform(delete("/api/users/me")).andExpect(status().isUnauthorized());

		verifyNoInteractions(deleteAccount);
	}

	@Test
	void deleteMe_returns204_deletingOnlyTheTokenOwner() throws Exception {
		UUID someoneElse = UUID.randomUUID();

		mockMvc.perform(delete("/api/users/me").param("id", someoneElse.toString())
						.header("X-User-Id", someoneElse.toString())
						.with(jwt().jwt(token -> token.subject(ANA.id().toString()))))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(deleteAccount).delete(ANA.id());
		verify(deleteAccount, never()).delete(someoneElse);
	}

	@Test
	void deleteMe_whenTheAccountIsAlreadyGone_returns404() throws Exception {
		doThrow(new UserNotFoundException()).when(deleteAccount).delete(ANA.id());

		mockMvc.perform(delete("/api/users/me").with(jwt().jwt(token -> token.subject(ANA.id().toString()))))
				.andExpect(status().isNotFound());
	}

	@Test
	void thereIsNoEndpointToDeleteAUserById() throws Exception {
		mockMvc.perform(delete("/api/users/" + UUID.randomUUID())
						.with(jwt().jwt(token -> token.subject(ANA.id().toString()))))
				.andExpect(status().isNotFound());

		verifyNoInteractions(deleteAccount);
	}
}
