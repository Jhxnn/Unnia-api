package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.SessionPolicy;
import com.Unnia.identity.application.fakes.FakePasswordEncoder;
import com.Unnia.identity.application.fakes.FakeTokens;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.fakes.InMemoryRefreshTokenRepository;
import com.Unnia.identity.application.usecase.RefreshSessionUseCase.Refreshed;
import com.Unnia.identity.application.usecase.RefreshSessionUseCase.Rejected;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.application.UnauthorizedException;
import com.Unnia.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionUseCasesTest {

	private static final Duration TTL = Duration.ofDays(30);

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
	private InMemoryAccountRepository accounts;
	private InMemoryRefreshTokenRepository tokens;
	private FakeTokens crypto;
	private Account account;
	private StartSessionUseCase start;
	private RefreshSessionUseCase refresh;
	private EndSessionUseCase end;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		tokens = new InMemoryRefreshTokenRepository();
		crypto = new FakeTokens();
		account = accounts.save(Account.register(Email.of("vol@exemplo.com"), PasswordHash.of("enc:senha1234"),
				Role.VOLUNTEER));
		SessionPolicy policy = new SessionPolicy(TTL);
		start = new StartSessionUseCase(tokens, crypto, crypto, policy, clock);
		refresh = new RefreshSessionUseCase(tokens, accounts, crypto, crypto, policy, clock);
		end = new EndSessionUseCase(tokens, crypto, clock);
	}

	@Test
	void startingASessionReturnsTheRawTokenButStoresOnlyItsHash() {
		StartSessionUseCase.Result session = start.execute(account.id());

		assertThat(session.refreshToken()).isEqualTo("token-1");
		assertThat(session.expiresAt()).isEqualTo(clock.instant().plus(TTL));
		assertThat(tokens.all()).singleElement().satisfies(t -> {
			assertThat(t.tokenHash()).isEqualTo("hash:token-1");
			assertThat(t.tokenHash()).isNotEqualTo(session.refreshToken());
			assertThat(t.accountId()).isEqualTo(account.id());
		});
	}

	@Test
	void refreshingRotatesTheTokenAndIdentifiesTheAccount() {
		String first = start.execute(account.id()).refreshToken();
		clock.advance(Duration.ofHours(1));

		Refreshed renewed = (Refreshed) refresh.execute(first);

		assertThat(renewed.accountId()).isEqualTo(account.id());
		assertThat(renewed.role()).isEqualTo(Role.VOLUNTEER);
		assertThat(renewed.refreshToken()).isEqualTo("token-2").isNotEqualTo(first);
		assertThat(renewed.refreshExpiresAt()).isEqualTo(clock.instant().plus(TTL));
		assertThat(tokens.all()).hasSize(2);
		assertThat(tokens.all().get(0).wasUsed()).isTrue();
		assertThat(tokens.all().get(1).familyId()).isEqualTo(tokens.all().get(0).familyId());
	}

	@Test
	void theSessionKeepsRenewingAlongTheChain() {
		String token = start.execute(account.id()).refreshToken();

		for (int i = 0; i < 3; i++) {
			clock.advance(Duration.ofDays(10));
			Refreshed renewed = (Refreshed) refresh.execute(token);
			token = renewed.refreshToken();
		}

		assertThat(tokens.all()).hasSize(4);
		assertThat(refresh.execute(token)).isInstanceOf(Refreshed.class);
	}

	@Test
	void reusingAnAlreadyUsedTokenRevokesTheWholeSession() {
		String stolen = start.execute(account.id()).refreshToken();
		String legitimate = ((Refreshed) refresh.execute(stolen)).refreshToken();

		assertThat(refresh.execute(stolen)).isInstanceOf(Rejected.class);

		// o sucessor legítimo também deixou de valer: o titular precisa fazer login de novo
		assertThat(refresh.execute(legitimate)).isInstanceOf(Rejected.class);
		assertThat(tokens.all()).allSatisfy(t -> assertThat(t.revokedAt()).isPresent());
	}

	@Test
	void anotherSessionOfTheSameAccountSurvivesTheReuse() {
		String stolen = start.execute(account.id()).refreshToken();
		String otherDevice = start.execute(account.id()).refreshToken();
		refresh.execute(stolen);

		refresh.execute(stolen);

		assertThat(refresh.execute(otherDevice)).isInstanceOf(Refreshed.class);
	}

	@Test
	void anExpiredTokenIsRejected() {
		String token = start.execute(account.id()).refreshToken();

		clock.advance(TTL.plusSeconds(1));

		assertThat(refresh.execute(token)).isInstanceOf(Rejected.class);
	}

	@Test
	void unknownBlankAndNullTokensAreRejected() {
		start.execute(account.id());

		assertThat(refresh.execute("nao-existe")).isInstanceOf(Rejected.class);
		assertThat(refresh.execute("  ")).isInstanceOf(Rejected.class);
		assertThat(refresh.execute(null)).isInstanceOf(Rejected.class);
		assertThat(tokens.all()).singleElement().satisfies(t -> assertThat(t.wasUsed()).isFalse());
	}

	@Test
	void aSuspendedAccountCannotRefreshAndItsSessionIsRevoked() {
		String token = start.execute(account.id()).refreshToken();
		account.suspend();

		assertThat(refresh.execute(token)).isInstanceOf(Rejected.class);

		account.activate();
		assertThat(refresh.execute(token)).isInstanceOf(Rejected.class);
	}

	@Test
	void endingASessionRevokesItAndIsIdempotent() {
		String token = start.execute(account.id()).refreshToken();

		end.execute(token);
		end.execute(token);
		end.execute("desconhecido");
		end.execute(null);

		assertThat(refresh.execute(token)).isInstanceOf(Rejected.class);
	}

	@Test
	void endingOneSessionLeavesTheOthersAlone() {
		String phone = start.execute(account.id()).refreshToken();
		String laptop = start.execute(account.id()).refreshToken();

		end.execute(phone);

		assertThat(refresh.execute(phone)).isInstanceOf(Rejected.class);
		assertThat(refresh.execute(laptop)).isInstanceOf(Refreshed.class);
	}

	@Test
	void changingThePasswordEndsEverySessionOfThatAccountOnly() {
		Account other = accounts.save(Account.register(Email.of("outra@exemplo.com"), PasswordHash.of("enc:x1234567"),
				Role.VOLUNTEER));
		String mine = start.execute(account.id()).refreshToken();
		String mineElsewhere = start.execute(account.id()).refreshToken();
		String theirs = start.execute(other.id()).refreshToken();

		new ChangePasswordUseCase(accounts, new FakePasswordEncoder(), tokens, clock).execute(
				new ChangePasswordUseCase.Command(account.id(), "senha1234", "novaSenha99"));

		assertThat(refresh.execute(mine)).isInstanceOf(Rejected.class);
		assertThat(refresh.execute(mineElsewhere)).isInstanceOf(Rejected.class);
		assertThat(refresh.execute(theirs)).isInstanceOf(Refreshed.class);
	}

	@Test
	void aFailedPasswordChangeKeepsTheSessions() {
		String token = start.execute(account.id()).refreshToken();
		ChangePasswordUseCase change = new ChangePasswordUseCase(accounts, new FakePasswordEncoder(), tokens, clock);

		assertThatThrownBy(() -> change.execute(
				new ChangePasswordUseCase.Command(account.id(), "senha-errada1", "novaSenha99")))
				.isInstanceOf(UnauthorizedException.class);
		assertThatThrownBy(() -> change.execute(
				new ChangePasswordUseCase.Command(account.id(), "senha1234", "fraca")))
				.isInstanceOf(DomainException.class);

		assertThat(refresh.execute(token)).isInstanceOf(Refreshed.class);
	}

	@Test
	void sessionPolicyRequiresAPositiveDuration() {
		assertThatThrownBy(() -> new SessionPolicy(Duration.ZERO)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> new SessionPolicy(Duration.ofDays(-1))).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> new SessionPolicy(null)).isInstanceOf(DomainException.class);
	}
}
