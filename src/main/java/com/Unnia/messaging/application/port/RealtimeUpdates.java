package com.Unnia.messaging.application.port;

import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.domain.id.AccountId;

import java.util.UUID;

/**
 * Avisa, em tempo real, quem está com o aplicativo aberto de que há novidade para ele. É só um empurrão: o aviso
 * diz o que mudou e o cliente busca os dados pela API de sempre. Se ninguém estiver conectado, não acontece nada,
 * e a novidade continua valendo quando a pessoa abrir o app.
 */
public interface RealtimeUpdates {

	/** Para quando não há canal em tempo real (testes, por exemplo). */
	RealtimeUpdates NONE = (recipient, type, referenceId) -> {
	};

	void notifyChange(AccountId recipient, NotificationType type, UUID referenceId);
}
