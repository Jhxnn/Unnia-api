package com.Unnia.social.infrastructure.web;

import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.infrastructure.web.RestExceptionHandler;
import com.Unnia.social.application.usecase.UploadMediaUseCase;
import com.Unnia.social.domain.model.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MediaControllerTest {

	private final AccountId account = AccountId.generate();
	private UploadMediaUseCase upload;
	private MockMvc mvc;

	/** Faz o papel do filtro JWT: entrega a conta autenticada ao parâmetro anotado com @AuthenticationPrincipal. */
	private HandlerMethodArgumentResolver loggedIn() {
		return new HandlerMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
			}

			@Override
			public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
					NativeWebRequest request, WebDataBinderFactory binder) {
				return new AuthenticatedUser(account, Role.VOLUNTEER);
			}
		};
	}

	@BeforeEach
	void setUp() {
		upload = mock(UploadMediaUseCase.class);
		mvc = MockMvcBuilders.standaloneSetup(new MediaController(upload)).setControllerAdvice(new RestExceptionHandler())
				.setCustomArgumentResolvers(loggedIn()).build();
	}

	@Test
	void theResponseCarriesTheUrlAndTheTypeAndTheBytesGoToTheUseCase() throws Exception {
		when(upload.execute(any())).thenReturn(new UploadMediaUseCase.Result("https://cdn/x.png", MediaType.IMAGE));

		mvc.perform(multipart(HttpMethod.POST, "/api/media")
				.file(new MockMultipartFile("file", "x.png", "image/png", new byte[] { 1, 2 })))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.url").value("https://cdn/x.png"))
				.andExpect(jsonPath("$.type").value("IMAGE"));

		ArgumentCaptor<UploadMediaUseCase.Command> command = ArgumentCaptor.forClass(UploadMediaUseCase.Command.class);
		verify(upload).execute(command.capture());
		assertThat(command.getValue().ownerId()).isEqualTo(account);
		assertThat(command.getValue().content()).containsExactly(1, 2);
	}

	@Test
	void aRejectedFileBecomesUnprocessableEntityWithTheReason() throws Exception {
		when(upload.execute(any())).thenThrow(new DomainException("Formato não aceito. Use JPEG, PNG, WebP ou GIF"));

		mvc.perform(multipart(HttpMethod.POST, "/api/media")
				.file(new MockMultipartFile("file", "x.txt", "text/plain", "oi".getBytes())))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Formato não aceito. Use JPEG, PNG, WebP ou GIF"));
	}

	@Test
	void withoutTheFilePartItIsABadRequestAndNothingRuns() throws Exception {
		mvc.perform(multipart(HttpMethod.POST, "/api/media")).andExpect(status().isBadRequest());

		verify(upload, never()).execute(any());
	}
}
