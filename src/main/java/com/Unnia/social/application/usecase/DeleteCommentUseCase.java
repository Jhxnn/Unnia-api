package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.repository.CommentRepository;

public class DeleteCommentUseCase {

	public record Command(AccountId accountId, CommentId commentId) {
	}

	private final CommentRepository comments;
	private final DomainEventPublisher publisher;

	public DeleteCommentUseCase(CommentRepository comments, DomainEventPublisher publisher) {
		this.comments = comments;
		this.publisher = publisher;
	}

	public void execute(Command command) {
		Comment comment = comments.findById(command.commentId()).filter(Comment::isActive)
				.orElseThrow(() -> new NotFoundException("Comentário não encontrado"));
		if (!comment.authorId().equals(command.accountId())) {
			throw new ForbiddenException("Somente o autor pode remover o comentário");
		}

		comment.delete(command.accountId());

		comments.save(comment);
		publisher.publishFrom(comment);
	}
}
