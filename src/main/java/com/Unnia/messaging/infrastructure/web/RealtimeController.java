package com.Unnia.messaging.infrastructure.web;

import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.messaging.infrastructure.realtime.SseHub;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Canal de tempo real da conta autenticada (Server-Sent Events). Emite {@code ready} ao conectar e {@code update}
 * a cada novidade, com {@code {"type": "...", "referenceId": "..."}}. O cliente reage buscando os dados pela API.
 */
@Tag(name = "Tempo real")
@RestController
class RealtimeController {

	private final SseHub hub;

	RealtimeController(SseHub hub) {
		this.hub = hub;
	}

	@GetMapping(path = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter stream(@AuthenticationPrincipal AuthenticatedUser user) {
		return hub.subscribe(user.accountId());
	}
}
