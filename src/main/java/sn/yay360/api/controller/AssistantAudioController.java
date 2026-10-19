package sn.yay360.api.controller;

import sn.yay360.api.dto.AssistantTranscriptionResponse;
import sn.yay360.api.service.AssistantAudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/assistant-ia/audio")
@Tag(name = "Assistant vocal", description = "Transcription vocale et synthèse audio")
public class AssistantAudioController {

    private final AssistantAudioService assistantAudioService;

    public AssistantAudioController(AssistantAudioService assistantAudioService) {
        this.assistantAudioService = assistantAudioService;
    }

    @PostMapping(value = "/transcription", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Transcrire un fichier audio après consentement")
    public AssistantTranscriptionResponse transcrire(
            Authentication authentication,
            @RequestPart("audio") MultipartFile audio,
            @RequestParam("consentementTransmission") boolean consentementTransmission) {
        return assistantAudioService.transcrire(
                authentication.getName(), audio, consentementTransmission);
    }

    @PostMapping(value = "/synthese-vocale", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Générer un fichier audio MP3 à partir d'une réponse après consentement")
    public ResponseEntity<byte[]> synthetiser(
            Authentication authentication,
            @Valid @RequestBody AssistantSpeechRequest request) {
        byte[] audio = assistantAudioService.synthetiser(
                authentication.getName(), request.text(), request.consentementTransmission());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header("Content-Disposition", "inline; filename=\"yay360-assistant.mp3\"")
                .body(audio);
    }

    public record AssistantSpeechRequest(
            @NotBlank @Size(max = 4000) String text,
            @NotNull Boolean consentementTransmission) {
    }
}
