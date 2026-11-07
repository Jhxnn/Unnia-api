package com.Unnia.identity.infrastructure.mail;

import com.Unnia.identity.domain.model.Email;
import com.Unnia.shared.application.ServiceUnavailableException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpVerificationEmailSenderTest {

	private JavaMailSender mail;
	private SmtpVerificationEmailSender sender;

	@BeforeEach
	void setUp() {
		mail = mock(JavaMailSender.class);
		when(mail.createMimeMessage()).thenAnswer(call -> new MimeMessage(Session.getInstance(new Properties())));
		sender = new SmtpVerificationEmailSender(mail, "Unnia <nao-responda@unnia.test>", "https://unnia.test/");
	}

	private String raw() throws Exception {
		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mail).send(captor.capture());
		MimeMessage message = captor.getValue();
		message.saveChanges();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		message.writeTo(out);
		// o quoted-printable quebra linhas longas com "=" no fim; desfaz para procurar trechos inteiros
		return out.toString(StandardCharsets.UTF_8).replace("=\r\n", "");
	}

	private MimeMessage message() throws Exception {
		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mail).send(captor.capture());
		return captor.getValue();
	}

	@Test
	void sendsToTheRecipientFromTheConfiguredSenderWithTheSubject() throws Exception {
		sender.send(Email.of("maria@exemplo.com"), "Maria Souza", "abc123");

		MimeMessage message = message();
		assertThat(message.getAllRecipients()[0].toString()).isEqualTo("maria@exemplo.com");
		assertThat(message.getFrom()[0].toString()).contains("nao-responda@unnia.test");
		assertThat(message.getSubject()).isEqualTo(SmtpVerificationEmailSender.SUBJECT);
	}

	@Test
	void theLinkPointsToThePublicSiteWithTheTokenAndNoDoubleSlash() throws Exception {
		sender.send(Email.of("maria@exemplo.com"), "Maria", "abc123");

		assertThat(raw()).contains("https://unnia.test/verificar-email?token=3Dabc123")
				.doesNotContain("unnia.test//verificar");
	}

	@Test
	void theTokenIsUrlEncodedSoOddCharactersCannotBreakTheLink() throws Exception {
		sender.send(Email.of("maria@exemplo.com"), "Maria", "a b&c");

		assertThat(raw()).contains("token=3Da+b%26c");
	}

	@Test
	void greetsByFirstNameAndHasBothHtmlAndTextVersions() throws Exception {
		sender.send(Email.of("maria@exemplo.com"), "Maria Souza", "abc123");

		String raw = raw();
		assertThat(raw).contains("text/plain").contains("text/html").contains("Ol=C3=A1, Maria!");
		assertThat(raw).doesNotContain("Olá, Maria Souza");
	}

	@Test
	void withoutANameItGreetsGenerically() throws Exception {
		sender.send(Email.of("maria@exemplo.com"), "", "abc123");

		assertThat(raw()).contains("Ol=C3=A1!").doesNotContain("{{greeting}}");
	}

	@Test
	void aNameWithMarkupIsEscapedInTheHtml() throws Exception {
		sender.send(Email.of("maria@exemplo.com"), "<script>alert(1)</script>", "abc123");

		// a versão em HTML escapa; a versão em texto simples não precisa
		assertThat(raw()).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
	}

	@Test
	void everyPlaceholderIsFilled() throws Exception {
		sender.send(Email.of("maria@exemplo.com"), "Maria", "abc123");

		assertThat(raw()).doesNotContain("{{").contains("24 horas");
	}

	@Test
	void aMailServerFailureBecomesServiceUnavailable() {
		doThrow(new MailSendException("connection refused")).when(mail).send(any(MimeMessage.class));

		assertThatThrownBy(() -> sender.send(Email.of("maria@exemplo.com"), "Maria", "abc123"))
				.isInstanceOf(ServiceUnavailableException.class);
	}
}
