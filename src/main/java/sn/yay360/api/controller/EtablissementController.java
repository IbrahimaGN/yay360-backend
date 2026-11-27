package sn.yay360.api.controller;

import sn.yay360.api.dto.*;
import sn.yay360.api.service.EtablissementService;
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
@RequestMapping("/etablissements")
@RequiredArgsConstructor
@Tag(name = "Établissements de santé", description = "Gestion des centres de santé (§5.8)")
public class EtablissementController {

    private final EtablissementService etablissementService;

    @GetMapping
    @Operation(summary = "Lister les établissements actifs")
    public List<EtablissementDto> listerActifs(@RequestParam(required = false) String ville) {
        if (ville != null && !ville.isBlank()) {
            return etablissementService.listerParVille(ville);
        }
        return etablissementService.listerActifs();
    }

    @GetMapping("/mes-etablissements")
    @Operation(summary = "Établissements actifs où j'exerce (médecin, sage-femme)")
    public List<EtablissementDto> mesEtablissements(org.springframework.security.core.Authentication authentication) {
        return etablissementService.mesEtablissements(authentication.getName());
    }

    @GetMapping("/mon-etablissement")
    @Operation(summary = "Établissement que j'administre (Admin d'établissement)")
    public EtablissementDto monEtablissement(Authentication authentication) {
        return etablissementService.monEtablissement(authentication.getName());
    }

    @GetMapping("/tous")
    @Operation(summary = "Lister tous les établissements, actifs et inactifs (Admin)")
    public List<EtablissementDto> listerTous() {
        return etablissementService.listerTous();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un établissement par id")
    public EtablissementDto obtenirParId(@PathVariable Long id) {
        return etablissementService.obtenirParId(id);
    }

    @PostMapping
    @Operation(summary = "Créer un établissement de santé (Admin)")
    public ResponseEntity<EtablissementDto> creer(@Valid @RequestBody EtablissementRequest request) {
        EtablissementDto dto = etablissementService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un établissement (Admin ; Admin d'établissement pour le sien)")
    public EtablissementDto mettreAJour(Authentication authentication, @PathVariable Long id,
                                        @Valid @RequestBody EtablissementRequest request) {
        return etablissementService.mettreAJour(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/professionnels")
    @Operation(summary = "Affecter un soignant à un établissement, par id ou email (Admin ; Admin d'établissement pour le sien)")
    public EtablissementDto affecterProfessionnel(Authentication authentication, @PathVariable Long id,
                                                   @Valid @RequestBody AffecterProfessionnelRequest request) {
        return etablissementService.affecterProfessionnel(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}/professionnels/{professionnelId}")
    @Operation(summary = "Retirer un soignant d'un établissement (Admin ; Admin d'établissement pour le sien)")
    public EtablissementDto retirerProfessionnel(Authentication authentication, @PathVariable Long id,
                                                 @PathVariable Long professionnelId) {
        return etablissementService.retirerProfessionnel(authentication.getName(), id, professionnelId);
    }

    @PutMapping("/{id}/desactiver")
    @Operation(summary = "Désactiver un établissement (Admin)")
    public MessageResponse desactiver(@PathVariable Long id) {
        etablissementService.desactiver(id);
        return new MessageResponse("Établissement désactivé avec succès");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un établissement (Admin)")
    public MessageResponse supprimer(@PathVariable Long id) {
        etablissementService.supprimer(id);
        return new MessageResponse("Établissement supprimé avec succès");
    }
}
