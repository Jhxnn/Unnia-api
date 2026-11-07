package com.Unnia.shared.application;

import com.Unnia.shared.domain.Guard;

import java.time.Duration;

/** Limite de tentativas excedido; {@code retryAfter} diz quanto esperar antes de tentar de novo. */
public class TooManyRequestsException extends ApplicationException {

	private final Duration retryAfter;

	public TooManyRequestsException(String message, Duration retryAfter) {
		super(message);
		this.retryAfter = Guard.notNull(retryAfter, "Tempo de espera é obrigatório");
	}

	public Duration retryAfter() {
		return retryAfter;
	}
}
