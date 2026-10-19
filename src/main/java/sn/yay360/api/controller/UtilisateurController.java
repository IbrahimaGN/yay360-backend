package sn.yay360.api.controller;

import sn.yay360.api.dto.MessageResponse;
import sn.yay360.api.dto.UpdateProfileRequest;
import sn.yay360.api.dto.UtilisateurDto;
import sn.yay360.api.security.UtilisateurPrincipal;
import sn.yay360.api.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des comptes utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping("/profil")
    @Operation(summary = "Consulter son propre profil")
    public UtilisateurDto obtenirMonProfil(Authentication authentication) {
        return utilisateurService.obtenirProfil(authentication.getName());
    }

    @PutMapping("/profil")
    @Operation(summary = "Mettre à jour son propre profil")
    public UtilisateurDto mettreAJourMonProfil(Authentication authentication,
                                                @Valid @RequestBody UpdateProfileRequest request) {
        return utilisateurService.mettreAJourProfil(authentication.getName(), request);
    }

    @GetMapping
    @Operation(summary = "Lister tous les utilisateurs (Admin)")
    public List<UtilisateurDto> listerTous(@RequestParam(required = false) String role) {
        if (role != null && !role.isBlank()) {
            return utilisateurService.listerParRole(role);
        }
        return utilisateurService.listerTous();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un utilisateur par son id (Admin)")
    public UtilisateurDto obtenirParId(@PathVariable Long id) {
        return utilisateurService.obtenirParId(id);
    }

    @PutMapping("/{id}/desactiver")
    @Operation(summary = "Désactiver un compte utilisateur (Admin)")
    public UtilisateurDto desactiver(@PathVariable Long id, Authentication authentication) {
        Long idAuteur = idDe(authentication);
        return utilisateurService.desactiverCompte(id, idAuteur);
    }

    @PutMapping("/{id}/activer")
    @Operation(summary = "Réactiver un compte utilisateur (Admin)")
    public UtilisateurDto activer(@PathVariable Long id) {
        return utilisateurService.activerCompte(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un compte utilisateur (Admin)")
    public MessageResponse supprimer(@PathVariable Long id, Authentication authentication) {
        Long idAuteur = idDe(authentication);
        utilisateurService.supprimerCompte(id, idAuteur);
        return new MessageResponse("Utilisateur supprimé avec succès");
    }

    private Long idDe(Authentication authentication) {
        return ((UtilisateurPrincipal) authentication.getPrincipal()).getId();
    }
}
