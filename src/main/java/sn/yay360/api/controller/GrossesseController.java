package sn.yay360.api.controller;

import sn.yay360.api.dto.*;
import sn.yay360.api.service.CalendrierService;
import sn.yay360.api.service.GrossesseService;
import sn.yay360.api.service.IntelligenceCliniqueService;
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
    private final IntelligenceCliniqueService intelligenceCliniqueService;
    private final sn.yay360.api.service.ExportDossierService exportDossierService;

    /** Réponse PDF en téléchargement : données de santé, jamais mises en cache. */
    private static org.springframework.http.ResponseEntity<byte[]> pdf(byte[] contenu, String nom) {
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store")
                .body(contenu);
    }

    private static String nomFichier(String suffixe) {
        return "yay360-dossier-grossesse-" + suffixe + "-" + java.time.LocalDate.now() + ".pdf";
    }

    // ==================== PATIENTE / CONJOINT ====================

    @PostMapping
    @Operation(summary = "Déclarer sa grossesse (femme enceinte)")
    public ResponseEntity<GrossesseDto> declarer(Authentication authentication,
                                                 @Valid @RequestBody DeclarerGrossesseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grossesseService.declarer(authentication.getName(), request));
    }

    @GetMapping("/ma-grossesse")
    @Operation(summary = "Grossesse en cours (femme enceinte ou conjoint)")
    public GrossesseDto obtenirMaGrossesse(Authentication authentication) {
        return grossesseService.obtenirGrossesseActive(authentication.getName());
    }

    @PutMapping("/ma-grossesse/etablissement")
    @Operation(summary = "Choisir ou changer son établissement de suivi (femme enceinte)")
    public GrossesseDto choisirEtablissement(Authentication authentication,
                                             @Valid @RequestBody ChoisirEtablissementRequest request) {
        return grossesseService.choisirEtablissement(authentication.getName(), request);
    }

    @GetMapping("/ma-grossesse/export-pdf")
    @Operation(summary = "Export PDF du dossier de la grossesse en cours (patiente)")
    public org.springframework.http.ResponseEntity<byte[]> exporterMaGrossesse(Authentication authentication) {
        return pdf(exportDossierService.exporterMaGrossesse(authentication.getName()), nomFichier("en-cours"));
    }

    @GetMapping("/mon-historique")
    @Operation(summary = "Historique des grossesses")
    public List<GrossesseDto> obtenirMonHistorique(Authentication authentication) {
        return grossesseService.obtenirHistorique(authentication.getName());
    }

    @GetMapping("/mon-calendrier")
    @Operation(summary = "Calendrier des 40 semaines de la grossesse en cours")
    public CalendrierGrossesseDto obtenirMonCalendrier(Authentication authentication) {
        return calendrierService.obtenirMonCalendrier(authentication.getName());
    }

    @GetMapping("/mon-suivi")
    @Operation(summary = "Suivi personnalisé de la semaine en cours")
    public SuiviHebdomadaireDto obtenirMonSuivi(Authentication authentication) {
        return suiviService.obtenirMonSuivi(authentication.getName());
    }

    @GetMapping("/mon-plan-cpn")
    @Operation(summary = "Plan de mes consultations prénatales (8 contacts OMS) avec les contacts en retard")
    public PlanCpnDto monPlanCpn(Authentication authentication) {
        return intelligenceCliniqueService.monPlanCpn(authentication.getName());
    }

    // ==================== PROFESSIONNELS / ADMIN ====================

    @GetMapping
    @Operation(summary = "Grossesses en cours : celles que je suis (soignant) ou toutes (admin)")
    public List<GrossesseDto> listerActives(Authentication authentication) {
        return grossesseService.listerActives(authentication.getName());
    }

    @GetMapping("/a-risque")
    @Operation(summary = "Grossesses à risque en cours")
    public List<GrossesseDto> listerARisque(Authentication authentication) {
        return grossesseService.listerARisque(authentication.getName());
    }

    @GetMapping("/recherche")
    @Operation(summary = "Rechercher une patiente par téléphone ou email exact (soignant)",
            description = "Pour prendre en charge une patiente qui se présente ; aucune donnée clinique n'est renvoyée.")
    public List<PatienteRechercheDto> rechercher(Authentication authentication,
                                                 @RequestParam(required = false) String telephone,
                                                 @RequestParam(required = false) String email) {
        return grossesseService.rechercherPatiente(authentication.getName(), telephone, email);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'une grossesse (soignant qui la suit, ou admin)")
    public GrossesseDto obtenirParId(Authentication authentication, @PathVariable Long id) {
        return grossesseService.obtenirParId(authentication.getName(), id);
    }

    @GetMapping("/{id}/calendrier")
    @Operation(summary = "Calendrier d'une grossesse")
    public CalendrierGrossesseDto obtenirCalendrier(Authentication authentication, @PathVariable Long id) {
        return calendrierService.obtenirCalendrierParId(authentication.getName(), id);
    }

    @GetMapping("/{id}/suivi")
    @Operation(summary = "Suivi hebdomadaire d'une grossesse")
    public SuiviHebdomadaireDto obtenirSuivi(Authentication authentication, @PathVariable Long id) {
        return suiviService.obtenirSuiviParId(authentication.getName(), id);
    }

    @GetMapping("/{id}/export-pdf")
    @Operation(summary = "Export PDF du dossier d'une grossesse (soignant qui suit la patiente)")
    public org.springframework.http.ResponseEntity<byte[]> exporterParId(Authentication authentication, @PathVariable Long id) {
        return pdf(exportDossierService.exporterParId(authentication.getName(), id), nomFichier(String.valueOf(id)));
    }

    @GetMapping("/{id}/plan-cpn")
    @Operation(summary = "Plan des consultations prénatales d'une grossesse (8 contacts OMS)")
    public PlanCpnDto planCpn(Authentication authentication, @PathVariable Long id) {
        return intelligenceCliniqueService.planCpnParId(authentication.getName(), id);
    }

    @GetMapping("/{id}/evaluation-risque")
    @Operation(summary = "Facteurs de risque détectés automatiquement (aide à la décision du médecin)")
    public EvaluationRisqueDto evaluationRisque(Authentication authentication, @PathVariable Long id) {
        return intelligenceCliniqueService.evaluerRisque(authentication.getName(), id);
    }

    @PutMapping("/{id}/rattacher")
    @Operation(summary = "Rattacher une grossesse sans suivi à son établissement (soignant membre)",
            description = "Le téléphone de la patiente est exigé comme preuve de présence.")
    public GrossesseDto rattacher(Authentication authentication, @PathVariable Long id,
                                  @Valid @RequestBody RattacherGrossesseRequest request) {
        return grossesseService.rattacher(authentication.getName(), id, request);
    }

    @PutMapping("/{id}/risque")
    @Operation(summary = "Déclarer une grossesse à risque (médecin qui la suit)")
    public GrossesseDto declarerARisque(Authentication authentication, @PathVariable Long id,
                                        @Valid @RequestBody DeclarerRisqueRequest request) {
        return grossesseService.declarerARisque(authentication.getName(), id, request);
    }

    @PutMapping("/{id}/risque/retirer")
    @Operation(summary = "Retirer le statut à risque (médecin qui la suit)")
    public GrossesseDto retirerRisque(Authentication authentication, @PathVariable Long id) {
        return grossesseService.retirerRisque(authentication.getName(), id);
    }

    @PutMapping("/{id}/terminer")
    @Operation(summary = "Clôturer une grossesse : accouchement ou interruption (médecin qui la suit)")
    public GrossesseDto terminer(Authentication authentication, @PathVariable Long id,
                                 @Valid @RequestBody TerminerGrossesseRequest request) {
        return grossesseService.terminer(authentication.getName(), id, request);
    }
}
