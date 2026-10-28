package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentContent;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.PostRepository;

import java.time.Clock;

public class AddCommentUseCase {

	public record Command(AccountId authorId, PostId postId, String text) {
	}

	private final PostRepository posts;
	private final CommentRepository comments;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public AddCommentUseCase(PostRepository posts, CommentRepository comments, DomainEventPublisher publisher,
			Clock clock) {
		this.posts = posts;
		this.comments = comments;
		this.publisher = publisher;
		this.clock = clock;
	}

	public CommentId execute(Command command) {
		Post post = posts.findById(command.postId()).filter(Post::isActive)
				.orElseThrow(() -> new NotFoundException("Postagem não encontrada"));

		Comment comment = Comment.onPost(post, command.authorId(), CommentContent.of(command.text()),
				clock.instant());

		comments.save(comment);
		publisher.publishFrom(comment);
		return comment.id();
	}
}
