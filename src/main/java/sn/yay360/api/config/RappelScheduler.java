package sn.yay360.api.config;

import sn.yay360.api.helper.CalculObstetricalHelper;
import sn.yay360.api.helper.NotificationHelper;
import sn.yay360.api.model.ConseilGrossesse;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.StatutRendezVous;
import sn.yay360.api.model.TypeNotification;
import sn.yay360.api.model.TypeRendezVous;
import sn.yay360.api.repo.ConseilGrossesseRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Génère automatiquement les rappels et conseils hebdomadaires (§5.10) :
 * - rappels de rendez-vous 24h avant l'échéance (tous les jours à 8h)
 * - conseils hebdomadaires personnalisés (chaque lundi à 9h)
 *
 * Composant d'infrastructure au même titre que DataInitializer : il orchestre
 * les helpers existants (NotificationHelper, CalculObstetricalHelper) sans dupliquer
 * la logique métier qui y est déjà centralisée.
 */
@Component
@RequiredArgsConstructor
public class RappelScheduler {

    private final RendezVousRepository rendezVousRepository;
    private final GrossesseRepository grossesseRepository;
    private final ConseilGrossesseRepository conseilGrossesseRepository;
    private final NotificationHelper notificationHelper;
    private final CalculObstetricalHelper calcul;

    /**
     * Tous les jours à 8h00 : rappelle les rendez-vous prévus dans les 24h à venir.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void rappelerRendezVousAVenir() {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime dansVingtQuatreHeures = maintenant.plusHours(24);

        List<RendezVous> rdvAVenir = rendezVousRepository.findByStatutOrderByDateHeureAsc(StatutRendezVous.PLANIFIE);
        rdvAVenir.addAll(rendezVousRepository.findByStatutOrderByDateHeureAsc(StatutRendezVous.CONFIRME));

        for (RendezVous rdv : rdvAVenir) {
            if (rdv.getDateHeure().isAfter(maintenant) && rdv.getDateHeure().isBefore(dansVingtQuatreHeures)) {
                String type = TypeRendezVous.ECHOGRAPHIE.equals(rdv.getType())
                        ? TypeNotification.RAPPEL_ECHOGRAPHIE
                        : TypeRendezVous.VACCINATION.equals(rdv.getType())
                            ? TypeNotification.RAPPEL_VACCINATION
                            : TypeNotification.RAPPEL_CONSULTATION;

                notificationHelper.envoyer(
                        rdv.getFemmeEnceinte(), rdv.getGrossesse(), type,
                        "Rappel de rendez-vous",
                        "Vous avez un rendez-vous (" + rdv.getType() + ") prévu le "
                                + rdv.getDateHeure() + ". Merci de ne pas le manquer."
                );
            }
        }
    }

    /**
     * Chaque lundi à 9h00 : envoie les conseils personnalisés de la semaine
     * à toutes les femmes ayant une grossesse en cours.
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void envoyerConseilsHebdomadaires() {
        List<Grossesse> grossessesActives = grossesseRepository.findByStatut(StatutGrossesse.EN_COURS);

        for (Grossesse grossesse : grossessesActives) {
            int semaine = calcul.calculerSemaineActuelle(grossesse.getDateDernieresRegles());

            List<ConseilGrossesse> conseils = conseilGrossesseRepository
                    .findByActifTrueAndSemaineDebutLessThanEqualAndSemaineFinGreaterThanEqual(semaine, semaine);

            if (conseils.isEmpty()) {
                continue;
            }

            StringBuilder message = new StringBuilder("Voici vos conseils pour la semaine " + semaine + " : ");
            for (ConseilGrossesse conseil : conseils) {
                message.append("\n- ").append(conseil.getTitre());
            }

            notificationHelper.envoyer(
                    grossesse.getFemmeEnceinte(), grossesse, TypeNotification.CONSEIL_HEBDOMADAIRE,
                    "Vos conseils de la semaine " + semaine,
                    message.toString()
            );
        }
    }
}
