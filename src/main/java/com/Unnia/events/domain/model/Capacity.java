package com.Unnia.events.domain.model;

import com.Unnia.shared.domain.Guard;

public record Capacity(int max) {

	public Capacity {
		Guard.check(max >= 1, "Capacidade deve ser de pelo menos 1 vaga");
	}

	public static Capacity of(int max) {
		return new Capacity(max);
	}
}
