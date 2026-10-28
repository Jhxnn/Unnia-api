package com.Unnia.shared.application;

public abstract class ApplicationException extends RuntimeException {

	protected ApplicationException(String message) {
		super(message);
	}
}
