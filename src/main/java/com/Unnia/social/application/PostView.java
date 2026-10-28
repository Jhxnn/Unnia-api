package com.Unnia.social.application;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.domain.model.MediaRef;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;

import java.time.Instant;
import java.util.List;

public record PostView(PostId postId, AccountId authorId, String text, List<MediaRef> media, EventId linkedEventId,
		Instant createdAt, Instant editedAt, long reactionCount, long commentCount) {

	public static PostView of(Post post, long reactionCount, long commentCount) {
		return new PostView(post.id(), post.authorId(), post.content().text(), post.media(),
				post.linkedEventId().orElse(null), post.createdAt(), post.editedAt().orElse(null), reactionCount,
				commentCount);
	}
}
