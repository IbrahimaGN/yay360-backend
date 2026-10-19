package sn.yay360.api.controller;

import sn.yay360.api.dto.EtapeGrossesseDto;
import sn.yay360.api.dto.EtapeGrossesseRequest;
import sn.yay360.api.dto.MessageResponse;
import sn.yay360.api.service.EtapeGrossesseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/etapes-grossesse")
@RequiredArgsConstructor
@Tag(name = "Étapes de grossesse", description = "Contenu de référence administrable par semaine (1 à 40)")
public class EtapeGrossesseController {

    private final EtapeGrossesseService etapeGrossesseService;

    @GetMapping
    @Operation(summary = "Lister toutes les étapes de grossesse (1 à 40)")
    public List<EtapeGrossesseDto> listerToutes() {
        return etapeGrossesseService.listerToutes();
    }

    @GetMapping("/{semaine}")
    @Operation(summary = "Consulter le contenu de référence d'une semaine")
    public EtapeGrossesseDto obtenirParSemaine(@PathVariable Integer semaine) {
        return etapeGrossesseService.obtenirParSemaine(semaine);
    }

    @PostMapping
    @Operation(summary = "Créer le contenu d'une semaine (Médecin/Sage-femme/Admin)")
    public ResponseEntity<EtapeGrossesseDto> creer(@Valid @RequestBody EtapeGrossesseRequest request) {
        EtapeGrossesseDto dto = etapeGrossesseService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour le contenu d'une semaine (Médecin/Sage-femme/Admin)")
    public EtapeGrossesseDto mettreAJour(@PathVariable Long id, @Valid @RequestBody EtapeGrossesseRequest request) {
        return etapeGrossesseService.mettreAJour(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer le contenu d'une semaine (Médecin/Sage-femme/Admin)")
    public MessageResponse supprimer(@PathVariable Long id) {
        etapeGrossesseService.supprimer(id);
        return new MessageResponse("Étape de grossesse supprimée avec succès");
    }
}
