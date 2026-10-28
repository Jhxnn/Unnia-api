package com.Unnia.messaging.domain.model;

import com.Unnia.messaging.domain.event.ConversationStarted;
import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;
import java.util.Set;

/** Conversa direta entre exatamente duas contas. */
public class Conversation extends AggregateRoot<ConversationId> {

	private final Set<AccountId> participants;
	private final Instant startedAt;
	private Instant lastActivityAt;

	private Conversation(ConversationId id, Set<AccountId> participants, Instant startedAt,
			Instant lastActivityAt) {
		super(id);
		Guard.notNull(participants, "Participantes são obrigatórios");
		Guard.check(participants.size() == 2, "Uma conversa precisa de exatamente dois participantes");
		this.participants = Set.copyOf(participants);
		this.startedAt = Guard.notNull(startedAt, "Data de início é obrigatória");
		this.lastActivityAt = Guard.notNull(lastActivityAt, "Última atividade é obrigatória");
	}

	public static Conversation start(AccountId initiatorId, AccountId otherId, Instant now) {
		Guard.notNull(initiatorId, "Iniciador é obrigatório");
		Guard.notNull(otherId, "Destinatário é obrigatório");
		Guard.check(!initiatorId.equals(otherId), "Não é possível conversar consigo mesmo");
		Conversation conversation = new Conversation(ConversationId.generate(), Set.of(initiatorId, otherId), now,
				now);
		conversation.raise(new ConversationStarted(conversation.id(), initiatorId, otherId));
		return conversation;
	}

	public static Conversation restore(ConversationId id, Set<AccountId> participants, Instant startedAt,
			Instant lastActivityAt) {
		return new Conversation(id, participants, startedAt, lastActivityAt);
	}

	public Message send(AccountId senderId, MessageContent content, Instant now) {
		Guard.check(includes(senderId), "Somente participantes podem enviar mensagens");
		Message message = Message.create(id(), senderId, otherParticipant(senderId), content, now);
		this.lastActivityAt = now;
		return message;
	}

	public boolean includes(AccountId accountId) {
		return participants.contains(accountId);
	}

	public AccountId otherParticipant(AccountId accountId) {
		Guard.check(includes(accountId), "A conta não participa da conversa");
		return participants.stream().filter(p -> !p.equals(accountId)).findFirst().orElseThrow();
	}

	public Set<AccountId> participants() {
		return participants;
	}

	public Instant startedAt() {
		return startedAt;
	}

	public Instant lastActivityAt() {
		return lastActivityAt;
	}
}
