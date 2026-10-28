package com.Unnia.messaging.application.handler;

import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.application.port.ContentAuthors;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.DomainEventHandler;
import com.Unnia.social.domain.event.CommentPosted;

/** Avisa o autor da postagem de um novo comentário, exceto quando ele mesmo comentou. */
public class CommentPostedHandler implements DomainEventHandler<CommentPosted> {

	private final ContentAuthors authors;
	private final Notifier notifier;

	public CommentPostedHandler(ContentAuthors authors, Notifier notifier) {
		this.authors = authors;
		this.notifier = notifier;
	}

	@Override
	public Class<CommentPosted> eventType() {
		return CommentPosted.class;
	}

	@Override
	public void handle(CommentPosted event) {
		authors.authorOfPost(event.postId()).filter(author -> !author.equals(event.authorId()))
				.ifPresent(recipient -> notifier.notify(recipient, NotificationType.NEW_COMMENT,
						"Alguém comentou na sua postagem", event.postId().value()));
	}
}
