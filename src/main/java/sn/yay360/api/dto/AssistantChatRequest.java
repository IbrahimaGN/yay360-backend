package sn.yay360.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AssistantChatRequest {

    @NotBlank
    @Size(max = 2000, message = "La question doit contenir au maximum 2000 caractères")
    @Schema(description = "Question saisie au clavier ou transcription textuelle de la voix")
    private String message;

    @NotBlank
    @Pattern(regexp = "TEXTE|VOIX", message = "Le canal doit être TEXTE ou VOIX")
    @Schema(description = "TEXTE ou VOIX ; en mode VOIX, le message est la transcription vocale")
    private String canal;

    @NotNull
    @Size(max = 12, message = "L'historique ne peut pas dépasser 12 messages")
    @Valid
    @Schema(description = "Historique non conservé par le serveur, limité à 12 messages user/assistant")
    private List<AssistantTourRequest> historique = new ArrayList<>();

    @NotNull
    @Schema(description = "Doit être true après information de l'utilisateur sur l'envoi au fournisseur IA externe")
    private Boolean consentementTransmission;
}
