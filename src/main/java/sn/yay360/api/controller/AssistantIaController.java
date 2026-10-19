package sn.yay360.api.controller;

import sn.yay360.api.dto.AssistantChatRequest;
import sn.yay360.api.dto.AssistantChatResponse;
import sn.yay360.api.service.AssistantIaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant-ia")
@RequiredArgsConstructor
@Tag(name = "Assistant IA", description = "Chat et réponses vocales d'information générale en santé maternelle")
public class AssistantIaController {

    private final AssistantIaService assistantIaService;

    @PostMapping("/chat")
    @Operation(summary = "Poser une question par texte ou envoyer sa transcription vocale")
    public AssistantChatResponse discuter(Authentication authentication,
                                          @Valid @RequestBody AssistantChatRequest request) {
        return assistantIaService.repondre(authentication.getName(), request);
    }
}
