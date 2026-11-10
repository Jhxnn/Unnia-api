package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.ServiceUnavailableException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.fakes.FakeMediaStorage;
import com.Unnia.social.application.usecase.UploadMediaUseCase.Command;
import com.Unnia.social.domain.model.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadMediaUseCaseTest {

	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10 };
	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0 };
	private static final byte[] GIF = "GIF89a....".getBytes();
	private static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes();

	private FakeMediaStorage storage;
	private UploadMediaUseCase useCase;
	private AccountId owner;

	@BeforeEach
	void setUp() {
		storage = new FakeMediaStorage();
		useCase = new UploadMediaUseCase(storage);
		owner = AccountId.generate();
	}

	@Test
	void storesAJpegAndReturnsItsUrl() {
		UploadMediaUseCase.Result result = useCase.execute(new Command(owner, JPEG));

		assertThat(result.type()).isEqualTo(MediaType.IMAGE);
		assertThat(result.url()).startsWith(FakeMediaStorage.PREFIX).endsWith(".jpg");
		assertThat(storage.stored()).singleElement().satisfies(stored -> {
			assertThat(stored.owner()).isEqualTo(owner);
			assertThat(stored.contentType()).isEqualTo("image/jpeg");
			assertThat(stored.size()).isEqualTo(JPEG.length);
		});
	}

	@Test
	void recognizesPngGifAndWebpByTheirContent() {
		assertThat(useCase.execute(new Command(owner, PNG)).url()).endsWith(".png");
		assertThat(useCase.execute(new Command(owner, GIF)).url()).endsWith(".gif");
		assertThat(useCase.execute(new Command(owner, WEBP)).url()).endsWith(".webp");
	}

	@Test
	void theFormatComesFromTheBytesNotFromAnythingTheClientSays() {
		byte[] script = "<script>alert(1)</script>".getBytes();

		assertThatThrownBy(() -> useCase.execute(new Command(owner, script))).isInstanceOf(DomainException.class)
				.hasMessage("Formato não aceito. Use JPEG, PNG, WebP ou GIF");
		assertThat(storage.stored()).isEmpty();
	}

	@Test
	void anSvgOrPdfIsNotAnAcceptedImage() {
		assertThatThrownBy(() -> useCase.execute(new Command(owner, "<svg xmlns=\"x\"/>".getBytes())))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(new Command(owner, "%PDF-1.7".getBytes())))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void aFileJustBelowAMagicNumberLengthIsRejectedWithoutCrashing() {
		assertThatThrownBy(() -> useCase.execute(new Command(owner, new byte[] { (byte) 0xFF, (byte) 0xD8 })))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void emptyOrMissingContentIsRejected() {
		assertThatThrownBy(() -> useCase.execute(new Command(owner, new byte[0]))).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(new Command(owner, null))).isInstanceOf(DomainException.class);
	}

	@Test
	void theFiveMegabyteLimitIsEnforcedExactly() {
		byte[] atLimit = Arrays.copyOf(JPEG, UploadMediaUseCase.MAX_BYTES);
		byte[] over = Arrays.copyOf(JPEG, UploadMediaUseCase.MAX_BYTES + 1);

		assertThat(useCase.execute(new Command(owner, atLimit)).url()).isNotBlank();
		assertThatThrownBy(() -> useCase.execute(new Command(owner, over))).isInstanceOf(DomainException.class)
				.hasMessage("A imagem passa do limite de 5 MB");
	}

	@Test
	void withoutConfiguredStorageUploadsAreUnavailableNotCrashes() {
		storage.disable();

		assertThatThrownBy(() -> useCase.execute(new Command(owner, JPEG))).isInstanceOf(ServiceUnavailableException.class);
	}
}
