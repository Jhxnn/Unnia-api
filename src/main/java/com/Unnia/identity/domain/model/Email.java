package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.regex.Pattern;

public record Email(String value) {

	private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	public Email {
		Guard.notNull(value, "E-mail inválido");
		value = value.trim().toLowerCase();
		if (value.length() > 254 || !FORMAT.matcher(value).matches()) {
			throw new DomainException("E-mail inválido");
		}
	}

	public static Email of(String value) {
		return new Email(value);
	}
}
