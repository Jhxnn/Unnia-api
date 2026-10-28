package com.Unnia.messaging.application.handler;

import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.application.port.ContentAuthors;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.DomainEventHandler;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.event.ReactionAdded;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.model.TargetType;

import java.util.Optional;

/** Avisa o autor da postagem ou do comentário de uma nova reação, exceto quando ele mesmo reagiu. */
public class ReactionAddedHandler implements DomainEventHandler<ReactionAdded> {

	private final ContentAuthors authors;
	private final Notifier notifier;

	public ReactionAddedHandler(ContentAuthors authors, Notifier notifier) {
		this.authors = authors;
		this.notifier = notifier;
	}

	@Override
	public Class<ReactionAdded> eventType() {
		return ReactionAdded.class;
	}

	@Override
	public void handle(ReactionAdded event) {
		authorOf(event.target()).filter(author -> !author.equals(event.authorId()))
				.ifPresent(recipient -> notifier.notify(recipient, NotificationType.NEW_REACTION,
						"Alguém reagiu ao seu conteúdo", event.target().id()));
	}

	private Optional<AccountId> authorOf(ReactionTarget target) {
		return target.type() == TargetType.POST ? authors.authorOfPost(new PostId(target.id()))
				: authors.authorOfComment(new CommentId(target.id()));
	}
}
