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
@Tag(name = "Consultations", description = "Enregistrement des consultations médicales (§5.6)")
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    @Operation(summary = "Enregistrer une consultation (Médecin/Sage-femme)")
    public ResponseEntity<ConsultationDto> enregistrer(Authentication authentication,
                                                        @Valid @RequestBody EnregistrerConsultationRequest request) {
        ConsultationDto dto = consultationService.enregistrer(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/mes-consultations")
    @Operation(summary = "Lister les consultations du dossier familial")
    public List<ConsultationDto> mesConsultations(Authentication authentication) {
        return consultationService.mesConsultations(authentication.getName());
    }

    @GetMapping("/mon-activite")
    @Operation(summary = "Lister les consultations que j'ai réalisées (Médecin/Sage-femme)")
    public List<ConsultationDto> monActivite(Authentication authentication) {
        return consultationService.mesConsultationsProfessionnel(authentication.getName());
    }

    @GetMapping("/grossesse/{grossesseId}")
    @Operation(summary = "Lister les consultations d'une grossesse (Médecin/Sage-femme/Admin)")
    public List<ConsultationDto> parGrossesse(@PathVariable Long grossesseId) {
        return consultationService.parGrossesse(grossesseId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une consultation par id")
    public ConsultationDto obtenirParId(Authentication authentication, @PathVariable Long id) {
        return consultationService.obtenirParId(authentication.getName(), id);
    }
}
