package sn.yay360.api.controller;

import sn.yay360.api.dto.ConseilDto;
import sn.yay360.api.dto.ConseilRequest;
import sn.yay360.api.dto.MessageResponse;
import sn.yay360.api.service.ConseilService;
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
@RequestMapping("/conseils")
@RequiredArgsConstructor
@Tag(name = "Conseils", description = "Bibliothèque de conseils classés par semaine/trimestre/catégorie")
public class ConseilController {

    private final ConseilService conseilService;

    @GetMapping
    @Operation(summary = "Lister tous les conseils (Médecin/Sage-femme/Admin)")
    public List<ConseilDto> listerTous() {
        return conseilService.listerTous();
    }

    @GetMapping("/semaine/{semaine}")
    @Operation(summary = "Consulter les conseils actifs pour une semaine donnée")
    public List<ConseilDto> obtenirPourSemaine(@PathVariable Integer semaine) {
        return conseilService.obtenirPourSemaine(semaine);
    }

    @GetMapping("/categorie/{categorie}")
    @Operation(summary = "Consulter les conseils actifs d'une catégorie")
    public List<ConseilDto> obtenirParCategorie(@PathVariable String categorie) {
        return conseilService.obtenirParCategorie(categorie);
    }

    @PostMapping
    @Operation(summary = "Créer un conseil (Médecin/Sage-femme/Admin)")
    public ResponseEntity<ConseilDto> creer(Authentication authentication, @Valid @RequestBody ConseilRequest request) {
        ConseilDto dto = conseilService.creer(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un conseil (Médecin/Sage-femme/Admin)")
    public ConseilDto mettreAJour(@PathVariable Long id, @Valid @RequestBody ConseilRequest request) {
        return conseilService.mettreAJour(id, request);
    }

    @PutMapping("/{id}/basculer-activation")
    @Operation(summary = "Activer ou désactiver un conseil (Médecin/Sage-femme/Admin)")
    public ConseilDto basculerActivation(@PathVariable Long id) {
        return conseilService.basculerActivation(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un conseil (Médecin/Sage-femme/Admin)")
    public MessageResponse supprimer(@PathVariable Long id) {
        conseilService.supprimer(id);
        return new MessageResponse("Conseil supprimé avec succès");
    }
}
