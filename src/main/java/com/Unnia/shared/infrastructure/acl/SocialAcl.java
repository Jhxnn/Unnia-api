package com.Unnia.shared.infrastructure.acl;

import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.social.application.port.AuthorDirectory;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.application.port.AccountDirectory;
import com.Unnia.social.application.port.EventReferences;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Camada anticorrupção: o contexto social enxerga identidade e eventos apenas por estas portas. */
final class SocialAcl {

	private SocialAcl() {
	}

	@Component
	static class IdentityAccountDirectory implements AccountDirectory {

		private final AccountRepository accounts;

		IdentityAccountDirectory(AccountRepository accounts) {
			this.accounts = accounts;
		}

		@Override
		public boolean isActive(AccountId accountId) {
			return accounts.findById(accountId).map(Account::isActive).orElse(false);
		}
	}

	@Component
	static class IdentityAuthorDirectory implements AuthorDirectory {

		private static final String TEAM_NAME = "Equipe Unnia";

		private final AccountRepository accounts;
		private final NgoProfileRepository ngos;
		private final VolunteerProfileRepository volunteers;

		IdentityAuthorDirectory(AccountRepository accounts, NgoProfileRepository ngos,
				VolunteerProfileRepository volunteers) {
			this.accounts = accounts;
			this.ngos = ngos;
			this.volunteers = volunteers;
		}

		@Override
		public Map<AccountId, Author> authorsOf(Collection<AccountId> accountIds) {
			Map<AccountId, Author> result = new HashMap<>();
			for (Account account : accounts.findAllById(accountIds)) {
				result.put(account.id(), switch (account.role()) {
					case NGO -> ngos.findByAccountId(account.id())
							.map(profile -> new Author(profile.name().value(), "NGO", profile.id().value()))
							.orElse(UNKNOWN);
					case VOLUNTEER -> volunteers.findByAccountId(account.id())
							.map(profile -> new Author(profile.name().value(), "VOLUNTEER", profile.id().value()))
							.orElse(UNKNOWN);
					case ADMIN -> new Author(TEAM_NAME, "ADMIN", null);
				});
			}
			return result;
		}
	}

	@Component
	static class EventsEventReferences implements EventReferences {

		private final EventRepository events;

		EventsEventReferences(EventRepository events) {
			this.events = events;
		}

		@Override
		public boolean exists(EventId eventId) {
			return events.findById(eventId).isPresent();
		}
	}
}
