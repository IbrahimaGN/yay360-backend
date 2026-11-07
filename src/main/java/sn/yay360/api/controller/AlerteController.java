package sn.yay360.api.controller;

import sn.yay360.api.dto.AlerteDto;
import sn.yay360.api.dto.DeclarerSymptomeRequest;
import sn.yay360.api.dto.PriseEnChargeRequest;
import sn.yay360.api.service.AlerteService;
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
@RequestMapping("/alertes")
@RequiredArgsConstructor
@Tag(name = "Alertes médicales", description = "Triage des symptômes et prise en charge des urgences (§4.7)")
public class AlerteController {

    private final AlerteService alerteService;

    @PostMapping
    @Operation(summary = "Signaler un symptôme (femme enceinte ou conjoint)")
    public ResponseEntity<AlerteDto> declarer(Authentication authentication,
                                              @Valid @RequestBody DeclarerSymptomeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(alerteService.declarerSymptome(authentication.getName(), request));
    }

    @GetMapping("/mes-alertes")
    @Operation(summary = "Mes alertes")
    public List<AlerteDto> mesAlertes(Authentication authentication) {
        return alerteService.mesAlertes(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "Alertes par statut (NOUVELLE, PRISE_EN_CHARGE, RESOLUE)")
    public List<AlerteDto> listerParStatut(Authentication authentication, @RequestParam String statut) {
        return alerteService.listerParStatut(authentication.getName(), statut);
    }

    @GetMapping("/urgentes")
    @Operation(summary = "Alertes urgentes non résolues")
    public List<AlerteDto> listerUrgentes(Authentication authentication) {
        return alerteService.listerUrgentes(authentication.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une alerte")
    public AlerteDto obtenirParId(Authentication authentication, @PathVariable Long id) {
        return alerteService.obtenirParId(authentication.getName(), id);
    }

    @PutMapping("/{id}/prendre-en-charge")
    @Operation(summary = "Prendre en charge une alerte NOUVELLE (soignant)")
    public AlerteDto prendreEnCharge(Authentication authentication, @PathVariable Long id) {
        return alerteService.prendreEnCharge(authentication.getName(), id);
    }

    @PutMapping("/{id}/resoudre")
    @Operation(summary = "Résoudre une alerte prise en charge, avec observations obligatoires (soignant)")
    public AlerteDto resoudre(Authentication authentication, @PathVariable Long id,
                              @Valid @RequestBody PriseEnChargeRequest request) {
        return alerteService.resoudre(authentication.getName(), id, request);
    }
}
