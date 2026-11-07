package com.Unnia.shared.infrastructure.ratelimit;

import java.time.Duration;

/**
 * Porta do limitador de requisições. A implementação atual guarda os contadores em memória; para
 * várias réplicas basta outra implementação (por exemplo, Redis) sem mexer em quem a usa.
 */
public interface RateLimiter {

	/** Tenta consumir uma requisição da chave. */
	Decision tryAcquire(String key, RateLimit limit);

	/**
	 * @param remaining  requisições que ainda cabem agora (0 quando recusada)
	 * @param retryAfter quanto esperar para a próxima ser aceita (zero quando permitida)
	 */
	record Decision(boolean allowed, int remaining, Duration retryAfter) {
	}
}
