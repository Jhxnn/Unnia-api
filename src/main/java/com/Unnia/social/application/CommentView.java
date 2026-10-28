package com.Unnia.social.application;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;

import java.time.Instant;
import java.util.List;

/** Comentário raiz com suas respostas (um único nível). */
public record CommentView(CommentId commentId, AccountId authorId, String text, Instant createdAt,
		List<CommentView> replies) {

	public static CommentView of(Comment comment, List<CommentView> replies) {
		return new CommentView(comment.id(), comment.authorId(), comment.content().text(), comment.createdAt(),
				List.copyOf(replies));
	}
}
