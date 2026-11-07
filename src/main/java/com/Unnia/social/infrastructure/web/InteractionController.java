package com.Unnia.social.infrastructure.web;

import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.usecase.DeleteCommentUseCase;
import com.Unnia.social.application.usecase.FollowAccountUseCase;
import com.Unnia.social.application.usecase.ReactUseCase;
import com.Unnia.social.application.usecase.RemoveReactionUseCase;
import com.Unnia.social.application.usecase.ReplyToCommentUseCase;
import com.Unnia.social.application.usecase.UnfollowAccountUseCase;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.model.ReactionType;
import com.Unnia.social.domain.model.TargetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Respostas a comentários, reações e seguidores. */
@RestController
class InteractionController {

	record ReplyRequest(@NotBlank String text) {
	}

	record ReactRequest(@NotNull TargetType targetType, @NotNull UUID targetId, @NotNull ReactionType type) {
	}

	record ReplyCreated(CommentId commentId) {
	}

	private final ReplyToCommentUseCase reply;
	private final DeleteCommentUseCase deleteComment;
	private final ReactUseCase react;
	private final RemoveReactionUseCase removeReaction;
	private final FollowAccountUseCase follow;
	private final UnfollowAccountUseCase unfollow;

	InteractionController(ReplyToCommentUseCase reply, DeleteCommentUseCase deleteComment, ReactUseCase react,
			RemoveReactionUseCase removeReaction, FollowAccountUseCase follow, UnfollowAccountUseCase unfollow) {
		this.reply = reply;
		this.deleteComment = deleteComment;
		this.react = react;
		this.removeReaction = removeReaction;
		this.follow = follow;
		this.unfollow = unfollow;
	}

	@PostMapping("/api/comments/{id}/replies")
	@ResponseStatus(HttpStatus.CREATED)
	ReplyCreated reply(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody ReplyRequest request) {
		return new ReplyCreated(reply.execute(
				new ReplyToCommentUseCase.Command(user.accountId(), new CommentId(id), request.text())));
	}

	@DeleteMapping("/api/comments/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteComment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		deleteComment.execute(new DeleteCommentUseCase.Command(user.accountId(), new CommentId(id)));
	}

	@PutMapping("/api/reactions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void react(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ReactRequest request) {
		react.execute(new ReactUseCase.Command(user.accountId(),
				new ReactionTarget(request.targetType(), request.targetId()), request.type()));
	}

	@DeleteMapping("/api/reactions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void removeReaction(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam TargetType targetType,
			@RequestParam UUID targetId) {
		removeReaction.execute(new RemoveReactionUseCase.Command(user.accountId(),
				new ReactionTarget(targetType, targetId)));
	}

	@PostMapping("/api/follows/{accountId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void follow(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID accountId) {
		follow.execute(new FollowAccountUseCase.Command(user.accountId(), new AccountId(accountId)));
	}

	@DeleteMapping("/api/follows/{accountId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void unfollow(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID accountId) {
		unfollow.execute(new UnfollowAccountUseCase.Command(user.accountId(), new AccountId(accountId)));
	}
}
