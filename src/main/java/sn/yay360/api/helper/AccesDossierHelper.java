package sn.yay360.api.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.UtilisateurRepository;

/**
 * Point unique de décision pour l'accès aux dossiers médicaux (cloisonnement, §6).
 *
 * <ul>
 *   <li>La femme enceinte et son conjoint lié accèdent à leur propre dossier.</li>
 *   <li>Un médecin ou une sage-femme n'accède qu'aux grossesses qu'il <b>suit</b> : établissement
 *       de suivi dont il est membre, ou lien de soin direct (rendez-vous assigné, consultation
 *       enregistrée, alerte prise en charge).</li>
 *   <li>L'administrateur a une lecture de supervision (grossesses, rendez-vous, alertes) mais
 *       jamais le contenu clinique des consultations (diagnostic, prescriptions).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AccesDossierHelper {

    private final UtilisateurRepository utilisateurRepository;
    private final GrossesseRepository grossesseRepository;
    private final EtablissementSanteRepository etablissementRepository;
    private final DossierFamilialHelper dossierFamilialHelper;

    public Utilisateur utilisateur(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    public static boolean estProfessionnel(Utilisateur utilisateur) {
        return RoleUtilisateur.MEDECIN.equals(utilisateur.getRole())
                || RoleUtilisateur.SAGE_FEMME.equals(utilisateur.getRole());
    }

    public static boolean estAdmin(Utilisateur utilisateur) {
        return RoleUtilisateur.ADMIN.equals(utilisateur.getRole());
    }

    public boolean estFamille(Utilisateur utilisateur, Grossesse grossesse) {
        return dossierFamilialHelper.aAccesAuDossier(utilisateur, grossesse.getFemmeEnceinte());
    }

    public boolean suit(Utilisateur professionnel, Grossesse grossesse) {
        return estProfessionnel(professionnel)
                && grossesseRepository.estSuivieParProfessionnel(grossesse.getId(), professionnel.getId());
    }

    /**
     * Une grossesse est « sans suivi » si AUCUN soignant actif ne la suit — y compris quand elle est
     * rattachée à un établissement où personne n'exerce encore. Même règle que la notification des
     * alertes : une urgence a donc toujours au moins un soignant capable de la prendre en charge.
     */
    public boolean estSansSuivi(Grossesse grossesse) {
        return utilisateurRepository.findSoignantsSuivant(grossesse.getId()).isEmpty();
    }

    public boolean estMembre(Utilisateur professionnel, Long etablissementId) {
        return etablissementRepository.existsByIdAndProfessionnelsId(etablissementId, professionnel.getId());
    }

    /** Lecture du dossier (identité, calendrier, suivi, rendez-vous, alertes). */
    public void verifierLecture(Utilisateur utilisateur, Grossesse grossesse) {
        if (estAdmin(utilisateur) || estFamille(utilisateur, grossesse) || suit(utilisateur, grossesse)) {
            return;
        }
        throw new OperationNonAutoriseeException("Vous ne suivez pas cette patiente : accès au dossier refusé");
    }

    /** Lecture du contenu clinique (consultations) : famille et soignants qui suivent, pas l'admin. */
    public void verifierLectureClinique(Utilisateur utilisateur, Grossesse grossesse) {
        if (estFamille(utilisateur, grossesse) || suit(utilisateur, grossesse)) {
            return;
        }
        throw new OperationNonAutoriseeException("Accès au contenu médical réservé aux soignants qui suivent la patiente");
    }

    /** Actes de soin (consultation, risque, clôture, rendez-vous planifié par un soignant). */
    public void verifierSoin(Utilisateur professionnel, Grossesse grossesse) {
        if (!suit(professionnel, grossesse)) {
            throw new OperationNonAutoriseeException(
                    "Vous ne suivez pas cette patiente. Rattachez d'abord la grossesse à votre établissement.");
        }
    }
}
