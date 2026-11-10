package com.Unnia.social.infrastructure.web;

import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.social.application.usecase.UploadMediaUseCase;
import com.Unnia.social.domain.model.MediaType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Envio de imagens para usar em postagens. A resposta traz a URL a incluir em {@code media} ao publicar. */
@Tag(name = "Mídia")
@RestController
class MediaController {

	record UploadedMedia(String url, MediaType type) {
	}

	private final UploadMediaUseCase upload;

	MediaController(UploadMediaUseCase upload) {
		this.upload = upload;
	}

	@PostMapping(path = "/api/media", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	UploadedMedia upload(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam("file") MultipartFile file)
			throws IOException {
		var result = upload.execute(new UploadMediaUseCase.Command(user.accountId(), file.getBytes()));
		return new UploadedMedia(result.url(), result.type());
	}
}
