package com.Unnia.shared.infrastructure.acl;

import com.Unnia.identity.application.usecase.SuspendAccountUseCase;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.moderation.application.port.ContentTakedown;
import com.Unnia.moderation.application.port.ModeratorDirectory;
import com.Unnia.moderation.application.port.ReportableContent;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.usecase.RemoveCommentByModeratorUseCase;
import com.Unnia.social.application.usecase.RemovePostByModeratorUseCase;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.PostRepository;
import org.springframework.stereotype.Component;

/** Camada anticorrupção: o contexto de moderação enxerga identidade e social apenas por estas portas. */
final class ModerationAcl {

	private ModerationAcl() {
	}

	@Component
	static class IdentityModeratorDirectory implements ModeratorDirectory {

		private final AccountRepository accounts;

		IdentityModeratorDirectory(AccountRepository accounts) {
			this.accounts = accounts;
		}

		@Override
		public boolean isModerator(AccountId accountId) {
			return accounts.findById(accountId).filter(Account::isActive).filter(a -> a.hasRole(Role.ADMIN))
					.isPresent();
		}
	}

	@Component
	static class OriginReportableContent implements ReportableContent {

		private final PostRepository posts;
		private final CommentRepository comments;
		private final AccountRepository accounts;

		OriginReportableContent(PostRepository posts, CommentRepository comments, AccountRepository accounts) {
			this.posts = posts;
			this.comments = comments;
			this.accounts = accounts;
		}

		@Override
		public boolean exists(ReportTarget target) {
			return switch (target.type()) {
				case POST -> posts.findById(new PostId(target.id())).filter(Post::isActive).isPresent();
				case COMMENT -> comments.findById(new CommentId(target.id())).filter(Comment::isActive).isPresent();
				case ACCOUNT -> accounts.findById(new AccountId(target.id())).isPresent();
			};
		}
	}

	@Component
	static class OriginContentTakedown implements ContentTakedown {

		private final RemovePostByModeratorUseCase removePost;
		private final RemoveCommentByModeratorUseCase removeComment;
		private final SuspendAccountUseCase suspendAccount;
		private final AccountRepository accounts;

		OriginContentTakedown(RemovePostByModeratorUseCase removePost, RemoveCommentByModeratorUseCase removeComment,
				SuspendAccountUseCase suspendAccount, AccountRepository accounts) {
			this.removePost = removePost;
			this.removeComment = removeComment;
			this.suspendAccount = suspendAccount;
			this.accounts = accounts;
		}

		@Override
		public void takeDown(AccountId moderatorId, ReportTarget target) {
			switch (target.type()) {
				case POST -> removePost.execute(new PostId(target.id()));
				case COMMENT -> removeComment.execute(new CommentId(target.id()));
				case ACCOUNT -> suspendIfActive(moderatorId, new AccountId(target.id()));
			}
		}

		private void suspendIfActive(AccountId moderatorId, AccountId target) {
			if (accounts.findById(target).filter(Account::isActive).isPresent()) {
				suspendAccount.execute(new SuspendAccountUseCase.Command(moderatorId, target));
			}
		}
	}
}
