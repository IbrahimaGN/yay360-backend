package sn.yay360.api.helper;

import sn.yay360.api.dto.CreationProfessionnelResponse;
import sn.yay360.api.dto.CreerProfessionnelRequest;
import sn.yay360.api.dto.UpdateProfileRequest;
import sn.yay360.api.exception.EmailDejaUtiliseException;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.repo.EtablissementSanteRepository;
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

import java.util.Comparator;
import java.util.List;

/**
 * Contient toute la logique métier liée à la gestion des utilisateurs :
 * consultation de profil, mise à jour, activation/désactivation, suppression.
 *
 * <p>Deux niveaux d'administration : l'administrateur de la plateforme gère tous les comptes,
 * l'administrateur d'établissement ne gère que les soignants de son établissement
 * (périmètre vérifié par {@link PerimetreAdminHelper}).</p>
 */
@Component
@RequiredArgsConstructor
@Transactional
public class UtilisateurHelper {

    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementSanteRepository etablissementRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PerimetreAdminHelper perimetre;

    private static final java.util.Set<String> ROLES_CONNUS = java.util.Set.of(
            RoleUtilisateur.ADMIN, RoleUtilisateur.ADMIN_ETABLISSEMENT, RoleUtilisateur.MEDECIN,
            RoleUtilisateur.SAGE_FEMME, RoleUtilisateur.FEMME_ENCEINTE, RoleUtilisateur.CONJOINT);

    /**
     * Création d'un compte par un administrateur. Un mot de passe temporaire est généré ; il devra être
     * changé à la première connexion. L'administrateur de la plateforme crée uniquement des administrateurs :
     * d'autres administrateurs de la plateforme, et les administrateurs d'établissement (un seul par
     * établissement) ; chaque administrateur d'établissement crée les médecins et les sages-femmes,
     * rattachés d'office à son établissement.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ADMIN_ETABLISSEMENT')")
    @Transactional
    public CreationProfessionnelResponse creerProfessionnel(String emailAuteur, CreerProfessionnelRequest request) {
        Utilisateur auteur = perimetre.auteur(emailAuteur);
        EtablissementSante etablissement = etablissementDuNouveauCompte(auteur, request);

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
        if (RoleUtilisateur.ADMIN_ETABLISSEMENT.equals(request.getRole())) {
            utilisateur.setEtablissementAdministre(etablissement);
        }
        Utilisateur enregistre = utilisateurRepository.save(utilisateur);

        if (etablissement != null && PerimetreAdminHelper.estSoignant(enregistre)) {
            etablissement.getProfessionnels().add(enregistre);
            etablissementRepository.save(etablissement);
        }

        boolean emailEnvoye = emailService.estConfigure();
        emailService.envoyer(email, "YAY360 — Votre compte a été créé",
                "Bonjour " + enregistre.getPrenom() + ",\n\n"
                        + "Un compte YAY360 (" + enregistre.getRole() + ") a été créé pour vous"
                        + (etablissement != null ? " à " + etablissement.getNom() : "") + ".\n"
                        + "Identifiant : " + email + "\nMot de passe temporaire : " + motDePasseTemporaire + "\n\n"
                        + "Vous devrez le changer lors de votre première connexion.",
                null);

        // Avec un SMTP, le mot de passe n'est transmis qu'au professionnel : l'admin ne le connaît pas.
        return new CreationProfessionnelResponse(utilisateurMapper.versDto(enregistre),
                emailEnvoye ? null : motDePasseTemporaire, emailEnvoye);
    }

    /** Établissement du nouveau compte selon l'auteur et le rôle demandé (null : aucun rattachement). */
    private EtablissementSante etablissementDuNouveauCompte(Utilisateur auteur, CreerProfessionnelRequest request) {
        String role = request.getRole();
        if (PerimetreAdminHelper.estAdminEtablissement(auteur)) {
            if (!RoleUtilisateur.MEDECIN.equals(role) && !RoleUtilisateur.SAGE_FEMME.equals(role)) {
                throw new OperationNonAutoriseeException(
                        "Un administrateur d'établissement ne crée que des comptes de médecin ou de sage-femme");
            }
            return perimetre.etablissementAdministre(auteur);
        }
        if (RoleUtilisateur.ADMIN.equals(role)) {
            if (request.getEtablissementId() != null) {
                throw new IllegalArgumentException("Un administrateur de la plateforme n'est rattaché à aucun établissement");
            }
            return null;
        }
        if (!RoleUtilisateur.ADMIN_ETABLISSEMENT.equals(role)) {
            throw new OperationNonAutoriseeException("L'administrateur de la plateforme crée uniquement des "
                    + "administrateurs : les soignants sont créés par l'administrateur de leur établissement");
        }
        if (request.getEtablissementId() == null) {
            throw new IllegalArgumentException("L'établissement est obligatoire pour un administrateur d'établissement");
        }
        EtablissementSante etablissement = etablissementRepository.findById(request.getEtablissementId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Établissement introuvable avec l'id : " + request.getEtablissementId()));
        if (utilisateurRepository.existsByEtablissementAdministreId(etablissement.getId())) {
            throw new IllegalArgumentException("Cet établissement a déjà un administrateur");
        }
        return etablissement;
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

    /** Soignants qui exercent dans l'établissement de l'administrateur connecté. */
    @PreAuthorize("hasAuthority('ADMIN_ETABLISSEMENT')")
    public List<UtilisateurDto> listerSoignantsDeMonEtablissement(String emailAuteur) {
        EtablissementSante etablissement = perimetre.etablissementAdministre(perimetre.auteur(emailAuteur));
        return etablissement.getProfessionnels().stream()
                .sorted(Comparator.comparing(Utilisateur::getNom, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Utilisateur::getPrenom, String.CASE_INSENSITIVE_ORDER))
                .map(utilisateurMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public UtilisateurDto obtenirParId(Long id) {
        Utilisateur utilisateur = trouverParId(id);
        return utilisateurMapper.versDto(utilisateur);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ADMIN_ETABLISSEMENT')")
    public UtilisateurDto desactiverCompte(Long id, String emailAuteur) {
        Utilisateur auteur = perimetre.auteur(emailAuteur);
        if (id.equals(auteur.getId())) {
            throw new OperationNonAutoriseeException("Vous ne pouvez pas désactiver votre propre compte");
        }
        Utilisateur utilisateur = trouverParId(id);
        perimetre.verifierGestionCompte(auteur, utilisateur);
        utilisateur.setActif(false);
        return utilisateurMapper.versDto(utilisateurRepository.save(utilisateur));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ADMIN_ETABLISSEMENT')")
    public UtilisateurDto activerCompte(Long id, String emailAuteur) {
        Utilisateur utilisateur = trouverParId(id);
        perimetre.verifierGestionCompte(perimetre.auteur(emailAuteur), utilisateur);
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
