package com.Unnia.social.application;

import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.ReactionRepository;

/** Monta a {@link PostView} agregando contadores de reações e comentários. */
public class PostViews {

	private final ReactionRepository reactions;
	private final CommentRepository comments;

	public PostViews(ReactionRepository reactions, CommentRepository comments) {
		this.reactions = reactions;
		this.comments = comments;
	}

	public PostView of(Post post) {
		return PostView.of(post, reactions.countByTarget(ReactionTarget.post(post.id())),
				comments.countActiveByPost(post.id()));
	}
}
