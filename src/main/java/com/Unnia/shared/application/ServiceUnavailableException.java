package com.Unnia.shared.application;

/** Um serviço de que a operação depende (por exemplo, o envio de e-mail) está fora do ar agora. */
public class ServiceUnavailableException extends ApplicationException {

	public ServiceUnavailableException(String message) {
		super(message);
	}

	public ServiceUnavailableException(String message, Throwable cause) {
		super(message);
		initCause(cause);
	}
}
