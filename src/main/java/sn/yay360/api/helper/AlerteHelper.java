package sn.yay360.api.helper;

import sn.yay360.api.dto.AlerteDto;
import sn.yay360.api.dto.DeclarerSymptomeRequest;
import sn.yay360.api.dto.PriseEnChargeRequest;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.mapper.AlerteMapper;
import sn.yay360.api.model.*;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Détecte et gère les alertes médicales déclenchées par les symptômes déclarés
 * par les femmes enceintes (§5.11). Notifie automatiquement les professionnels de santé.
 */
@Component
@RequiredArgsConstructor
public class AlerteHelper {

    private final AlerteMedicaleRepository alerteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final GrossesseHelper grossesseHelper;
    private final NotificationHelper notificationHelper;
    private final AlerteMapper alerteMapper;
    private final DossierFamilialHelper dossierFamilialHelper;

    private static final Map<String, String> NIVEAU_PAR_SYMPTOME = Map.ofEntries(
            Map.entry(TypeSymptome.SAIGNEMENTS, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.DOULEURS_ABDOMINALES, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.PERTE_LIQUIDE, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.DIMINUTION_MOUVEMENTS_BEBE, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.FIEVRE_ELEVEE, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.MAUX_DE_TETE_INTENSES, NiveauGravite.ELEVE),
            Map.entry(TypeSymptome.OEDEMES_SOUDAINS, NiveauGravite.ELEVE),
            Map.entry(TypeSymptome.VOMISSEMENTS_SEVERES, NiveauGravite.MODERE),
            Map.entry(TypeSymptome.AUTRE, NiveauGravite.MODERE)
    );

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public AlerteDto declarerSymptome(String emailFemme, DeclarerSymptomeRequest request) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        Grossesse grossesse = request.getGrossesseId() == null
                ? grossesseHelper.trouverActiveParEmail(emailFemme)
                : grossesseHelper.trouverParId(request.getGrossesseId());
        if (!dossierFamilialHelper.aAccesAuDossier(femme, grossesse.getFemmeEnceinte())) {
            throw new OperationNonAutoriseeException("Vous n'avez pas accès à ce dossier d'alerte");
        }
        if (!StatutGrossesse.EN_COURS.equals(grossesse.getStatut())) {
            throw new OperationNonAutoriseeException("Cette grossesse ne permet pas de déclarer un nouveau signalement");
        }

        String niveauGravite = NIVEAU_PAR_SYMPTOME.getOrDefault(request.getTypeSymptome(), NiveauGravite.MODERE);
        String recommandation = genererRecommandation(niveauGravite);

        AlerteMedicale alerte = new AlerteMedicale();
        alerte.setGrossesse(grossesse);
        alerte.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        alerte.setTypeSymptome(request.getTypeSymptome());
        alerte.setDescription(request.getDescription());
        alerte.setNiveauGravite(niveauGravite);
        alerte.setStatut(StatutAlerte.NOUVELLE);

        AlerteMedicale enregistree = alerteRepository.save(alerte);

        // Confirmation à la femme enceinte
        notificationHelper.envoyer(
                grossesse.getFemmeEnceinte(), grossesse, TypeNotification.ALERTE_MEDICALE,
                "Alerte enregistrée",
                recommandation
        );

        // Notification des professionnels de santé concernés
        notifierProfessionnelsSante(grossesse, enregistree, niveauGravite);

        return alerteMapper.versDto(enregistree, recommandation);
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public List<AlerteDto> mesAlertes(String emailFemme) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        return alerteRepository.findByFemmeEnceinteIdOrderByDateDeclarationDesc(femme.getId()).stream()
                .map(a -> alerteMapper.versDto(a, genererRecommandation(a.getNiveauGravite())))
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public List<AlerteDto> listerParStatut(String statut) {
        return alerteRepository.findByStatutOrderByDateDeclarationDesc(statut).stream()
                .map(a -> alerteMapper.versDto(a, genererRecommandation(a.getNiveauGravite())))
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public List<AlerteDto> listerUrgentes() {
        return alerteRepository.findByNiveauGraviteOrderByDateDeclarationDesc(NiveauGravite.URGENT).stream()
                .map(a -> alerteMapper.versDto(a, genererRecommandation(a.getNiveauGravite())))
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    public AlerteDto prendreEnCharge(String emailProfessionnel, Long id) {
        AlerteMedicale alerte = trouverParId(id);
        Utilisateur professionnel = utilisateurRepository.findByEmail(emailProfessionnel)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + emailProfessionnel));

        alerte.setStatut(StatutAlerte.PRISE_EN_CHARGE);
        alerte.setProfessionnelPriseEnCharge(professionnel);
        AlerteMedicale mise_a_jour = alerteRepository.save(alerte);

        notificationHelper.envoyer(
                alerte.getFemmeEnceinte(), alerte.getGrossesse(), TypeNotification.ALERTE_MEDICALE,
                "Votre alerte est prise en charge",
                "Un professionnel de santé (" + professionnel.getPrenom() + " " + professionnel.getNom()
                        + ") a pris en charge votre alerte. Il/elle vous contactera si nécessaire."
        );

        return alerteMapper.versDto(mise_a_jour, genererRecommandation(mise_a_jour.getNiveauGravite()));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    public AlerteDto resoudre(Long id, PriseEnChargeRequest request) {
        AlerteMedicale alerte = trouverParId(id);
        alerte.setStatut(StatutAlerte.RESOLUE);
        alerte.setNotesPriseEnCharge(request.getNotesPriseEnCharge());
        alerte.setDateResolution(LocalDateTime.now());
        AlerteMedicale mise_a_jour = alerteRepository.save(alerte);
        return alerteMapper.versDto(mise_a_jour, genererRecommandation(mise_a_jour.getNiveauGravite()));
    }

    @PreAuthorize("isAuthenticated()")
    public AlerteDto obtenirParId(String email, Long id) {
        AlerteMedicale alerte = trouverParId(id);
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
        boolean professionnel = List.of(RoleUtilisateur.ADMIN, RoleUtilisateur.MEDECIN, RoleUtilisateur.SAGE_FEMME)
                .contains(utilisateur.getRole());
        if (!professionnel && !dossierFamilialHelper.aAccesAuDossier(utilisateur, alerte.getFemmeEnceinte())) {
            throw new OperationNonAutoriseeException("Vous n'avez pas accès à cette alerte");
        }
        return alerteMapper.versDto(alerte, genererRecommandation(alerte.getNiveauGravite()));
    }

    // --- internes ---

    private void notifierProfessionnelsSante(Grossesse grossesse, AlerteMedicale alerte, String niveauGravite) {
        List<Utilisateur> professionnels = new ArrayList<>();
        professionnels.addAll(utilisateurRepository.findByRole(RoleUtilisateur.MEDECIN));
        professionnels.addAll(utilisateurRepository.findByRole(RoleUtilisateur.SAGE_FEMME));

        String titre = NiveauGravite.URGENT.equals(niveauGravite)
                ? "⚠ Alerte URGENTE - " + grossesse.getFemmeEnceinte().getPrenom() + " " + grossesse.getFemmeEnceinte().getNom()
                : "Nouvelle alerte médicale";

        String message = "Symptôme déclaré pendant la grossesse : " + alerte.getTypeSymptome()
                + " (gravité : " + niveauGravite + ") par " + grossesse.getFemmeEnceinte().getPrenom()
                + " " + grossesse.getFemmeEnceinte().getNom()
                + (grossesse.getFemmeEnceinte().getTelephone() != null
                        ? " - Tél : " + grossesse.getFemmeEnceinte().getTelephone() : "");

        for (Utilisateur professionnel : professionnels) {
            if (Boolean.TRUE.equals(professionnel.getActif())) {
                notificationHelper.envoyer(professionnel, grossesse, TypeNotification.ALERTE_MEDICALE, titre, message);
            }
        }
    }

    private String genererRecommandation(String niveauGravite) {
        return switch (niveauGravite) {
            case NiveauGravite.URGENT -> "Consultez en urgence un professionnel de santé ou rendez-vous immédiatement à l'établissement de santé le plus proche.";
            case NiveauGravite.ELEVE -> "Prenez rapidement contact avec votre professionnel de santé pour un avis médical.";
            default -> "Surveillez l'évolution de ce symptôme et signalez tout changement à votre professionnel de santé.";
        };
    }

    private AlerteMedicale trouverParId(Long id) {
        return alerteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte introuvable avec l'id : " + id));
    }
}
