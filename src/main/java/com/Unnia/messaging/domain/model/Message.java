package com.Unnia.messaging.domain.model;

import com.Unnia.messaging.domain.event.MessageSent;
import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;
import java.util.Optional;

public class Message extends AggregateRoot<MessageId> {

	private final ConversationId conversationId;
	private final AccountId senderId;
	private final AccountId recipientId;
	private final MessageContent content;
	private final Instant sentAt;
	private Instant readAt;

	private Message(MessageId id, ConversationId conversationId, AccountId senderId, AccountId recipientId,
			MessageContent content, Instant sentAt, Instant readAt) {
		super(id);
		this.conversationId = Guard.notNull(conversationId, "Conversa é obrigatória");
		this.senderId = Guard.notNull(senderId, "Remetente é obrigatório");
		this.recipientId = Guard.notNull(recipientId, "Destinatário é obrigatório");
		this.content = Guard.notNull(content, "Conteúdo é obrigatório");
		this.sentAt = Guard.notNull(sentAt, "Data de envio é obrigatória");
		this.readAt = readAt;
	}

	/** Criada apenas por {@link Conversation#send}, que valida os participantes. */
	static Message create(ConversationId conversationId, AccountId senderId, AccountId recipientId,
			MessageContent content, Instant now) {
		Message message = new Message(MessageId.generate(), conversationId, senderId, recipientId, content, now,
				null);
		message.raise(new MessageSent(message.id(), conversationId, senderId, recipientId));
		return message;
	}

	public static Message restore(MessageId id, ConversationId conversationId, AccountId senderId,
			AccountId recipientId, MessageContent content, Instant sentAt, Instant readAt) {
		return new Message(id, conversationId, senderId, recipientId, content, sentAt, readAt);
	}

	/** Idempotente: marcar uma mensagem já lida não altera a data de leitura. */
	public void markRead(AccountId reader, Instant now) {
		Guard.check(recipientId.equals(reader), "Somente o destinatário pode marcar a mensagem como lida");
		if (readAt == null) {
			readAt = now;
		}
	}

	public boolean isRead() {
		return readAt != null;
	}

	public ConversationId conversationId() {
		return conversationId;
	}

	public AccountId senderId() {
		return senderId;
	}

	public AccountId recipientId() {
		return recipientId;
	}

	public MessageContent content() {
		return content;
	}

	public Instant sentAt() {
		return sentAt;
	}

	public Optional<Instant> readAt() {
		return Optional.ofNullable(readAt);
	}
}
