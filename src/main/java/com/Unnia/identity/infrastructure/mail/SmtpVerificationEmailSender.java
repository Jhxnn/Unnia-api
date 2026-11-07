package com.Unnia.identity.infrastructure.mail;

import com.Unnia.identity.application.EmailVerificationIssuer;
import com.Unnia.identity.application.port.VerificationEmailSender;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.shared.application.ServiceUnavailableException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Envia o e-mail de confirmação por SMTP, em HTML com versão em texto, a partir de {@code mail/verify-email.html}. */
@Component
class SmtpVerificationEmailSender implements VerificationEmailSender {

	private static final Logger log = LoggerFactory.getLogger(SmtpVerificationEmailSender.class);
	static final String SUBJECT = "Confirme seu e-mail no Unnia";

	private final JavaMailSender mail;
	private final String from;
	private final String publicUrl;
	private final String template;

	SmtpVerificationEmailSender(JavaMailSender mail, @Value("${unnia.mail.from}") String from,
			@Value("${unnia.public-url}") String publicUrl) {
		this.mail = mail;
		this.from = from;
		this.publicUrl = publicUrl.replaceAll("/+$", "");
		try {
			this.template = new String(new ClassPathResource("mail/verify-email.html").getInputStream().readAllBytes(),
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Modelo do e-mail de confirmação não encontrado", e);
		}
	}

	@Override
	public void send(Email to, String recipientName, String token) {
		String link = publicUrl + "/verificar-email?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
		String greeting = recipientName == null || recipientName.isBlank() ? "Olá!"
				: "Olá, " + HtmlUtils.htmlEscape(firstName(recipientName)) + "!";
		long hours = EmailVerificationIssuer.TTL.toHours();

		String html = template.replace("{{greeting}}", greeting).replace("{{link}}", link)
				.replace("{{hours}}", String.valueOf(hours));
		String text = plainText(recipientName, link, hours);

		try {
			MimeMessage message = mail.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(new InternetAddress(from, true));
			helper.setTo(to.value());
			helper.setSubject(SUBJECT);
			helper.setText(text, html);
			mail.send(message);
		} catch (MailException | MessagingException e) {
			log.error("Falha ao enviar o e-mail de confirmação", e);
			throw new ServiceUnavailableException("Não foi possível enviar o e-mail agora. Tente novamente em instantes.",
					e);
		}
	}

	private static String firstName(String name) {
		String trimmed = name.trim();
		int space = trimmed.indexOf(' ');
		return space > 0 ? trimmed.substring(0, space) : trimmed;
	}

	private static String plainText(String recipientName, String link, long hours) {
		String hello = recipientName == null || recipientName.isBlank() ? "Olá!" : "Olá, " + firstName(recipientName) + "!";
		return hello + "\n\n"
				+ "Falta só um passo para entrar no Unnia: confirme que este e-mail é seu.\n\n"
				+ link + "\n\n"
				+ "O link vale por " + hours + " horas e só pode ser usado uma vez.\n"
				+ "Não criou uma conta no Unnia? Pode ignorar esta mensagem.\n\n"
				+ "Unnia · Conecta ONGs e voluntários em eventos solidários.\n";
	}
}
