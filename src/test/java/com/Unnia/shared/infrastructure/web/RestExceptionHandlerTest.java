package com.Unnia.shared.infrastructure.web;

import com.Unnia.shared.application.ConflictException;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.application.TooManyRequestsException;
import com.Unnia.shared.application.UnauthorizedException;
import com.Unnia.shared.domain.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionHandlerTest {

	record Body(@NotBlank String name) {
	}

	@RestController
	static class Stub {

		@GetMapping("/not-found")
		void notFound() {
			throw new NotFoundException("Evento não encontrado");
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new ConflictException("E-mail já cadastrado");
		}

		@GetMapping("/unauthorized")
		void unauthorized() {
			throw new UnauthorizedException("Credenciais inválidas");
		}

		@GetMapping("/too-many")
		void tooMany() {
			throw new TooManyRequestsException("Muitas tentativas", Duration.ofSeconds(90));
		}

		@GetMapping("/forbidden")
		void forbidden() {
			throw new ForbiddenException("Acesso restrito");
		}

		@GetMapping("/domain")
		void domain() {
			throw new DomainException("Evento lotado");
		}

		@GetMapping("/optimistic")
		void optimistic() {
			throw new OptimisticLockingFailureException("versão antiga");
		}

		@GetMapping("/integrity")
		void integrity() {
			throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
		}

		@GetMapping("/uuid/{id}")
		void uuid(@PathVariable UUID id) {
		}

		@PostMapping("/body")
		void body(@Valid @RequestBody Body body) {
		}
	}

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mvc = MockMvcBuilders.standaloneSetup(new Stub()).setControllerAdvice(new RestExceptionHandler())
				.setValidator(validator).build();
	}

	@Test
	void mapsApplicationExceptionsToTheirStatus() throws Exception {
		mvc.perform(get("/not-found")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Evento não encontrado"));
		mvc.perform(get("/conflict")).andExpect(status().isConflict());
		mvc.perform(get("/unauthorized")).andExpect(status().isUnauthorized());
		mvc.perform(get("/forbidden")).andExpect(status().isForbidden());
	}

	@Test
	void tooManyRequestsAnswers429WithRetryAfter() throws Exception {
		mvc.perform(get("/too-many")).andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "90"))
				.andExpect(jsonPath("$.detail").value("Muitas tentativas"));
	}

	@Test
	void domainRuleViolationIsUnprocessableEntityWithTheMessage() throws Exception {
		mvc.perform(get("/domain")).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Evento lotado"));
	}

	@Test
	void concurrentUpdatesAndIntegrityViolationsAreConflicts() throws Exception {
		mvc.perform(get("/optimistic")).andExpect(status().isConflict());
		mvc.perform(get("/integrity")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("A operação conflita com dados já existentes."));
	}

	@Test
	void invalidBodyListsTheOffendingFields() throws Exception {
		mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.name").exists());
	}

	@Test
	void malformedJsonAndBadPathVariablesAreBadRequests() throws Exception {
		mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{nope"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/uuid/not-a-uuid")).andExpect(status().isBadRequest());
	}
}
