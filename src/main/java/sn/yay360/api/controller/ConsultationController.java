package sn.yay360.api.controller;

import sn.yay360.api.dto.ConsultationDto;
import sn.yay360.api.dto.EnregistrerConsultationRequest;
import sn.yay360.api.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/consultations")
@RequiredArgsConstructor
@Tag(name = "Consultations", description = "Consultations prénatales (CPN) : constantes, diagnostic, prescriptions (§4.5)")
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    @Operation(summary = "Enregistrer une consultation (soignant qui suit la patiente)")
    public ResponseEntity<ConsultationDto> enregistrer(Authentication authentication,
                                                       @Valid @RequestBody EnregistrerConsultationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultationService.enregistrer(authentication.getName(), request));
    }

    @GetMapping("/mes-consultations")
    @Operation(summary = "Mes consultations (femme enceinte ou conjoint)")
    public List<ConsultationDto> mesConsultations(Authentication authentication) {
        return consultationService.mesConsultations(authentication.getName());
    }

    @GetMapping("/mon-activite")
    @Operation(summary = "Consultations réalisées par le soignant connecté")
    public List<ConsultationDto> monActivite(Authentication authentication) {
        return consultationService.mesConsultationsProfessionnel(authentication.getName());
    }

    @GetMapping("/grossesse/{grossesseId}")
    @Operation(summary = "Consultations d'une grossesse (soignant qui la suit)")
    public List<ConsultationDto> parGrossesse(Authentication authentication, @PathVariable Long grossesseId) {
        return consultationService.parGrossesse(authentication.getName(), grossesseId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une consultation")
    public ConsultationDto obtenirParId(Authentication authentication, @PathVariable Long id) {
        return consultationService.obtenirParId(authentication.getName(), id);
    }
}
