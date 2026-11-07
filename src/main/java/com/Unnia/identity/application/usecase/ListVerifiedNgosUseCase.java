package com.Unnia.identity.application.usecase;

import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.VerificationStatus;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.vo.StateCode;

import java.util.List;

/** Vitrine pública: as ONGs verificadas, em ordem alfabética, para quem quer conhecê-las e segui-las. */
public class ListVerifiedNgosUseCase {

	public static final int MAX_PAGE_SIZE = 50;

	public record NgoCard(NgoId ngoId, AccountId accountId, String name, String description, String city,
			String state) {
	}

	private final NgoProfileRepository ngos;

	public ListVerifiedNgosUseCase(NgoProfileRepository ngos) {
		this.ngos = ngos;
	}

	public List<NgoCard> execute(int page, int size) {
		Guard.check(page >= 0, "Página inválida");
		Guard.check(size >= 1 && size <= MAX_PAGE_SIZE, "O tamanho da página deve ficar entre 1 e " + MAX_PAGE_SIZE);
		return ngos.findByVerificationStatus(VerificationStatus.VERIFIED, page, size).stream()
				.map(ListVerifiedNgosUseCase::toCard).toList();
	}

	private static NgoCard toCard(NgoProfile profile) {
		return new NgoCard(profile.id(), profile.accountId(), profile.name().value(), profile.description().value(),
				profile.city().value(),
				profile.state().map(StateCode::value).orElse(null));
	}
}
