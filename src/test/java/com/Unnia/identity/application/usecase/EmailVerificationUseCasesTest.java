package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.EmailVerificationIssuer;
import com.Unnia.identity.application.fakes.FakeTokens;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.fakes.InMemoryEmailVerificationTokenRepository;
import com.Unnia.identity.application.fakes.InMemoryNgoProfileRepository;
import com.Unnia.identity.application.fakes.InMemoryVolunteerProfileRepository;
import com.Unnia.identity.application.fakes.RecordingVerificationEmailSender;
import com.Unnia.identity.application.handler.SendVerificationEmailHandler;
import com.Unnia.identity.domain.event.AccountRegistered;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.application.ServiceUnavailableException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerificationUseCasesTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
	private InMemoryAccountRepository accounts;
	private InMemoryEmailVerificationTokenRepository tokens;
	private RecordingVerificationEmailSender sender;
	private FakeTokens fakeTokens;
	private EmailVerificationIssuer issuer;
	private VerifyEmailUseCase verify;
	private ResendEmailVerificationUseCase resend;
	private Account account;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		tokens = new InMemoryEmailVerificationTokenRepository();
		sender = new RecordingVerificationEmailSender();
		fakeTokens = new FakeTokens();
		InMemoryVolunteerProfileRepository volunteers = new InMemoryVolunteerProfileRepository();
		issuer = new EmailVerificationIssuer(tokens, fakeTokens, fakeTokens, sender, new InMemoryNgoProfileRepository(),
				volunteers, clock);
		verify = new VerifyEmailUseCase(accounts, tokens, fakeTokens, clock);
		resend = new ResendEmailVerificationUseCase(accounts, issuer);

		account = accounts.save(Account.register(Email.of("maria@exemplo.com"), PasswordHash.of("h"), Role.VOLUNTEER));
		volunteers.save(VolunteerProfile.create(account, DisplayName.of("Maria Souza"), Description.of("Bio"),
				City.of("Recife")));
	}

	private void register() {
		new SendVerificationEmailHandler(accounts, issuer).handle(new AccountRegistered(account.id(), Role.VOLUNTEER));
	}

	@Test
	void registrationSendsTheEmailWithTheNameAndAFreshToken() {
		register();

		assertThat(sender.sent()).singleElement().satisfies(sent -> {
			assertThat(sent.to()).isEqualTo(Email.of("maria@exemplo.com"));
			assertThat(sent.recipientName()).isEqualTo("Maria Souza");
			assertThat(sent.token()).isEqualTo("token-1");
		});
	}

	@Test
	void onlyTheHashOfTheTokenIsStored() {
		register();

		assertThat(tokens.all()).singleElement().satisfies(stored -> {
			assertThat(stored.tokenHash()).isEqualTo("hash:token-1");
			assertThat(stored.expiresAt()).isEqualTo(clock.instant().plus(EmailVerificationIssuer.TTL));
		});
	}

	@Test
	void theLinkConfirmsTheAccount() {
		register();

		verify.execute(new VerifyEmailUseCase.Command("token-1"));

		assertThat(accounts.findById(account.id()).orElseThrow().isEmailVerified()).isTrue();
	}

	@Test
	void openingTheSameLinkAgainIsNotAnError() {
		register();
		verify.execute(new VerifyEmailUseCase.Command("token-1"));

		verify.execute(new VerifyEmailUseCase.Command("token-1"));

		assertThat(account.isEmailVerified()).isTrue();
	}

	@Test
	void anUnknownBlankOrMissingTokenIsRejected() {
		register();

		assertThatThrownBy(() -> verify.execute(new VerifyEmailUseCase.Command("outro"))).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> verify.execute(new VerifyEmailUseCase.Command(" "))).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> verify.execute(new VerifyEmailUseCase.Command(null))).isInstanceOf(DomainException.class);
		assertThat(account.isEmailVerified()).isFalse();
	}

	@Test
	void anExpiredLinkIsRejectedAndTheAccountStaysUnconfirmed() {
		register();
		clock.advance(EmailVerificationIssuer.TTL.plusSeconds(1));

		assertThatThrownBy(() -> verify.execute(new VerifyEmailUseCase.Command("token-1")))
				.isInstanceOf(DomainException.class);
		assertThat(account.isEmailVerified()).isFalse();
	}

	@Test
	void resendingReplacesTheOldLinkSoOnlyTheNewestWorks() {
		register();
		clock.advance(EmailVerificationIssuer.RESEND_COOLDOWN.plusSeconds(1));

		resend.execute(new ResendEmailVerificationUseCase.Command("maria@exemplo.com"));

		assertThat(sender.sent()).extracting(RecordingVerificationEmailSender.Sent::token)
				.containsExactly("token-1", "token-2");
		assertThatThrownBy(() -> verify.execute(new VerifyEmailUseCase.Command("token-1")))
				.isInstanceOf(DomainException.class);
		verify.execute(new VerifyEmailUseCase.Command("token-2"));
		assertThat(account.isEmailVerified()).isTrue();
	}

	@Test
	void resendingTwiceInARowIsIgnoredDuringTheCooldown() {
		register();

		resend.execute(new ResendEmailVerificationUseCase.Command("maria@exemplo.com"));
		resend.execute(new ResendEmailVerificationUseCase.Command("maria@exemplo.com"));

		assertThat(sender.sent()).hasSize(1);
	}

	@Test
	void resendingAfterTheCooldownWorksAgain() {
		register();
		clock.advance(Duration.ofSeconds(61));
		resend.execute(new ResendEmailVerificationUseCase.Command("maria@exemplo.com"));
		clock.advance(Duration.ofSeconds(61));

		resend.execute(new ResendEmailVerificationUseCase.Command("maria@exemplo.com"));

		assertThat(sender.sent()).hasSize(3);
	}

	@Test
	void resendingNeverRevealsWhetherTheEmailExists() {
		resend.execute(new ResendEmailVerificationUseCase.Command("ninguem@exemplo.com"));
		resend.execute(new ResendEmailVerificationUseCase.Command("isso-nao-e-email"));
		resend.execute(new ResendEmailVerificationUseCase.Command(null));

		assertThat(sender.sent()).isEmpty();
	}

	@Test
	void accountsAlreadyConfirmedReceiveNothing() {
		account.verifyEmail();

		resend.execute(new ResendEmailVerificationUseCase.Command("maria@exemplo.com"));
		register();

		assertThat(sender.sent()).isEmpty();
		assertThat(tokens.all()).isEmpty();
	}

	@Test
	void suspendedAccountsDoNotGetResentLinks() {
		account.suspend();

		resend.execute(new ResendEmailVerificationUseCase.Command("maria@exemplo.com"));

		assertThat(sender.sent()).isEmpty();
	}

	@Test
	void aMailServerOutageSurfacesOnResendSoThePersonCanTryAgain() {
		sender.goDown();

		assertThatThrownBy(() -> resend.execute(new ResendEmailVerificationUseCase.Command("maria@exemplo.com")))
				.isInstanceOf(ServiceUnavailableException.class);
	}

	@Test
	void theHandlerListensToAccountRegistrations() {
		assertThat(new SendVerificationEmailHandler(accounts, issuer).eventType()).isEqualTo(AccountRegistered.class);
	}
}
