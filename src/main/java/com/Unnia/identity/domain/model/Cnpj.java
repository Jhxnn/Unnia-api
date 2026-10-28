package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

public record Cnpj(String value) {

	private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
	private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

	public Cnpj {
		Guard.notNull(value, "CNPJ inválido");
		value = value.trim().replaceAll("[.\\-/]", "");
		if (!isValid(value)) {
			throw new DomainException("CNPJ inválido");
		}
	}

	public static Cnpj of(String value) {
		return new Cnpj(value);
	}

	public String formatted() {
		return "%s.%s.%s/%s-%s".formatted(
				value.substring(0, 2), value.substring(2, 5), value.substring(5, 8),
				value.substring(8, 12), value.substring(12, 14));
	}

	private static boolean isValid(String digits) {
		if (!digits.matches("\\d{14}") || digits.chars().distinct().count() == 1) {
			return false;
		}
		return checkDigit(digits, FIRST_WEIGHTS) == digits.charAt(12) - '0'
				&& checkDigit(digits, SECOND_WEIGHTS) == digits.charAt(13) - '0';
	}

	private static int checkDigit(String digits, int[] weights) {
		int sum = 0;
		for (int i = 0; i < weights.length; i++) {
			sum += (digits.charAt(i) - '0') * weights[i];
		}
		int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}
}
