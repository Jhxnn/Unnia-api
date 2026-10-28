package com.Unnia.shared.infrastructure.acl;

import com.Unnia.events.application.port.NgoDirectory;
import com.Unnia.events.application.port.VolunteerDirectory;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Camada anticorrupção: o contexto de eventos enxerga o de identidade apenas por estas portas. */
final class EventsAcl {

	private EventsAcl() {
	}

	@Component
	static class IdentityNgoDirectory implements NgoDirectory {

		private final NgoProfileRepository ngos;

		IdentityNgoDirectory(NgoProfileRepository ngos) {
			this.ngos = ngos;
		}

		@Override
		public Optional<NgoId> findNgoIdByAccount(AccountId accountId) {
			return ngos.findByAccountId(accountId).map(NgoProfile::id);
		}

		@Override
		public boolean isVerified(NgoId ngoId) {
			return ngos.findById(ngoId).map(NgoProfile::isVerified).orElse(false);
		}
	}

	@Component
	static class IdentityVolunteerDirectory implements VolunteerDirectory {

		private final VolunteerProfileRepository volunteers;

		IdentityVolunteerDirectory(VolunteerProfileRepository volunteers) {
			this.volunteers = volunteers;
		}

		@Override
		public Optional<VolunteerId> findVolunteerIdByAccount(AccountId accountId) {
			return volunteers.findByAccountId(accountId).map(VolunteerProfile::id);
		}
	}
}
