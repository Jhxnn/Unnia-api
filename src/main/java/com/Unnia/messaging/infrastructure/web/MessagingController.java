package com.Unnia.messaging.infrastructure.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.messaging.application.ConversationView;
import com.Unnia.messaging.application.MessageView;
import com.Unnia.messaging.application.usecase.GetMessagesUseCase;
import com.Unnia.messaging.application.usecase.ListConversationsUseCase;
import com.Unnia.messaging.application.usecase.MarkConversationReadUseCase;
import com.Unnia.messaging.application.usecase.SendMessageUseCase;
import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.model.MessageId;
import com.Unnia.shared.domain.id.AccountId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Mensagens")
@RestController
class MessagingController {

	record SendMessageRequest(@NotNull UUID recipientId, @NotBlank String text) {
	}

	record MessageSent(ConversationId conversationId, MessageId messageId) {
	}

	private final SendMessageUseCase send;
	private final ListConversationsUseCase listConversations;
	private final GetMessagesUseCase getMessages;
	private final MarkConversationReadUseCase markRead;

	MessagingController(SendMessageUseCase send, ListConversationsUseCase listConversations,
			GetMessagesUseCase getMessages, MarkConversationReadUseCase markRead) {
		this.send = send;
		this.listConversations = listConversations;
		this.getMessages = getMessages;
		this.markRead = markRead;
	}

	@PostMapping("/api/messages")
	@ResponseStatus(HttpStatus.CREATED)
	MessageSent send(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody SendMessageRequest request) {
		var result = send.execute(new SendMessageUseCase.Command(user.accountId(),
				new AccountId(request.recipientId()), request.text()));
		return new MessageSent(result.conversationId(), result.messageId());
	}

	@GetMapping("/api/conversations")
	List<ConversationView> conversations(@AuthenticationPrincipal AuthenticatedUser user) {
		return listConversations.execute(user.accountId());
	}

	@GetMapping("/api/conversations/{id}/messages")
	List<MessageView> messages(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size) {
		return getMessages.execute(user.accountId(), new ConversationId(id), page, size);
	}

	@PostMapping("/api/conversations/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void markRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		markRead.execute(new MarkConversationReadUseCase.Command(user.accountId(), new ConversationId(id)));
	}
}
