package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.fakes.InMemoryNgoProfileRepository;
import com.Unnia.identity.application.usecase.ListVerifiedNgosUseCase.NgoCard;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListVerifiedNgosUseCaseTest {

	private static final String[] CNPJS = { "11222333000181", "45723174000110", "11444777000161" };

	private InMemoryAccountRepository accounts;
	private InMemoryNgoProfileRepository ngos;
	private ListVerifiedNgosUseCase useCase;
	private int created;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		ngos = new InMemoryNgoProfileRepository();
		useCase = new ListVerifiedNgosUseCase(ngos);
	}

	private NgoProfile ngo(String name, boolean verified) {
		int index = created++;
		Account account = accounts.save(Account.register(Email.of("o" + index + "@x.com"), PasswordHash.of("h"), Role.NGO));
		NgoProfile profile = NgoProfile.create(account, Cnpj.of(CNPJS[index]), DisplayName.of(name),
				Description.of("Sobre " + name), City.of("Recife"));
		if (verified) {
			profile.verify();
		}
		return ngos.save(profile);
	}

	@Test
	void listsOnlyVerifiedNgosAlphabeticallyWithTheAccountToFollowOrMessage() {
		ngo("Zeta", true);
		NgoProfile alfa = ngo("Alfa", true);
		ngo("Pendente", false);

		List<NgoCard> cards = useCase.execute(0, 10);

		assertThat(cards).extracting(NgoCard::name).containsExactly("Alfa", "Zeta");
		assertThat(cards.get(0).accountId()).isEqualTo(alfa.accountId());
		assertThat(cards.get(0).ngoId()).isEqualTo(alfa.id());
	}

	@Test
	void paginatesAndValidatesTheRequest() {
		ngo("A", true);
		ngo("B", true);
		ngo("C", true);

		assertThat(useCase.execute(1, 2)).extracting(NgoCard::name).containsExactly("C");
		assertThatThrownBy(() -> useCase.execute(-1, 10)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(0, ListVerifiedNgosUseCase.MAX_PAGE_SIZE + 1))
				.isInstanceOf(DomainException.class);
	}
}
