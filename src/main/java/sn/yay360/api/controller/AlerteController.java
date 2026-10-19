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
@Tag(name = "Alertes médicales", description = "Détection et gestion des situations nécessitant une vigilance particulière (§5.11)")
public class AlerteController {

    private final AlerteService alerteService;

    @PostMapping
    @Operation(summary = "Déclarer un symptôme préoccupant (famille)")
    public ResponseEntity<AlerteDto> declarer(Authentication authentication,
                                               @Valid @RequestBody DeclarerSymptomeRequest request) {
        AlerteDto dto = alerteService.declarerSymptome(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/mes-alertes")
    @Operation(summary = "Consulter les alertes du dossier familial")
    public List<AlerteDto> mesAlertes(Authentication authentication) {
        return alerteService.mesAlertes(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "Lister les alertes par statut (Médecin/Sage-femme/Admin)")
    public List<AlerteDto> listerParStatut(@RequestParam String statut) {
        return alerteService.listerParStatut(statut);
    }

    @GetMapping("/urgentes")
    @Operation(summary = "Lister les alertes urgentes (Médecin/Sage-femme/Admin)")
    public List<AlerteDto> listerUrgentes() {
        return alerteService.listerUrgentes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une alerte par id")
    public AlerteDto obtenirParId(Authentication authentication, @PathVariable Long id) {
        return alerteService.obtenirParId(authentication.getName(), id);
    }

    @PutMapping("/{id}/prendre-en-charge")
    @Operation(summary = "Prendre en charge une alerte (Médecin/Sage-femme)")
    public AlerteDto prendreEnCharge(Authentication authentication, @PathVariable Long id) {
        return alerteService.prendreEnCharge(authentication.getName(), id);
    }

    @PutMapping("/{id}/resoudre")
    @Operation(summary = "Résoudre/clôturer une alerte (Médecin/Sage-femme)")
    public AlerteDto resoudre(@PathVariable Long id, @RequestBody(required = false) PriseEnChargeRequest request) {
        PriseEnChargeRequest req = request != null ? request : new PriseEnChargeRequest();
        return alerteService.resoudre(id, req);
    }
}
