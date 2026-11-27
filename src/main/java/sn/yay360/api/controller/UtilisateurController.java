package sn.yay360.api.controller;

import sn.yay360.api.dto.CreationProfessionnelResponse;
import sn.yay360.api.dto.CreerProfessionnelRequest;
import sn.yay360.api.dto.MessageResponse;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/professionnels")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte soignant ou administrateur (Admin, Admin d'établissement)",
            description = "L'administrateur d'établissement ne crée que des médecins et des sages-femmes, rattachés "
                    + "à son établissement. Renvoie un mot de passe temporaire, affiché une seule fois, "
                    + "à changer à la première connexion.")
    public CreationProfessionnelResponse creerProfessionnel(Authentication authentication,
                                                            @Valid @RequestBody CreerProfessionnelRequest request) {
        return utilisateurService.creerProfessionnel(authentication.getName(), request);
    }

    @GetMapping("/mon-etablissement")
    @Operation(summary = "Soignants de mon établissement (Admin d'établissement)")
    public List<UtilisateurDto> soignantsDeMonEtablissement(Authentication authentication) {
        return utilisateurService.listerSoignantsDeMonEtablissement(authentication.getName());
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
    @Operation(summary = "Désactiver un compte (Admin ; Admin d'établissement pour ses soignants)")
    public UtilisateurDto desactiver(@PathVariable Long id, Authentication authentication) {
        return utilisateurService.desactiverCompte(id, authentication.getName());
    }

    @PutMapping("/{id}/activer")
    @Operation(summary = "Réactiver un compte (Admin ; Admin d'établissement pour ses soignants)")
    public UtilisateurDto activer(@PathVariable Long id, Authentication authentication) {
        return utilisateurService.activerCompte(id, authentication.getName());
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
