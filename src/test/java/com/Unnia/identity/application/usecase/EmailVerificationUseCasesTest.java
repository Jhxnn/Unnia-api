package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.EmailVerificationIssuer;
import com.Unnia.identity.application.fakes.FakeCodes;
import com.Unnia.identity.application.fakes.FakeTokens;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.fakes.InMemoryEmailVerificationTokenRepository;
import com.Unnia.identity.application.fakes.InMemoryNgoProfileRepository;
import com.Unnia.identity.application.fakes.InMemoryVolunteerProfileRepository;
import com.Unnia.identity.application.fakes.RecordingVerificationEmailSender;
import com.Unnia.identity.application.handler.SendVerificationEmailHandler;
import com.Unnia.identity.application.usecase.VerifyEmailUseCase.Result;
import com.Unnia.identity.domain.event.AccountRegistered;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.EmailVerificationToken;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.application.ServiceUnavailableException;
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

	private static final String EMAIL = "maria@exemplo.com";

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
	private InMemoryAccountRepository accounts;
	private InMemoryEmailVerificationTokenRepository tokens;
	private RecordingVerificationEmailSender sender;
	private EmailVerificationIssuer issuer;
	private VerifyEmailUseCase verify;
	private ResendEmailVerificationUseCase resend;
	private Account account;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		tokens = new InMemoryEmailVerificationTokenRepository();
		sender = new RecordingVerificationEmailSender();
		FakeTokens hasher = new FakeTokens();
		InMemoryVolunteerProfileRepository volunteers = new InMemoryVolunteerProfileRepository();
		issuer = new EmailVerificationIssuer(tokens, new FakeCodes(), hasher, sender, new InMemoryNgoProfileRepository(),
				volunteers, clock);
		verify = new VerifyEmailUseCase(accounts, tokens, hasher, clock);
		resend = new ResendEmailVerificationUseCase(accounts, issuer);

		account = accounts.save(Account.register(Email.of(EMAIL), PasswordHash.of("h"), Role.VOLUNTEER));
		volunteers.save(VolunteerProfile.create(account, DisplayName.of("Maria Souza"), Description.of("Bio"),
				City.of("Recife")));
	}

	private void register() {
		new SendVerificationEmailHandler(accounts, issuer).handle(new AccountRegistered(account.id(), Role.VOLUNTEER));
	}

	private Result verify(String email, String code) {
		return verify.execute(new VerifyEmailUseCase.Command(email, code));
	}

	@Test
	void registrationSendsASixDigitCodeWithTheName() {
		register();

		assertThat(sender.sent()).singleElement().satisfies(sent -> {
			assertThat(sent.to()).isEqualTo(Email.of(EMAIL));
			assertThat(sent.recipientName()).isEqualTo("Maria Souza");
			assertThat(sent.code()).isEqualTo("111111");
		});
	}

	@Test
	void onlyTheHashOfTheCodeIsStoredAndItExpiresInFifteenMinutes() {
		register();

		assertThat(tokens.all()).singleElement().satisfies(stored -> {
			assertThat(stored.tokenHash()).isEqualTo("hash:" + EmailVerificationToken.secretOf(account.id(), "111111"));
			assertThat(stored.tokenHash()).doesNotContain("111111:").startsWith("hash:");
			assertThat(stored.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
		});
	}

	@Test
	void theRightCodeConfirmsTheAccount() {
		register();

		assertThat(verify(EMAIL, "111111")).isEqualTo(Result.CONFIRMED);

		assertThat(accounts.findById(account.id()).orElseThrow().isEmailVerified()).isTrue();
	}

	@Test
	void theCodeIsTrimmedAndTheEmailIsCaseInsensitive() {
		register();

		assertThat(verify(" Maria@Exemplo.com ", " 111111 ")).isEqualTo(Result.CONFIRMED);
	}

	@Test
	void aWrongCodeIsRejectedAndTheAccountStaysUnconfirmed() {
		register();

		assertThat(verify(EMAIL, "999999")).isEqualTo(Result.REJECTED);

		assertThat(account.isEmailVerified()).isFalse();
		assertThat(tokens.all().get(0).failedAttempts()).isEqualTo(1);
	}

	@Test
	void afterFiveWrongAttemptsEvenTheRightCodeIsRefused() {
		register();
		for (int i = 0; i < EmailVerificationToken.MAX_FAILED_ATTEMPTS; i++) {
			assertThat(verify(EMAIL, "00000" + i)).isEqualTo(Result.REJECTED);
		}

		assertThat(verify(EMAIL, "111111")).isEqualTo(Result.REJECTED);
		assertThat(account.isEmailVerified()).isFalse();
	}

	@Test
	void aNewCodeStartsFreshAfterTheOldOneWasExhausted() {
		register();
		for (int i = 0; i < EmailVerificationToken.MAX_FAILED_ATTEMPTS; i++) {
			verify(EMAIL, "00000" + i);
		}
		clock.advance(EmailVerificationIssuer.RESEND_COOLDOWN.plusSeconds(1));
		resend.execute(new ResendEmailVerificationUseCase.Command(EMAIL));

		assertThat(verify(EMAIL, "222222")).isEqualTo(Result.CONFIRMED);
	}

	@Test
	void anExpiredCodeIsRejected() {
		register();
		clock.advance(EmailVerificationIssuer.TTL.plusSeconds(1));

		assertThat(verify(EMAIL, "111111")).isEqualTo(Result.REJECTED);
		assertThat(account.isEmailVerified()).isFalse();
	}

	@Test
	void unknownBlankOrMalformedInputsAreAllJustRejected() {
		register();

		assertThat(verify("ninguem@exemplo.com", "111111")).isEqualTo(Result.REJECTED);
		assertThat(verify("isso-nao-e-email", "111111")).isEqualTo(Result.REJECTED);
		assertThat(verify(null, "111111")).isEqualTo(Result.REJECTED);
		assertThat(verify(EMAIL, " ")).isEqualTo(Result.REJECTED);
		assertThat(verify(EMAIL, null)).isEqualTo(Result.REJECTED);
		assertThat(account.isEmailVerified()).isFalse();
	}

	@Test
	void anAccountWithNoCodeYetIsRejected() {
		assertThat(verify(EMAIL, "111111")).isEqualTo(Result.REJECTED);
	}

	@Test
	void repeatingTheCodeThatAlreadyConfirmedTheAccountIsNotAnError() {
		register();
		verify(EMAIL, "111111");

		assertThat(verify(EMAIL, "111111")).isEqualTo(Result.CONFIRMED);
	}

	@Test
	void guessingOnAConfirmedAccountRevealsNothing() {
		register();
		verify(EMAIL, "111111");

		assertThat(verify(EMAIL, "123456")).isEqualTo(Result.REJECTED);
	}

	@Test
	void resendingReplacesTheOldCodeSoOnlyTheNewestWorks() {
		register();
		clock.advance(EmailVerificationIssuer.RESEND_COOLDOWN.plusSeconds(1));

		resend.execute(new ResendEmailVerificationUseCase.Command(EMAIL));

		assertThat(sender.sent()).extracting(RecordingVerificationEmailSender.Sent::code).containsExactly("111111", "222222");
		assertThat(verify(EMAIL, "111111")).isEqualTo(Result.REJECTED);
		assertThat(verify(EMAIL, "222222")).isEqualTo(Result.CONFIRMED);
	}

	@Test
	void resendingTwiceInARowIsIgnoredDuringTheCooldown() {
		register();

		resend.execute(new ResendEmailVerificationUseCase.Command(EMAIL));
		resend.execute(new ResendEmailVerificationUseCase.Command(EMAIL));

		assertThat(sender.sent()).hasSize(1);
	}

	@Test
	void resendingAfterTheCooldownWorksAgain() {
		register();
		clock.advance(Duration.ofSeconds(61));
		resend.execute(new ResendEmailVerificationUseCase.Command(EMAIL));
		clock.advance(Duration.ofSeconds(61));

		resend.execute(new ResendEmailVerificationUseCase.Command(EMAIL));

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

		resend.execute(new ResendEmailVerificationUseCase.Command(EMAIL));
		register();

		assertThat(sender.sent()).isEmpty();
		assertThat(tokens.all()).isEmpty();
	}

	@Test
	void suspendedAccountsGetNoCodesAndCannotVerify() {
		register();
		account.suspend();

		resend.execute(new ResendEmailVerificationUseCase.Command(EMAIL));

		assertThat(sender.sent()).hasSize(1);
		assertThat(verify(EMAIL, "111111")).isEqualTo(Result.REJECTED);
	}

	@Test
	void aMailServerOutageSurfacesOnResendSoThePersonCanTryAgain() {
		sender.goDown();

		assertThatThrownBy(() -> resend.execute(new ResendEmailVerificationUseCase.Command(EMAIL)))
				.isInstanceOf(ServiceUnavailableException.class);
	}

	@Test
	void theHandlerListensToAccountRegistrations() {
		assertThat(new SendVerificationEmailHandler(accounts, issuer).eventType()).isEqualTo(AccountRegistered.class);
	}
}
