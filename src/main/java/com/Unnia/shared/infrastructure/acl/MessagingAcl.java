package com.Unnia.shared.infrastructure.acl;

import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.messaging.application.port.AccountResolver;
import com.Unnia.messaging.application.port.ContentAuthors;
import com.Unnia.messaging.application.port.ParticipantDirectory;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.PostRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Camada anticorrupção: o contexto de mensagens enxerga identidade e social apenas por estas portas. */
final class MessagingAcl {

	private MessagingAcl() {
	}

	@Component
	static class IdentityParticipantDirectory implements ParticipantDirectory {

		private final AccountRepository accounts;

		IdentityParticipantDirectory(AccountRepository accounts) {
			this.accounts = accounts;
		}

		@Override
		public Optional<Kind> activeKindOf(AccountId accountId) {
			return accounts.findById(accountId).filter(Account::isActive).map(account -> switch (account.role()) {
				case NGO -> Kind.NGO;
				case VOLUNTEER -> Kind.VOLUNTEER;
				case ADMIN -> Kind.OTHER;
			});
		}
	}

	@Component
	static class IdentityAccountResolver implements AccountResolver {

		private final NgoProfileRepository ngos;
		private final VolunteerProfileRepository volunteers;

		IdentityAccountResolver(NgoProfileRepository ngos, VolunteerProfileRepository volunteers) {
			this.ngos = ngos;
			this.volunteers = volunteers;
		}

		@Override
		public Optional<AccountId> accountOfNgo(NgoId ngoId) {
			return ngos.findById(ngoId).map(NgoProfile::accountId);
		}

		@Override
		public Optional<AccountId> accountOfVolunteer(VolunteerId volunteerId) {
			return volunteers.findById(volunteerId).map(VolunteerProfile::accountId);
		}
	}

	@Component
	static class SocialContentAuthors implements ContentAuthors {

		private final PostRepository posts;
		private final CommentRepository comments;

		SocialContentAuthors(PostRepository posts, CommentRepository comments) {
			this.posts = posts;
			this.comments = comments;
		}

		@Override
		public Optional<AccountId> authorOfPost(PostId postId) {
			return posts.findById(postId).map(Post::authorId);
		}

		@Override
		public Optional<AccountId> authorOfComment(CommentId commentId) {
			return comments.findById(commentId).map(Comment::authorId);
		}
	}
}
