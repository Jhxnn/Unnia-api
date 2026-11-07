package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.AdminPolicy;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.fakes.InMemoryNgoProfileRepository;
import com.Unnia.identity.application.usecase.ListNgosForReviewUseCase.NgoReview;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.AccountStatus;
import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VerificationStatus;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListNgosForReviewUseCaseTest {

	private static final String[] CNPJS = { "11222333000181", "45723174000110", "11444777000161", "34028316000103" };

	private InMemoryAccountRepository accounts;
	private InMemoryNgoProfileRepository ngos;
	private ListNgosForReviewUseCase useCase;
	private Account admin;
	private int created;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		ngos = new InMemoryNgoProfileRepository();
		useCase = new ListNgosForReviewUseCase(new AdminPolicy(accounts), ngos, accounts);
		admin = accounts.save(Account.register(Email.of("admin@unnia.com"), PasswordHash.of("h"), Role.ADMIN));
		created = 0;
	}

	private NgoProfile ngo(String name) {
		int index = created++;
		Account account = accounts.save(Account.register(Email.of("ong" + index + "@exemplo.com"),
				PasswordHash.of("h"), Role.NGO));
		return ngos.save(NgoProfile.create(account, Cnpj.of(CNPJS[index]), DisplayName.of(name),
				Description.of("Descrição de " + name), City.of("Recife")));
	}

	@Test
	void listsTheNgosInTheRequestedStatusWithTheDataNeededToDecide() {
		NgoProfile pending = ngo("Mãos Solidárias");

		List<NgoReview> result = useCase.execute(admin.id(), VerificationStatus.PENDING, 0, 10);

		assertThat(result).singleElement().satisfies(review -> {
			assertThat(review.ngoId()).isEqualTo(pending.id());
			assertThat(review.accountId()).isEqualTo(pending.accountId());
			assertThat(review.name()).isEqualTo("Mãos Solidárias");
			assertThat(review.cnpj()).isEqualTo("11.222.333/0001-81");
			assertThat(review.description()).isEqualTo("Descrição de Mãos Solidárias");
			assertThat(review.city()).isEqualTo("Recife");
			assertThat(review.email()).isEqualTo("ong0@exemplo.com");
			assertThat(review.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
			assertThat(review.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
		});
	}

	@Test
	void filtersByVerificationStatus() {
		NgoProfile verified = ngo("Aprovada");
		verified.verify();
		NgoProfile rejected = ngo("Recusada");
		rejected.reject();
		ngo("Pendente");

		assertThat(useCase.execute(admin.id(), VerificationStatus.PENDING, 0, 10)).extracting(NgoReview::name)
				.containsExactly("Pendente");
		assertThat(useCase.execute(admin.id(), VerificationStatus.VERIFIED, 0, 10)).extracting(NgoReview::name)
				.containsExactly("Aprovada");
		assertThat(useCase.execute(admin.id(), VerificationStatus.REJECTED, 0, 10)).extracting(NgoReview::name)
				.containsExactly("Recusada");
	}

	@Test
	void ordersByNameAndPaginates() {
		ngo("Casa Verde");
		ngo("Abrigo Esperança");
		ngo("Banco de Alimentos");

		assertThat(useCase.execute(admin.id(), VerificationStatus.PENDING, 0, 2)).extracting(NgoReview::name)
				.containsExactly("Abrigo Esperança", "Banco de Alimentos");
		assertThat(useCase.execute(admin.id(), VerificationStatus.PENDING, 1, 2)).extracting(NgoReview::name)
				.containsExactly("Casa Verde");
	}

	@Test
	void showsWhenTheOwnerAccountIsSuspended() {
		NgoProfile suspended = ngo("Suspensa");
		accounts.findById(suspended.accountId()).orElseThrow().suspend();

		assertThat(useCase.execute(admin.id(), VerificationStatus.PENDING, 0, 10)).singleElement()
				.satisfies(review -> assertThat(review.accountStatus()).isEqualTo(AccountStatus.SUSPENDED));
	}

	@Test
	void anEmptyQueueIsAnEmptyList() {
		assertThat(useCase.execute(admin.id(), VerificationStatus.PENDING, 0, 10)).isEmpty();
	}

	@Test
	void onlyAdminsCanReviewNgos() {
		Account volunteer = accounts.save(Account.register(Email.of("vol@exemplo.com"), PasswordHash.of("h"),
				Role.VOLUNTEER));
		NgoProfile someNgo = ngo("Qualquer");

		assertThatThrownBy(() -> useCase.execute(volunteer.id(), VerificationStatus.PENDING, 0, 10))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(someNgo.accountId(), VerificationStatus.PENDING, 0, 10))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(AccountId.generate(), VerificationStatus.PENDING, 0, 10))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void validatesTheRequest() {
		assertThatThrownBy(() -> useCase.execute(admin.id(), null, 0, 10)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(admin.id(), VerificationStatus.PENDING, -1, 10))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(admin.id(), VerificationStatus.PENDING, 0, 0))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(admin.id(), VerificationStatus.PENDING, 0,
				ListNgosForReviewUseCase.MAX_PAGE_SIZE + 1)).isInstanceOf(DomainException.class);
	}
}
