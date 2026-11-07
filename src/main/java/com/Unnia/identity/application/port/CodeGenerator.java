package com.Unnia.identity.application.port;

public interface CodeGenerator {

	/** Código numérico de 6 dígitos (com zeros à esquerda), para o usuário digitar. */
	String newCode();
}
