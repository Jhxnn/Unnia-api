package com.Unnia.identity.infrastructure.web;

import com.Unnia.identity.application.usecase.AuthenticateUseCase;
import com.Unnia.identity.application.usecase.ChangePasswordUseCase;
import com.Unnia.identity.application.usecase.RegisterNgoUseCase;
import com.Unnia.identity.application.usecase.RegisterVolunteerUseCase;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

	private RegisterNgoUseCase registerNgo;
	private RegisterVolunteerUseCase registerVolunteer;
	private AuthenticateUseCase authenticate;
	private JwtService jwt;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		registerNgo = mock(RegisterNgoUseCase.class);
		registerVolunteer = mock(RegisterVolunteerUseCase.class);
		authenticate = mock(AuthenticateUseCase.class);
		jwt = new JwtService("a-secret-with-clearly-more-than-32-chars", Duration.ofHours(2), new MutableClock(Instant.now()));
		AuthController controller = new AuthController(registerNgo, registerVolunteer, authenticate,
				mock(ChangePasswordUseCase.class), jwt);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		JsonMapper.Builder mapper = JsonMapper.builder().addModule(new IdentifierJacksonModule());
		mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new RestExceptionHandler())
				.setValidator(validator).setMessageConverters(new JacksonJsonHttpMessageConverter(mapper)).build();
	}

	private static final String NGO_BODY = """
			{"email":"ong@exemplo.com","password":"senha1234","cnpj":"11.222.333/0001-81",
			 "name":"Mãos Solidárias","description":"Ajudamos","city":"Recife"}""";

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
	void loginReturnsATokenThatCarriesTheAccountAndRole() throws Exception {
		AccountId account = AccountId.generate();
		when(authenticate.execute(any())).thenReturn(new AuthenticateUseCase.Result(account, Role.VOLUNTEER));

		String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"vol@exemplo.com\",\"password\":\"senha1234\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.role").value("VOLUNTEER"))
				.andExpect(jsonPath("$.accountId").value(account.value().toString())).andReturn().getResponse()
				.getContentAsString();

		String token = JsonMapper.builder().build().readTree(response).get("token").asString();
		assertThat(jwt.parse(token)).hasValueSatisfying(user -> {
			assertThat(user.accountId()).isEqualTo(account);
			assertThat(user.role()).isEqualTo(Role.VOLUNTEER);
		});
	}

	@Test
	void badCredentialsAreUnauthorized() throws Exception {
		when(authenticate.execute(any())).thenThrow(new UnauthorizedException("Credenciais inválidas"));

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"vol@exemplo.com\",\"password\":\"errada\"}"))
				.andExpect(status().isUnauthorized());
	}
}
