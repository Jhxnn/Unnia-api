package com.Unnia.messaging.application.fakes;

import com.Unnia.messaging.application.port.AccountResolver;
import com.Unnia.messaging.application.port.ContentAuthors;
import com.Unnia.messaging.application.port.ParticipantDirectory;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.PostId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Implementa as três portas de consulta do contexto de mensagens com dados em memória. */
public class FakeMessagingDirectories implements ParticipantDirectory, AccountResolver, ContentAuthors {

	private final Map<AccountId, Kind> kinds = new HashMap<>();
	private final Map<NgoId, AccountId> ngos = new HashMap<>();
	private final Map<VolunteerId, AccountId> volunteers = new HashMap<>();
	private final Map<PostId, AccountId> postAuthors = new HashMap<>();
	private final Map<CommentId, AccountId> commentAuthors = new HashMap<>();

	public AccountId account(Kind kind) {
		AccountId id = AccountId.generate();
		kinds.put(id, kind);
		return id;
	}

	public NgoId ngoOf(AccountId account) {
		NgoId id = NgoId.generate();
		ngos.put(id, account);
		return id;
	}

	public VolunteerId volunteerOf(AccountId account) {
		VolunteerId id = VolunteerId.generate();
		volunteers.put(id, account);
		return id;
	}

	public PostId postBy(AccountId author) {
		PostId id = PostId.generate();
		postAuthors.put(id, author);
		return id;
	}

	public CommentId commentBy(AccountId author) {
		CommentId id = CommentId.generate();
		commentAuthors.put(id, author);
		return id;
	}

	public void deactivate(AccountId account) {
		kinds.remove(account);
	}

	@Override
	public Optional<Kind> activeKindOf(AccountId accountId) {
		return Optional.ofNullable(kinds.get(accountId));
	}

	@Override
	public Optional<AccountId> accountOfNgo(NgoId ngoId) {
		return Optional.ofNullable(ngos.get(ngoId));
	}

	@Override
	public Optional<AccountId> accountOfVolunteer(VolunteerId volunteerId) {
		return Optional.ofNullable(volunteers.get(volunteerId));
	}

	@Override
	public Optional<AccountId> authorOfPost(PostId postId) {
		return Optional.ofNullable(postAuthors.get(postId));
	}

	@Override
	public Optional<AccountId> authorOfComment(CommentId commentId) {
		return Optional.ofNullable(commentAuthors.get(commentId));
	}
}
