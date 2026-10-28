package com.Unnia.shared.infrastructure.acl;

import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.application.port.AccountDirectory;
import com.Unnia.social.application.port.EventReferences;
import org.springframework.stereotype.Component;

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
