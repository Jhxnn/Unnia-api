package com.Unnia.identity.application.fakes;

import com.Unnia.identity.application.port.CodeGenerator;

/** Códigos previsíveis para os testes: 111111, 222222, 333333... */
public class FakeCodes implements CodeGenerator {

	private int counter;

	@Override
	public String newCode() {
		counter++;
		return String.valueOf(counter).repeat(6);
	}
}
