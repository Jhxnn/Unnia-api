package com.Unnia.identity.infrastructure.web;

import com.Unnia.identity.application.usecase.AuthenticateUseCase;
import com.Unnia.identity.application.usecase.ChangePasswordUseCase;
import com.Unnia.identity.application.usecase.EndSessionUseCase;
import com.Unnia.identity.application.usecase.RefreshSessionUseCase;
import com.Unnia.identity.application.usecase.RegisterNgoUseCase;
import com.Unnia.identity.application.usecase.RegisterVolunteerUseCase;
import com.Unnia.identity.application.usecase.ResendEmailVerificationUseCase;
import com.Unnia.identity.application.usecase.VerifyEmailUseCase;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.ServiceUnavailableException;
import com.Unnia.identity.application.usecase.StartSessionUseCase;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.infrastructure.security.JwtService;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.application.UnauthorizedException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.infrastructure.web.IdentifierJacksonModule;
import com.Unnia.shared.infrastructure.web.RestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final Instant REFRESH_EXPIRES = NOW.plus(Duration.ofDays(30));

	private RegisterNgoUseCase registerNgo;
	private AuthenticateUseCase authenticate;
	private StartSessionUseCase startSession;
	private RefreshSessionUseCase refreshSession;
	private EndSessionUseCase endSession;
	private VerifyEmailUseCase verifyEmail;
	private ResendEmailVerificationUseCase resendVerification;
	private JwtService jwt;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		registerNgo = mock(RegisterNgoUseCase.class);
		authenticate = mock(AuthenticateUseCase.class);
		startSession = mock(StartSessionUseCase.class);
		refreshSession = mock(RefreshSessionUseCase.class);
		endSession = mock(EndSessionUseCase.class);
		verifyEmail = mock(VerifyEmailUseCase.class);
		resendVerification = mock(ResendEmailVerificationUseCase.class);
		jwt = new JwtService("a-secret-with-clearly-more-than-32-chars", Duration.ofMinutes(15),
				new MutableClock(NOW));
		AuthController controller = new AuthController(registerNgo, mock(RegisterVolunteerUseCase.class),
				authenticate, mock(ChangePasswordUseCase.class), startSession, refreshSession, endSession,
				verifyEmail, resendVerification, jwt);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		JsonMapper.Builder mapper = JsonMapper.builder().addModule(new IdentifierJacksonModule());
		mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new RestExceptionHandler())
				.setValidator(validator).setMessageConverters(new JacksonJsonHttpMessageConverter(mapper)).build();
	}

	private static final String NGO_BODY = """
			{"email":"ong@exemplo.com","password":"senha1234","cnpj":"11.222.333/0001-81",
			 "name":"Mãos Solidárias","description":"Ajudamos","city":"Recife"}""";

	private static final String LOGIN_BODY = "{\"email\":\"vol@exemplo.com\",\"password\":\"senha1234\"}";

	@Test
	void verifyingTheEmailPassesTheTokenAndReturnsNoContent() throws Exception {
		mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"abc123\"}")).andExpect(status().isNoContent());

		verify(verifyEmail).execute(new VerifyEmailUseCase.Command("abc123"));
	}

	@Test
	void anInvalidLinkBecomesUnprocessableEntityWithTheReason() throws Exception {
		doThrow(new DomainException("Link de confirmação inválido ou expirado")).when(verifyEmail).execute(any());

		mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"velho\"}")).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Link de confirmação inválido ou expirado"));
	}

	@Test
	void verifyingWithoutATokenIsABadRequest() throws Exception {
		mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());

		verify(verifyEmail, never()).execute(any());
	}

	@Test
	void resendingAlwaysAnswersNoContent() throws Exception {
		mvc.perform(post("/api/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"qualquer@exemplo.com\"}")).andExpect(status().isNoContent());

		verify(resendVerification).execute(new ResendEmailVerificationUseCase.Command("qualquer@exemplo.com"));
	}

	@Test
	void whenTheMailServerIsDownResendingReportsServiceUnavailable() throws Exception {
		doThrow(new ServiceUnavailableException("Não foi possível enviar o e-mail agora.")).when(resendVerification)
				.execute(any());

		mvc.perform(post("/api/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"qualquer@exemplo.com\"}")).andExpect(status().isServiceUnavailable());
	}

	@Test
	void loginWithAnUnconfirmedEmailIsForbiddenWithTheReason() throws Exception {
		when(authenticate.execute(any())).thenThrow(new ForbiddenException(AuthenticateUseCase.EMAIL_NOT_VERIFIED));

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value(AuthenticateUseCase.EMAIL_NOT_VERIFIED));
	}

	@Test
	void registeringANgoReturnsCreatedWithPlainStringIds() throws Exception {
		AccountId account = AccountId.generate();
		NgoId ngo = NgoId.generate();
		when(registerNgo.execute(any())).thenReturn(new RegisterNgoUseCase.Result(account, ngo));

		mvc.perform(post("/api/auth/register/ngo").contentType(MediaType.APPLICATION_JSON).content(NGO_BODY))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.accountId").value(account.value().toString()))
				.andExpect(jsonPath("$.ngoId").value(ngo.value().toString()));
	}

	@Test
	void invalidRegistrationNeverReachesTheUseCase() throws Exception {
		mvc.perform(post("/api/auth/register/ngo").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"nao-e-email\",\"password\":\"senha1234\",\"cnpj\":\"1\",\"name\":\"\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.email").exists())
				.andExpect(jsonPath("$.fields.name").exists()).andExpect(jsonPath("$.fields.city").exists());

		verify(registerNgo, never()).execute(any());
	}

	@Test
	void domainRuleViolationsBecomeUnprocessableEntity() throws Exception {
		when(registerNgo.execute(any())).thenThrow(new DomainException("CNPJ inválido"));

		mvc.perform(post("/api/auth/register/ngo").contentType(MediaType.APPLICATION_JSON).content(NGO_BODY))
				.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.detail").value("CNPJ inválido"));
	}

	@Test
	void loginReturnsAShortAccessTokenAndARefreshToken() throws Exception {
		AccountId account = AccountId.generate();
		when(authenticate.execute(any())).thenReturn(new AuthenticateUseCase.Result(account, Role.VOLUNTEER));
		when(startSession.execute(account)).thenReturn(new StartSessionUseCase.Result("refresh-1", REFRESH_EXPIRES));

		String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(LOGIN_BODY)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("VOLUNTEER"))
				.andExpect(jsonPath("$.accountId").value(account.value().toString()))
				.andExpect(jsonPath("$.refreshToken").value("refresh-1")).andReturn().getResponse()
				.getContentAsString();

		String token = JsonMapper.builder().build().readTree(response).get("token").asString();
		assertThat(jwt.parse(token)).hasValueSatisfying(user -> {
			assertThat(user.accountId()).isEqualTo(account);
			assertThat(user.role()).isEqualTo(Role.VOLUNTEER);
		});
	}

	@Test
	void badCredentialsAreUnauthorizedAndNoSessionIsStarted() throws Exception {
		when(authenticate.execute(any())).thenThrow(new UnauthorizedException("Credenciais inválidas"));

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"vol@exemplo.com\",\"password\":\"errada\"}"))
				.andExpect(status().isUnauthorized());

		verify(startSession, never()).execute(any());
	}

	@Test
	void refreshingReturnsANewPairWithAFreshAccessToken() throws Exception {
		AccountId account = AccountId.generate();
		when(refreshSession.execute("refresh-1")).thenReturn(
				new RefreshSessionUseCase.Refreshed(account, Role.NGO, "refresh-2", REFRESH_EXPIRES));

		String response = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"refresh-1\"}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.refreshToken").value("refresh-2")).andExpect(jsonPath("$.role").value("NGO"))
				.andReturn().getResponse().getContentAsString();

		String token = JsonMapper.builder().build().readTree(response).get("token").asString();
		assertThat(jwt.parse(token)).hasValueSatisfying(user -> assertThat(user.accountId()).isEqualTo(account));
	}

	@Test
	void aRejectedRefreshIsUnauthorized() throws Exception {
		when(refreshSession.execute(any())).thenReturn(new RefreshSessionUseCase.Rejected());

		mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"usado\"}")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Sessão inválida ou expirada"));
	}

	@Test
	void refreshAndLogoutRequireATokenInTheBody() throws Exception {
		mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\" \"}")).andExpect(status().isBadRequest());

		verify(refreshSession, never()).execute(any());
		verify(endSession, never()).execute(any());
	}

	@Test
	void logoutEndsTheSession() throws Exception {
		mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"refresh-1\"}")).andExpect(status().isNoContent());

		verify(endSession).execute("refresh-1");
	}
}
