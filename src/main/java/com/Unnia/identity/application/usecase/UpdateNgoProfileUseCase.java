package com.Unnia.identity.application.usecase;

import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.vo.Cep;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.StateCode;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;

public class UpdateNgoProfileUseCase {

	public record Command(AccountId accountId, String name, String description, String city, String state,
			String cep) {

		/** Sem estado nem CEP (nulos): o perfil mantém os que já tem; em branco, limpa. */
		public Command(AccountId accountId, String name, String description, String city) {
			this(accountId, name, description, city, null, null);
		}
	}

	private final NgoProfileRepository ngos;

	public UpdateNgoProfileUseCase(NgoProfileRepository ngos) {
		this.ngos = ngos;
	}

	public void execute(Command command) {
		NgoProfile profile = ngos.findByAccountId(command.accountId())
				.orElseThrow(() -> new NotFoundException("Perfil de ONG não encontrado"));

		profile.updateDetails(DisplayName.of(command.name()), Description.of(command.description()),
				City.of(command.city()),
				command.state() == null ? profile.state().orElse(null) : StateCode.ofNullable(command.state()),
				command.cep() == null ? profile.cep().orElse(null) : Cep.ofNullable(command.cep()));

		ngos.save(profile);
	}
}
