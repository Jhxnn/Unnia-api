package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.ServiceUnavailableException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.port.MediaStorage;
import com.Unnia.social.domain.model.MediaType;

/**
 * Recebe uma imagem para usar em postagens. O tipo é descoberto pelo <em>conteúdo</em> do arquivo (os primeiros
 * bytes), nunca pelo que o cliente diz: um arquivo renomeado para .png não passa.
 */
public class UploadMediaUseCase {

	public static final int MAX_BYTES = 5 * 1024 * 1024;

	public record Command(AccountId ownerId, byte[] content) {
	}

	public record Result(String url, MediaType type) {
	}

	private enum Format {
		JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), GIF("gif", "image/gif"), WEBP("webp", "image/webp");

		final String extension;
		final String contentType;

		Format(String extension, String contentType) {
			this.extension = extension;
			this.contentType = contentType;
		}
	}

	private final MediaStorage storage;

	public UploadMediaUseCase(MediaStorage storage) {
		this.storage = storage;
	}

	public Result execute(Command command) {
		if (!storage.isEnabled()) {
			throw new ServiceUnavailableException("O envio de imagens ainda não está configurado.");
		}
		byte[] content = command.content();
		if (content == null || content.length == 0) {
			throw new DomainException("Escolha uma imagem para enviar");
		}
		if (content.length > MAX_BYTES) {
			throw new DomainException("A imagem passa do limite de 5 MB");
		}
		Format format = detect(content);
		String url = storage.store(command.ownerId(), format.extension, format.contentType, content);
		return new Result(url, MediaType.IMAGE);
	}

	private static Format detect(byte[] b) {
		if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
			return Format.JPEG;
		}
		if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' && b[4] == 0x0D
				&& b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
			return Format.PNG;
		}
		if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8' && (b[4] == '7' || b[4] == '9')
				&& b[5] == 'a') {
			return Format.GIF;
		}
		if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E'
				&& b[10] == 'B' && b[11] == 'P') {
			return Format.WEBP;
		}
		throw new DomainException("Formato não aceito. Use JPEG, PNG, WebP ou GIF");
	}
}
