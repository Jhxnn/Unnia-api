package com.Unnia.social.application;

import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.application.RecordingEventPublisher;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.fakes.FakeDirectories;
import com.Unnia.social.application.fakes.InMemoryCommentRepository;
import com.Unnia.social.application.fakes.InMemoryFollowRepository;
import com.Unnia.social.application.fakes.InMemoryPostRepository;
import com.Unnia.social.application.fakes.InMemoryReactionRepository;
import com.Unnia.social.application.usecase.AddCommentUseCase;
import com.Unnia.social.application.usecase.PublishPostUseCase;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.PostId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Cenário compartilhado pelos testes de casos de uso do contexto social. */
public class SocialFixture {

	public static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

	public final MutableClock clock = new MutableClock(NOW);
	public final InMemoryPostRepository posts = new InMemoryPostRepository();
	public final InMemoryCommentRepository comments = new InMemoryCommentRepository();
	public final InMemoryReactionRepository reactions = new InMemoryReactionRepository();
	public final InMemoryFollowRepository follows = new InMemoryFollowRepository();
	public final FakeDirectories directories = new FakeDirectories();
	public final RecordingEventPublisher publisher = new RecordingEventPublisher();
	public final PostViews views = new PostViews(reactions, comments);

	public AccountId account() {
		return directories.newAccount();
	}

	public PostId post(AccountId author, String text) {
		PostId id = new PublishPostUseCase(posts, directories, publisher, clock)
				.execute(new PublishPostUseCase.Command(author, text, List.of(), null));
		publisher.clear();
		clock.advance(Duration.ofMinutes(1));
		return id;
	}

	public CommentId comment(AccountId author, PostId postId, String text) {
		CommentId id = new AddCommentUseCase(posts, comments, publisher, clock)
				.execute(new AddCommentUseCase.Command(author, postId, text));
		publisher.clear();
		clock.advance(Duration.ofMinutes(1));
		return id;
	}
}
