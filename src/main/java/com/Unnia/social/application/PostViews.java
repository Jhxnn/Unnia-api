package com.Unnia.social.application;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.port.AuthorDirectory;
import com.Unnia.social.application.port.AuthorDirectory.Author;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.Reaction;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.model.ReactionType;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.ReactionRepository;

import java.util.List;
import java.util.Map;

/** Monta a {@link PostView} agregando autor, contadores de reações e comentários e a reação de quem consulta. */
public class PostViews {

	private final ReactionRepository reactions;
	private final CommentRepository comments;
	private final AuthorDirectory authors;

	public PostViews(ReactionRepository reactions, CommentRepository comments, AuthorDirectory authors) {
		this.reactions = reactions;
		this.comments = comments;
		this.authors = authors;
	}

	public PostView of(Post post, AccountId viewer) {
		return list(List.of(post), viewer).get(0);
	}

	/** Os autores são buscados de uma vez para a lista inteira. */
	public List<PostView> list(List<Post> posts, AccountId viewer) {
		Map<AccountId, Author> found = authors.authorsOf(posts.stream().map(Post::authorId).distinct().toList());
		return posts.stream()
				.map(post -> PostView.of(post, found.getOrDefault(post.authorId(), AuthorDirectory.UNKNOWN),
						reactions.countByTarget(ReactionTarget.post(post.id())),
						comments.countActiveByPost(post.id()), reactionOf(viewer, post)))
				.toList();
	}

	private ReactionType reactionOf(AccountId viewer, Post post) {
		if (viewer == null) {
			return null;
		}
		return reactions.findByAuthorAndTarget(viewer, ReactionTarget.post(post.id())).map(Reaction::type)
				.orElse(null);
	}
}
