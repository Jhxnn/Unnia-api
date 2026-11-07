package com.Unnia.moderation.application.port;

import com.Unnia.moderation.domain.model.ReportTarget;

import java.util.Optional;

/** Verifica se o alvo de uma denúncia existe nos contextos de origem. */
public interface ReportableContent {

	boolean exists(ReportTarget target);

	/**
	 * Resumo legível do alvo para o moderador decidir: o trecho do texto de uma postagem ou comentário, ou
	 * o e-mail de uma conta. Vazio se o conteúdo não existe mais.
	 */
	Optional<String> describe(ReportTarget target);
}
