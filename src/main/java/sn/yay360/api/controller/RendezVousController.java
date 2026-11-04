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
@Tag(name = "Rendez-vous", description = "Prise, planification, report, annulation et suivi des rendez-vous (§4.6)")
public class RendezVousController {

    private final RendezVousService rendezVousService;

    @PostMapping
    @Operation(summary = "Demander un rendez-vous (femme enceinte ou conjoint)")
    public ResponseEntity<RendezVousDto> prendre(Authentication authentication,
                                                 @Valid @RequestBody PrendreRendezVousRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rendezVousService.prendre(authentication.getName(), request));
    }

    @PostMapping("/planifier")
    @Operation(summary = "Planifier un rendez-vous pour une patiente suivie (soignant)")
    public ResponseEntity<RendezVousDto> planifier(Authentication authentication,
                                                   @Valid @RequestBody PlanifierRendezVousRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rendezVousService.planifier(authentication.getName(), request));
    }

    @GetMapping("/mes-rendez-vous")
    @Operation(summary = "Mes rendez-vous")
    public List<RendezVousDto> mesRendezVous(Authentication authentication) {
        return rendezVousService.mesRendezVous(authentication.getName());
    }

    @GetMapping("/mes-rendez-vous/a-venir")
    @Operation(summary = "Mes rendez-vous à venir")
    public List<RendezVousDto> mesRendezVousAVenir(Authentication authentication) {
        return rendezVousService.mesRendezVousAVenir(authentication.getName());
    }

    @GetMapping("/mon-planning")
    @Operation(summary = "Planning du soignant connecté")
    public List<RendezVousDto> monPlanning(Authentication authentication) {
        return rendezVousService.rendezVousDuProfessionnel(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "Rendez-vous par statut : patientes suivies (soignant) ou tous (admin)")
    public List<RendezVousDto> listerParStatut(Authentication authentication, @RequestParam String statut) {
        return rendezVousService.listerParStatut(authentication.getName(), statut);
    }

    @GetMapping("/grossesse/{grossesseId}")
    @Operation(summary = "Rendez-vous d'une grossesse")
    public List<RendezVousDto> listerParGrossesse(Authentication authentication, @PathVariable Long grossesseId) {
        return rendezVousService.listerParGrossesse(authentication.getName(), grossesseId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un rendez-vous")
    public RendezVousDto obtenirParId(Authentication authentication, @PathVariable Long id) {
        return rendezVousService.obtenirParId(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reporter un rendez-vous")
    public RendezVousDto modifier(Authentication authentication, @PathVariable Long id,
                                  @Valid @RequestBody ModifierRendezVousRequest request) {
        return rendezVousService.modifier(authentication.getName(), id, request);
    }

    @PutMapping("/{id}/annuler")
    @Operation(summary = "Annuler un rendez-vous")
    public RendezVousDto annuler(Authentication authentication, @PathVariable Long id,
                                 @RequestBody(required = false) AnnulerRendezVousRequest request) {
        return rendezVousService.annuler(authentication.getName(), id, request);
    }

    @PutMapping("/{id}/statut")
    @Operation(summary = "Changer le statut (CONFIRME, ANNULE, TERMINE, ABSENT) selon les transitions autorisées (soignant)")
    public RendezVousDto changerStatut(Authentication authentication, @PathVariable Long id,
                                       @Valid @RequestBody ChangerStatutRendezVousRequest request) {
        return rendezVousService.changerStatut(authentication.getName(), id, request);
    }
}
