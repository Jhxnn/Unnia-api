package com.Unnia.messaging.application.port;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.PostId;

import java.util.Optional;

/** Autoria de conteúdo do contexto social, usada para decidir quem notificar. */
public interface ContentAuthors {

	Optional<AccountId> authorOfPost(PostId postId);

	Optional<AccountId> authorOfComment(CommentId commentId);
}
