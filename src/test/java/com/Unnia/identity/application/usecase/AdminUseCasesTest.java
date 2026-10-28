package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.AdminPolicy;
import com.Unnia.identity.application.fakes.FakePasswordEncoder;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.fakes.InMemoryNgoProfileRepository;
import com.Unnia.identity.domain.event.AccountActivated;
import com.Unnia.identity.domain.event.AccountRegistered;
import com.Unnia.identity.domain.event.AccountSuspended;
import com.Unnia.identity.domain.event.NgoRejected;
import com.Unnia.identity.domain.event.NgoVerified;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VerificationStatus;
import com.Unnia.shared.application.ConflictException;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.application.RecordingEventPublisher;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminUseCasesTest {

	private InMemoryAccountRepository accounts;
	private InMemoryNgoProfileRepository ngos;
	private RecordingEventPublisher publisher;
	private AdminPolicy policy;
	private Account admin;
	private Account volunteer;
	private NgoProfile ngo;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		ngos = new InMemoryNgoProfileRepository();
		publisher = new RecordingEventPublisher();
		policy = new AdminPolicy(accounts);
		admin = accounts.save(Account.register(Email.of("admin@unnia.com"), PasswordHash.of("h"), Role.ADMIN));
		volunteer = accounts.save(Account.register(Email.of("vol@exemplo.com"), PasswordHash.of("h"),
				Role.VOLUNTEER));
		Account ngoAccount = accounts.save(Account.register(Email.of("ong@exemplo.com"), PasswordHash.of("h"),
				Role.NGO));
		ngo = ngos.save(NgoProfile.create(ngoAccount, Cnpj.of("11222333000181"), DisplayName.of("Mãos"),
				Description.EMPTY, City.of("Recife")));
		// os agregados do cenário já "existiam": descarta os eventos de criação pendentes
		admin.pullDomainEvents();
		volunteer.pullDomainEvents();
		ngoAccount.pullDomainEvents();
		ngo.pullDomainEvents();
	}

	@Test
	void adminVerifiesNgo() {
		new VerifyNgoUseCase(policy, ngos, publisher)
				.execute(new VerifyNgoUseCase.Command(admin.id(), ngo.id()));

		assertThat(ngos.findById(ngo.id()).orElseThrow().verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
		assertThat(publisher.published()).contains(new NgoVerified(ngo.id(), ngo.accountId()));
	}

	@Test
	void adminRejectsNgo() {
		new RejectNgoUseCase(policy, ngos, publisher)
				.execute(new RejectNgoUseCase.Command(admin.id(), ngo.id()));

		assertThat(ngos.findById(ngo.id()).orElseThrow().verificationStatus()).isEqualTo(VerificationStatus.REJECTED);
		assertThat(publisher.published()).contains(new NgoRejected(ngo.id(), ngo.accountId()));
	}

	@Test
	void verifyFailsForUnknownNgoAndDecidedNgo() {
		VerifyNgoUseCase useCase = new VerifyNgoUseCase(policy, ngos, publisher);

		assertThatThrownBy(() -> useCase.execute(new VerifyNgoUseCase.Command(admin.id(), NgoId.generate())))
				.isInstanceOf(NotFoundException.class);
		useCase.execute(new VerifyNgoUseCase.Command(admin.id(), ngo.id()));
		assertThatThrownBy(() -> useCase.execute(new VerifyNgoUseCase.Command(admin.id(), ngo.id())))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void nonAdminsCannotVerifyOrReject() {
		VerifyNgoUseCase verify = new VerifyNgoUseCase(policy, ngos, publisher);
		RejectNgoUseCase reject = new RejectNgoUseCase(policy, ngos, publisher);

		assertThatThrownBy(() -> verify.execute(new VerifyNgoUseCase.Command(volunteer.id(), ngo.id())))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> reject.execute(new RejectNgoUseCase.Command(ngo.accountId(), ngo.id())))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> verify.execute(new VerifyNgoUseCase.Command(AccountId.generate(), ngo.id())))
				.isInstanceOf(ForbiddenException.class);
		assertThat(ngos.findById(ngo.id()).orElseThrow().verificationStatus()).isEqualTo(VerificationStatus.PENDING);
	}

	@Test
	void suspendedAdminLosesPrivileges() {
		admin.suspend();

		assertThatThrownBy(() -> new VerifyNgoUseCase(policy, ngos, publisher)
				.execute(new VerifyNgoUseCase.Command(admin.id(), ngo.id()))).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void adminSuspendsAndReactivatesAccount() {
		new SuspendAccountUseCase(policy, accounts, publisher)
				.execute(new SuspendAccountUseCase.Command(admin.id(), volunteer.id()));
		assertThat(accounts.findById(volunteer.id()).orElseThrow().isActive()).isFalse();

		new ActivateAccountUseCase(policy, accounts, publisher)
				.execute(new ActivateAccountUseCase.Command(admin.id(), volunteer.id()));
		assertThat(accounts.findById(volunteer.id()).orElseThrow().isActive()).isTrue();

		assertThat(publisher.published()).containsExactly(new AccountSuspended(volunteer.id()),
				new AccountActivated(volunteer.id()));
	}

	@Test
	void adminsCannotBeSuspendedAndUnknownAccountsAreNotFound() {
		SuspendAccountUseCase suspend = new SuspendAccountUseCase(policy, accounts, publisher);

		assertThatThrownBy(() -> suspend.execute(new SuspendAccountUseCase.Command(admin.id(), admin.id())))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> suspend.execute(new SuspendAccountUseCase.Command(admin.id(),
				AccountId.generate()))).isInstanceOf(NotFoundException.class);
	}

	@Test
	void nonAdminCannotSuspendOrActivate() {
		assertThatThrownBy(() -> new SuspendAccountUseCase(policy, accounts, publisher)
				.execute(new SuspendAccountUseCase.Command(volunteer.id(), ngo.accountId())))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> new ActivateAccountUseCase(policy, accounts, publisher)
				.execute(new ActivateAccountUseCase.Command(volunteer.id(), ngo.accountId())))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void registerAdminCreatesOnceAndIsIdempotent() {
		InMemoryAccountRepository empty = new InMemoryAccountRepository();
		RegisterAdminUseCase useCase = new RegisterAdminUseCase(empty, new FakePasswordEncoder(), publisher);
		RegisterAdminUseCase.Command command = new RegisterAdminUseCase.Command("root@unnia.com", "senhaAdmin1");

		RegisterAdminUseCase.Result first = useCase.execute(command);
		RegisterAdminUseCase.Result second = useCase.execute(command);

		assertThat(first.created()).isTrue();
		assertThat(second.created()).isFalse();
		assertThat(second.accountId()).isEqualTo(first.accountId());
		assertThat(empty.size()).isEqualTo(1);
		assertThat(publisher.published()).containsExactly(new AccountRegistered(first.accountId(), Role.ADMIN));
	}

	@Test
	void registerAdminRefusesEmailOwnedByAnotherRole() {
		RegisterAdminUseCase useCase = new RegisterAdminUseCase(accounts, new FakePasswordEncoder(), publisher);

		assertThatThrownBy(() -> useCase.execute(new RegisterAdminUseCase.Command("vol@exemplo.com", "senhaAdmin1")))
				.isInstanceOf(ConflictException.class);
	}
}
