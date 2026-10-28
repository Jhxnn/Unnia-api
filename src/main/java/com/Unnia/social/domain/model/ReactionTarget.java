package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record ReactionTarget(TargetType type, UUID id) {

	public ReactionTarget {
		Guard.notNull(type, "Tipo do alvo é obrigatório");
		Guard.notNull(id, "Alvo é obrigatório");
	}

	public static ReactionTarget post(PostId postId) {
		return new ReactionTarget(TargetType.POST, postId.value());
	}

	public static ReactionTarget comment(CommentId commentId) {
		return new ReactionTarget(TargetType.COMMENT, commentId.value());
	}
}
