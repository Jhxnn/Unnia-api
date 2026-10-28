package com.Unnia.shared.domain;

public final class Guard {

	private Guard() {
	}

	public static <T> T notNull(T value, String message) {
		if (value == null) {
			throw new DomainException(message);
		}
		return value;
	}

	public static String notBlank(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new DomainException(message);
		}
		return value.trim();
	}

	public static String maxLength(String value, int max, String message) {
		if (value.length() > max) {
			throw new DomainException(message);
		}
		return value;
	}

	public static void check(boolean condition, String message) {
		if (!condition) {
			throw new DomainException(message);
		}
	}
}
