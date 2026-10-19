package sn.yay360.api.controller;

import sn.yay360.api.dto.*;
import sn.yay360.api.service.RendezVousService;
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
@RequestMapping("/rendez-vous")
@RequiredArgsConstructor
@Tag(name = "Rendez-vous", description = "Prise, modification, annulation et suivi des rendez-vous (§5.7)")
public class RendezVousController {

    private final RendezVousService rendezVousService;

    // --- Dossier familial ---

    @PostMapping
    @Operation(summary = "Prendre un rendez-vous pour le dossier familial")
    public ResponseEntity<RendezVousDto> prendre(Authentication authentication,
                                                  @Valid @RequestBody PrendreRendezVousRequest request) {
        RendezVousDto dto = rendezVousService.prendre(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/mes-rendez-vous")
    @Operation(summary = "Lister les rendez-vous du dossier familial")
    public List<RendezVousDto> mesRendezVous(Authentication authentication) {
        return rendezVousService.mesRendezVous(authentication.getName());
    }

    @GetMapping("/mes-rendez-vous/a-venir")
    @Operation(summary = "Lister les rendez-vous à venir du dossier familial")
    public List<RendezVousDto> mesRendezVousAVenir(Authentication authentication) {
        return rendezVousService.mesRendezVousAVenir(authentication.getName());
    }

    // --- Professionnels de santé / Admin ---

    @GetMapping("/mon-planning")
    @Operation(summary = "Lister mes rendez-vous en tant que professionnel (Médecin/Sage-femme)")
    public List<RendezVousDto> monPlanning(Authentication authentication) {
        return rendezVousService.rendezVousDuProfessionnel(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "Lister les rendez-vous par statut (Médecin/Sage-femme/Admin)")
    public List<RendezVousDto> listerParStatut(@RequestParam String statut) {
        return rendezVousService.listerParStatut(statut);
    }

    @GetMapping("/grossesse/{grossesseId}")
    @Operation(summary = "Lister les rendez-vous d'une grossesse (Médecin/Sage-femme/Admin)")
    public List<RendezVousDto> listerParGrossesse(@PathVariable Long grossesseId) {
        return rendezVousService.listerParGrossesse(grossesseId);
    }

    @PutMapping("/{id}/statut")
    @Operation(summary = "Changer le statut d'un rendez-vous : confirmer/terminer/absent (Médecin/Sage-femme/Admin)")
    public RendezVousDto changerStatut(@PathVariable Long id, @Valid @RequestBody ChangerStatutRendezVousRequest request) {
        return rendezVousService.changerStatut(id, request);
    }

    // --- Communs (avec vérification d'accès interne) ---

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un rendez-vous par id")
    public RendezVousDto obtenirParId(Authentication authentication, @PathVariable Long id) {
        return rendezVousService.obtenirParId(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un rendez-vous (déplacer la date)")
    public RendezVousDto modifier(Authentication authentication, @PathVariable Long id,
                                   @Valid @RequestBody ModifierRendezVousRequest request) {
        return rendezVousService.modifier(authentication.getName(), id, request);
    }

    @PutMapping("/{id}/annuler")
    @Operation(summary = "Annuler un rendez-vous")
    public RendezVousDto annuler(Authentication authentication, @PathVariable Long id,
                                  @RequestBody(required = false) AnnulerRendezVousRequest request) {
        AnnulerRendezVousRequest req = request != null ? request : new AnnulerRendezVousRequest();
        return rendezVousService.annuler(authentication.getName(), id, req);
    }
}
