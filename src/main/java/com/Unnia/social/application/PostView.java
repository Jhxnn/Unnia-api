package com.Unnia.social.application;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.application.port.AuthorDirectory.Author;
import com.Unnia.social.domain.model.MediaRef;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.ReactionType;

import java.time.Instant;
import java.util.List;

/** @param myReaction a reação de quem consulta, ou nulo se ainda não reagiu */
public record PostView(PostId postId, AccountId authorId, Author author, String text, List<MediaRef> media,
		EventId linkedEventId, Instant createdAt, Instant editedAt, long reactionCount, long commentCount,
		ReactionType myReaction) {

	public static PostView of(Post post, Author author, long reactionCount, long commentCount,
			ReactionType myReaction) {
		return new PostView(post.id(), post.authorId(), author, post.content().text(), post.media(),
				post.linkedEventId().orElse(null), post.createdAt(), post.editedAt().orElse(null), reactionCount,
				commentCount, myReaction);
	}
}
