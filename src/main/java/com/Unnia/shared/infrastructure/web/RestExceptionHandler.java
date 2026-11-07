package com.Unnia.shared.infrastructure.web;

import com.Unnia.shared.application.ConflictException;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.application.ServiceUnavailableException;
import com.Unnia.shared.application.TooManyRequestsException;
import com.Unnia.shared.application.UnauthorizedException;
import com.Unnia.shared.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduz as exceções da aplicação e do domínio em respostas HTTP no formato Problem Details. */
@RestControllerAdvice
public class RestExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException e) {
		return problem(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail conflict(ConflictException e) {
		return problem(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(UnauthorizedException.class)
	ProblemDetail unauthorized(UnauthorizedException e) {
		return problem(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	/** Excesso de tentativas (por exemplo, falhas de login): informa quanto esperar em {@code Retry-After}. */
	@ExceptionHandler(TooManyRequestsException.class)
	ResponseEntity<ProblemDetail> tooManyRequests(TooManyRequestsException e) {
		long seconds = Math.max(1, (long) Math.ceil(e.retryAfter().toMillis() / 1000.0));
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
				.body(problem(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()));
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	ProblemDetail serviceUnavailable(ServiceUnavailableException e) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	ProblemDetail forbidden(ForbiddenException e) {
		return problem(HttpStatus.FORBIDDEN, e.getMessage());
	}

	/** Regra de negócio violada: a requisição é válida na forma, mas não pode ser atendida. */
	@ExceptionHandler(DomainException.class)
	ProblemDetail domain(DomainException e) {
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
	}

	/** Duas operações concorrentes no mesmo agregado, por exemplo as últimas vagas de um evento. */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail concurrentUpdate(OptimisticLockingFailureException e) {
		return problem(HttpStatus.CONFLICT, "O recurso foi alterado por outra operação. Tente novamente.");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail integrity(DataIntegrityViolationException e) {
		log.warn("Violação de integridade", e);
		return problem(HttpStatus.CONFLICT, "A operação conflita com dados já existentes.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail invalidBody(MethodArgumentNotValidException e) {
		Map<String, String> fields = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos");
		problem.setProperty("fields", fields);
		return problem;
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	ProblemDetail malformed(Exception e) {
		return problem(HttpStatus.BAD_REQUEST, "Requisição malformada");
	}

	private ProblemDetail problem(HttpStatus status, String detail) {
		return ProblemDetail.forStatusAndDetail(status, detail);
	}
}
