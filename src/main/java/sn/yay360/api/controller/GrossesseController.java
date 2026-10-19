package sn.yay360.api.controller;

import sn.yay360.api.dto.*;
import sn.yay360.api.service.CalendrierService;
import sn.yay360.api.service.GrossesseService;
import sn.yay360.api.service.SuiviService;
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
@RequestMapping("/grossesses")
@RequiredArgsConstructor
@Tag(name = "Grossesses", description = "Suivi prénatal et gestion du dossier maternel partagé")
public class GrossesseController {

    private final GrossesseService grossesseService;
    private final CalendrierService calendrierService;
    private final SuiviService suiviService;

    // --- Espace Femme enceinte ---

    @PostMapping
    @Operation(summary = "Déclarer une grossesse (Femme enceinte ou conjoint associé)")
    public ResponseEntity<GrossesseDto> declarer(Authentication authentication,
                                                  @Valid @RequestBody DeclarerGrossesseRequest request) {
        GrossesseDto dto = grossesseService.declarer(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/ma-grossesse")
    @Operation(summary = "Consulter la grossesse en cours du dossier familial")
    public GrossesseDto obtenirMaGrossesse(Authentication authentication) {
        return grossesseService.obtenirGrossesseActive(authentication.getName());
    }

    @GetMapping("/mon-historique")
    @Operation(summary = "Consulter l'historique des grossesses du dossier familial")
    public List<GrossesseDto> obtenirMonHistorique(Authentication authentication) {
        return grossesseService.obtenirHistorique(authentication.getName());
    }

    @GetMapping("/mon-calendrier")
    @Operation(summary = "Calendrier intelligent du dossier familial")
    public CalendrierGrossesseDto obtenirMonCalendrier(Authentication authentication) {
        return calendrierService.obtenirMonCalendrier(authentication.getName());
    }

    @GetMapping("/mon-suivi")
    @Operation(summary = "Accompagnement prénatal personnalisé de la semaine en cours")
    public SuiviHebdomadaireDto obtenirMonSuivi(Authentication authentication) {
        return suiviService.obtenirMonSuivi(authentication.getName());
    }

    // --- Espace Professionnels de santé / Admin ---

    @GetMapping
    @Operation(summary = "Lister les grossesses actives (Médecin/Sage-femme/Admin)")
    public List<GrossesseDto> listerActives() {
        return grossesseService.listerActives();
    }

    @GetMapping("/a-risque")
    @Operation(summary = "Lister les grossesses à risque (Médecin/Sage-femme/Admin)")
    public List<GrossesseDto> listerARisque() {
        return grossesseService.listerARisque();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une grossesse par id (Médecin/Sage-femme/Admin)")
    public GrossesseDto obtenirParId(@PathVariable Long id) {
        return grossesseService.obtenirParId(id);
    }

    @GetMapping("/{id}/calendrier")
    @Operation(summary = "Calendrier intelligent d'une grossesse (Médecin/Sage-femme/Admin)")
    public CalendrierGrossesseDto obtenirCalendrier(@PathVariable Long id) {
        return calendrierService.obtenirCalendrierParId(id);
    }

    @GetMapping("/{id}/suivi")
    @Operation(summary = "Suivi hebdomadaire d'une grossesse (Médecin/Sage-femme/Admin)")
    public SuiviHebdomadaireDto obtenirSuivi(@PathVariable Long id) {
        return suiviService.obtenirSuiviParId(id);
    }

    @PutMapping("/{id}/risque")
    @Operation(summary = "Déclarer une grossesse à risque (Médecin)")
    public GrossesseDto declarerARisque(@PathVariable Long id, @Valid @RequestBody DeclarerRisqueRequest request) {
        return grossesseService.declarerARisque(id, request);
    }

    @PutMapping("/{id}/risque/retirer")
    @Operation(summary = "Retirer le statut à risque d'une grossesse (Médecin)")
    public GrossesseDto retirerRisque(@PathVariable Long id) {
        return grossesseService.retirerRisque(id);
    }

    @PutMapping("/{id}/terminer")
    @Operation(summary = "Clôturer le suivi d'une grossesse (Médecin/Sage-femme)")
    public GrossesseDto terminer(@PathVariable Long id, @Valid @RequestBody TerminerGrossesseRequest request) {
        return grossesseService.terminer(id, request);
    }
}
