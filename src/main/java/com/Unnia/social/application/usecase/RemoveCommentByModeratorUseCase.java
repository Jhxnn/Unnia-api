package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.repository.CommentRepository;

/**
 * Uso interno: a autorização é feita pelo contexto de moderação antes de chamar este caso de uso.
 * Idempotente: um comentário já removido não é erro.
 */
public class RemoveCommentByModeratorUseCase {

	private final CommentRepository comments;
	private final DomainEventPublisher publisher;

	public RemoveCommentByModeratorUseCase(CommentRepository comments, DomainEventPublisher publisher) {
		this.comments = comments;
		this.publisher = publisher;
	}

	public void execute(CommentId commentId) {
		Comment comment = comments.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Comentário não encontrado"));
		if (!comment.isActive()) {
			return;
		}

		comment.removeByModerator();

		comments.save(comment);
		publisher.publishFrom(comment);
	}
}
