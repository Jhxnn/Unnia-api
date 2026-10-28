package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentContent;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.PostRepository;

import java.time.Clock;

public class ReplyToCommentUseCase {

	public record Command(AccountId authorId, CommentId parentId, String text) {
	}

	private final PostRepository posts;
	private final CommentRepository comments;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public ReplyToCommentUseCase(PostRepository posts, CommentRepository comments, DomainEventPublisher publisher,
			Clock clock) {
		this.posts = posts;
		this.comments = comments;
		this.publisher = publisher;
		this.clock = clock;
	}

	public CommentId execute(Command command) {
		Comment parent = comments.findById(command.parentId()).filter(Comment::isActive)
				.orElseThrow(() -> new NotFoundException("Comentário não encontrado"));
		posts.findById(parent.postId()).filter(Post::isActive)
				.orElseThrow(() -> new NotFoundException("Postagem não encontrada"));

		Comment reply = Comment.replyTo(parent, command.authorId(), CommentContent.of(command.text()),
				clock.instant());

		comments.save(reply);
		publisher.publishFrom(reply);
		return reply.id();
	}
}
