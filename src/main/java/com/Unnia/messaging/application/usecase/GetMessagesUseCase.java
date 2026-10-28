package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.application.MessageView;
import com.Unnia.messaging.domain.model.Conversation;
import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.repository.ConversationRepository;
import com.Unnia.messaging.domain.repository.MessageRepository;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

import java.util.List;

/** Mensagens de uma conversa, da mais recente para a mais antiga. Só participantes podem ler. */
public class GetMessagesUseCase {

	public static final int MAX_PAGE_SIZE = 100;

	private final ConversationRepository conversations;
	private final MessageRepository messages;

	public GetMessagesUseCase(ConversationRepository conversations, MessageRepository messages) {
		this.conversations = conversations;
		this.messages = messages;
	}

	public List<MessageView> execute(AccountId accountId, ConversationId conversationId, int page, int size) {
		Guard.check(page >= 0, "Página inválida");
		Guard.check(size >= 1 && size <= MAX_PAGE_SIZE, "O tamanho da página deve ficar entre 1 e " + MAX_PAGE_SIZE);
		Conversation conversation = conversations.findById(conversationId)
				.orElseThrow(() -> new NotFoundException("Conversa não encontrada"));
		if (!conversation.includes(accountId)) {
			throw new ForbiddenException("Você não participa desta conversa");
		}
		return messages.findByConversation(conversationId, page, size).stream().map(MessageView::from).toList();
	}
}
