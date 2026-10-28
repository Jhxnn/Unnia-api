package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.Reaction;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.model.ReactionType;
import com.Unnia.social.domain.model.TargetType;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.PostRepository;
import com.Unnia.social.domain.repository.ReactionRepository;

import java.time.Clock;

/** Reage a uma postagem ou comentário. Reagir de novo ao mesmo alvo troca o tipo da reação. */
public class ReactUseCase {

	public record Command(AccountId accountId, ReactionTarget target, ReactionType type) {
	}

	private final PostRepository posts;
	private final CommentRepository comments;
	private final ReactionRepository reactions;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public ReactUseCase(PostRepository posts, CommentRepository comments, ReactionRepository reactions,
			DomainEventPublisher publisher, Clock clock) {
		this.posts = posts;
		this.comments = comments;
		this.reactions = reactions;
		this.publisher = publisher;
		this.clock = clock;
	}

	public void execute(Command command) {
		ensureTargetIsActive(command.target());

		var existing = reactions.findByAuthorAndTarget(command.accountId(), command.target());
		if (existing.isPresent()) {
			Reaction reaction = existing.get();
			reaction.changeType(command.type());
			reactions.save(reaction);
			return;
		}

		Reaction reaction = Reaction.react(command.accountId(), command.target(), command.type(), clock.instant());
		reactions.save(reaction);
		publisher.publishFrom(reaction);
	}

	private void ensureTargetIsActive(ReactionTarget target) {
		if (target.type() == TargetType.POST) {
			posts.findById(new PostId(target.id())).filter(Post::isActive)
					.orElseThrow(() -> new NotFoundException("Postagem não encontrada"));
		} else {
			comments.findById(new CommentId(target.id())).filter(Comment::isActive)
					.orElseThrow(() -> new NotFoundException("Comentário não encontrado"));
		}
	}
}
