package com.Unnia.social.application;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.port.AuthorDirectory.Author;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;

import java.time.Instant;
import java.util.List;

/** Comentário raiz com suas respostas (um único nível). */
public record CommentView(CommentId commentId, AccountId authorId, Author author, String text, Instant createdAt,
		List<CommentView> replies) {

	public static CommentView of(Comment comment, Author author, List<CommentView> replies) {
		return new CommentView(comment.id(), comment.authorId(), author, comment.content().text(),
				comment.createdAt(), List.copyOf(replies));
	}
}
