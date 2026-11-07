package com.Unnia.identity.application.port;

import java.time.Duration;
import java.util.Optional;

/**
 * Controle de tentativas de login falhas por identificador (o e-mail normalizado), para frear
 * ataques de força bruta contra uma conta.
 */
public interface LoginAttempts {

	/** Quanto falta para a chave voltar a poder tentar, se estiver bloqueada; vazio se está liberada. */
	Optional<Duration> blockedFor(String key);

	void recordFailure(String key);

	/** Limpa o histórico de falhas, por exemplo após um login bem-sucedido. */
	void reset(String key);
}
