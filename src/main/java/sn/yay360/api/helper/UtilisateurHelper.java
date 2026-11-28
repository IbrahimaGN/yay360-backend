package sn.yay360.api.helper;

import sn.yay360.api.dto.CreationProfessionnelResponse;
import sn.yay360.api.dto.CreerProfessionnelRequest;
import sn.yay360.api.dto.UpdateProfileRequest;
import sn.yay360.api.exception.EmailDejaUtiliseException;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.security.JetonSecurise;
import sn.yay360.api.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional
public class UtilisateurHelper {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final java.util.Set<String> ROLES_CONNUS = java.util.Set.of(
            RoleUtilisateur.ADMIN, RoleUtilisateur.MEDECIN, RoleUtilisateur.SAGE_FEMME,
            RoleUtilisateur.FEMME_ENCEINTE, RoleUtilisateur.CONJOINT);

    /**
     * Création d'un compte professionnel ou administrateur (cahier des charges §3 :
     * l'administrateur gère les comptes professionnels). Un mot de passe temporaire est
     * généré ; il devra être changé à la première connexion.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public CreationProfessionnelResponse creerProfessionnel(CreerProfessionnelRequest request) {
        String email = AuthHelper.normaliserEmail(request.getEmail());
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailDejaUtiliseException("Un compte existe déjà avec cet email");
        }
        RegleAgeHelper.valider(request.getRole(), request.getDateNaissance());

        String motDePasseTemporaire = "Yay" + JetonSecurise.generer().substring(0, 12) + "9";
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.getNom().trim());
        utilisateur.setPrenom(request.getPrenom().trim());
        utilisateur.setEmail(email);
        utilisateur.setTelephone(request.getTelephone());
        utilisateur.setDateNaissance(request.getDateNaissance());
        utilisateur.setRole(request.getRole());
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasseTemporaire));
        utilisateur.setMotDePasseTemporaire(true);
        utilisateur.setActif(true);
        Utilisateur enregistre = utilisateurRepository.save(utilisateur);

        boolean emailEnvoye = emailService.estConfigure();
        emailService.envoyer(email, "YAY360 — Votre compte a été créé",
                "Bonjour " + enregistre.getPrenom() + ",\n\n"
                        + "Un compte YAY360 (" + enregistre.getRole() + ") a été créé pour vous.\n"
                        + "Identifiant : " + email + "\nMot de passe temporaire : " + motDePasseTemporaire + "\n\n"
                        + "Vous devrez le changer lors de votre première connexion.",
                null);

        // Avec un SMTP, le mot de passe n'est transmis qu'au professionnel : l'admin ne le connaît pas.
        return new CreationProfessionnelResponse(utilisateurMapper.versDto(enregistre),
                emailEnvoye ? null : motDePasseTemporaire, emailEnvoye);
    }

    public UtilisateurDto obtenirProfil(String email) {
        Utilisateur utilisateur = trouverParEmail(email);
        return utilisateurMapper.versDto(utilisateur);
    }

    public UtilisateurDto mettreAJourProfil(String email, UpdateProfileRequest request) {
        Utilisateur utilisateur = trouverParEmail(email);
        utilisateur.setNom(request.getNom().trim());
        utilisateur.setPrenom(request.getPrenom().trim());
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
        if (!ROLES_CONNUS.contains(role)) {
            throw new IllegalArgumentException("Rôle inconnu : " + role);
        }
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
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
    }

    private Utilisateur trouverParId(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id));
    }
}
