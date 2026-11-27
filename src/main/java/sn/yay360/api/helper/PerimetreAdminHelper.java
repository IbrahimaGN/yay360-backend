package sn.yay360.api.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.UtilisateurRepository;

import java.util.List;

/**
 * Point unique de décision pour le périmètre des administrateurs.
 *
 * <ul>
 *   <li>L'administrateur de la plateforme (ADMIN) gère tous les établissements et tous les comptes.</li>
 *   <li>L'administrateur d'établissement (ADMIN_ETABLISSEMENT) ne gère que son établissement et les
 *       soignants qui y exercent.</li>
 *   <li>Aucun des deux n'accède aux dossiers de santé : ce cloisonnement est appliqué par
 *       {@link AccesDossierHelper}, qui ne leur ouvre aucun droit.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PerimetreAdminHelper {

    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementSanteRepository etablissementRepository;

    public Utilisateur auteur(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    public static boolean estAdminPlateforme(Utilisateur utilisateur) {
        return RoleUtilisateur.ADMIN.equals(utilisateur.getRole());
    }

    public static boolean estAdminEtablissement(Utilisateur utilisateur) {
        return RoleUtilisateur.ADMIN_ETABLISSEMENT.equals(utilisateur.getRole());
    }

    public static boolean estSoignant(Utilisateur utilisateur) {
        return RoleUtilisateur.MEDECIN.equals(utilisateur.getRole())
                || RoleUtilisateur.SAGE_FEMME.equals(utilisateur.getRole());
    }

    /** Établissement géré par un administrateur d'établissement. */
    public EtablissementSante etablissementAdministre(Utilisateur admin) {
        if (!estAdminEtablissement(admin) || admin.getEtablissementAdministre() == null) {
            throw new OperationNonAutoriseeException("Ce compte n'administre aucun établissement");
        }
        return admin.getEtablissementAdministre();
    }

    /** L'auteur peut-il gérer cet établissement ? Plateforme : tous ; établissement : le sien seulement. */
    public void verifierGestionEtablissement(Utilisateur auteur, Long etablissementId) {
        if (estAdminPlateforme(auteur)) {
            return;
        }
        if (estAdminEtablissement(auteur) && etablissementAdministre(auteur).getId().equals(etablissementId)) {
            return;
        }
        throw new OperationNonAutoriseeException("Vous ne pouvez gérer que votre propre établissement");
    }

    /**
     * L'auteur peut-il activer ou désactiver ce compte ? Un administrateur d'établissement n'agit que sur
     * un soignant qui exerce uniquement dans son établissement : désactiver un compte partagé couperait
     * l'accès du soignant aux autres établissements, qui ne relèvent pas de lui.
     */
    public void verifierGestionCompte(Utilisateur auteur, Utilisateur cible) {
        if (estAdminPlateforme(auteur)) {
            return;
        }
        if (!estAdminEtablissement(auteur) || !estSoignant(cible)) {
            throw new OperationNonAutoriseeException("Vous ne pouvez gérer que les soignants de votre établissement");
        }
        Long sien = etablissementAdministre(auteur).getId();
        List<Long> exercices = etablissementRepository.findByProfessionnelsId(cible.getId()).stream()
                .map(EtablissementSante::getId)
                .toList();
        if (!exercices.contains(sien)) {
            throw new OperationNonAutoriseeException("Ce soignant n'exerce pas dans votre établissement");
        }
        if (exercices.size() > 1) {
            throw new OperationNonAutoriseeException(
                    "Ce soignant exerce aussi dans un autre établissement : retirez-le de votre établissement "
                            + "au lieu de désactiver son compte");
        }
    }
}
