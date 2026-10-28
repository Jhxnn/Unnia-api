package com.Unnia.social.application.port;

import com.Unnia.shared.domain.id.EventId;

/** Visão do contexto de eventos que o contexto social precisa conhecer. */
public interface EventReferences {

	boolean exists(EventId eventId);
}
