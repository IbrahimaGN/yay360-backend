package sn.yay360.api.helper;

import sn.yay360.api.dto.UpdateProfileRequest;
import sn.yay360.api.dto.UtilisateurDto;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.UtilisateurMapper;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Contient toute la logique métier liée à la gestion des utilisateurs :
 * consultation de profil, mise à jour, activation/désactivation, suppression.
 */
@Component
@RequiredArgsConstructor
public class UtilisateurHelper {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;

    public UtilisateurDto obtenirProfil(String email) {
        Utilisateur utilisateur = trouverParEmail(email);
        return utilisateurMapper.versDto(utilisateur);
    }

    public UtilisateurDto mettreAJourProfil(String email, UpdateProfileRequest request) {
        Utilisateur utilisateur = trouverParEmail(email);
        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setTelephone(request.getTelephone());
        Utilisateur mis_a_jour = utilisateurRepository.save(utilisateur);
        return utilisateurMapper.versDto(mis_a_jour);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public List<UtilisateurDto> listerTous() {
        return utilisateurRepository.findAll().stream()
                .map(utilisateurMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public List<UtilisateurDto> listerParRole(String role) {
        return utilisateurRepository.findByRole(role).stream()
                .map(utilisateurMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public UtilisateurDto obtenirParId(Long id) {
        Utilisateur utilisateur = trouverParId(id);
        return utilisateurMapper.versDto(utilisateur);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public UtilisateurDto desactiverCompte(Long id, Long idAuteur) {
        if (id.equals(idAuteur)) {
            throw new OperationNonAutoriseeException("Vous ne pouvez pas désactiver votre propre compte");
        }
        Utilisateur utilisateur = trouverParId(id);
        utilisateur.setActif(false);
        return utilisateurMapper.versDto(utilisateurRepository.save(utilisateur));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public UtilisateurDto activerCompte(Long id) {
        Utilisateur utilisateur = trouverParId(id);
        utilisateur.setActif(true);
        return utilisateurMapper.versDto(utilisateurRepository.save(utilisateur));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public void supprimerCompte(Long id, Long idAuteur) {
        if (id.equals(idAuteur)) {
            throw new OperationNonAutoriseeException("Vous ne pouvez pas supprimer votre propre compte");
        }
        Utilisateur utilisateur = trouverParId(id);
        utilisateurRepository.delete(utilisateur);
    }

    private Utilisateur trouverParEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
    }

    private Utilisateur trouverParId(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id));
    }
}
