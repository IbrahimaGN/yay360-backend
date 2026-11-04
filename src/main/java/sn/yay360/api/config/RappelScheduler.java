package sn.yay360.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import sn.yay360.api.dto.PlanCpnDto;
import sn.yay360.api.helper.CalculObstetricalHelper;
import sn.yay360.api.helper.IntelligenceCliniqueHelper;
import sn.yay360.api.helper.MoteurReglesCliniques;
import sn.yay360.api.repo.NotificationRepository;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tâches planifiées (§4.6) :
 * <ul>
 *   <li>toutes les heures : rappel des rendez-vous des 24 prochaines heures, une seule fois par rendez-vous ;</li>
 *   <li>chaque lundi à 9 h : conseils de la semaine pour chaque grossesse en cours.</li>
 * </ul>
 * Chaque envoi s'exécute dans sa propre transaction : l'échec d'un envoi n'empêche pas les autres
 * et les relations JPA chargées à la demande restent accessibles.
 */
@Slf4j
@Component
public class RappelScheduler {

    static final String FUSEAU = "Africa/Dakar";
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm");

    private final RendezVousRepository rendezVousRepository;
    private final GrossesseRepository grossesseRepository;
    private final ConseilGrossesseRepository conseilGrossesseRepository;
    private final NotificationHelper notificationHelper;
    private final CalculObstetricalHelper calcul;
    private final TransactionTemplate transaction;
    private final IntelligenceCliniqueHelper intelligenceClinique;
    private final NotificationRepository notificationRepository;

    public RappelScheduler(RendezVousRepository rendezVousRepository, GrossesseRepository grossesseRepository,
                           ConseilGrossesseRepository conseilGrossesseRepository, NotificationHelper notificationHelper,
                           CalculObstetricalHelper calcul, PlatformTransactionManager transactionManager,
                           IntelligenceCliniqueHelper intelligenceClinique,
                           NotificationRepository notificationRepository) {
        this.intelligenceClinique = intelligenceClinique;
        this.notificationRepository = notificationRepository;
        this.rendezVousRepository = rendezVousRepository;
        this.grossesseRepository = grossesseRepository;
        this.conseilGrossesseRepository = conseilGrossesseRepository;
        this.notificationHelper = notificationHelper;
        this.calcul = calcul;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(cron = "0 0 * * * *", zone = FUSEAU)
    public void rappelerRendezVousAVenir() {
        LocalDateTime maintenant = LocalDateTime.now();
        List<Long> ids = transaction.execute(status -> rendezVousRepository
                .findARappeler(maintenant, maintenant.plusHours(24),
                        List.of(StatutRendezVous.PLANIFIE, StatutRendezVous.CONFIRME))
                .stream().map(RendezVous::getId).toList());
        int envoyes = 0;
        for (Long id : ids == null ? List.<Long>of() : ids) {
            try {
                transaction.executeWithoutResult(status -> rappeler(id));
                envoyes++;
            } catch (RuntimeException e) {
                log.error("[YAY360] Échec du rappel du rendez-vous id={} : {}", id, e.getMessage());
            }
        }
        if (envoyes > 0) {
            log.info("[YAY360] {} rappel(s) de rendez-vous envoyé(s)", envoyes);
        }
    }

    void rappeler(Long rendezVousId) {
        RendezVous rdv = rendezVousRepository.findById(rendezVousId).orElseThrow();
        String type = switch (rdv.getType()) {
            case TypeRendezVous.ECHOGRAPHIE -> TypeNotification.RAPPEL_ECHOGRAPHIE;
            case TypeRendezVous.VACCINATION -> TypeNotification.RAPPEL_VACCINATION;
            default -> TypeNotification.RAPPEL_CONSULTATION;
        };
        String lieu = rdv.getEtablissement() != null ? " à " + rdv.getEtablissement().getNom() : "";
        notificationHelper.envoyer(rdv.getFemmeEnceinte(), rdv.getGrossesse(), type,
                "Rappel : rendez-vous le " + rdv.getDateHeure().format(FORMAT),
                "Vous avez un rendez-vous (" + libelle(rdv.getType()) + ") le " + rdv.getDateHeure().format(FORMAT)
                        + lieu + ". Pensez à apporter votre carnet de santé.");
        rdv.setRappelEnvoye(true);
        rendezVousRepository.save(rdv);
    }

    @Scheduled(cron = "0 0 9 * * MON", zone = FUSEAU)
    public void envoyerConseilsHebdomadaires() {
        List<Long> ids = transaction.execute(status -> grossesseRepository.findByStatut(StatutGrossesse.EN_COURS)
                .stream().map(Grossesse::getId).toList());
        for (Long id : ids == null ? List.<Long>of() : ids) {
            try {
                transaction.executeWithoutResult(status -> envoyerConseils(id));
            } catch (RuntimeException e) {
                log.error("[YAY360] Échec de l'envoi des conseils pour la grossesse id={} : {}", id, e.getMessage());
            }
        }
    }

    void envoyerConseils(Long grossesseId) {
        Grossesse grossesse = grossesseRepository.findById(grossesseId).orElseThrow();
        rappelerCpnEnRetard(grossesse);
        int semaine = calcul.calculerSemaineActuelle(grossesse.getDateDernieresRegles());
        List<ConseilGrossesse> conseils = conseilGrossesseRepository
                .findByActifTrueAndSemaineDebutLessThanEqualAndSemaineFinGreaterThanEqual(semaine, semaine);
        if (conseils.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder("Voici vos conseils pour la semaine " + semaine + " :");
        conseils.forEach(conseil -> message.append("\n- ").append(conseil.getTitre()));
        notificationHelper.envoyer(grossesse.getFemmeEnceinte(), grossesse, TypeNotification.CONSEIL_HEBDOMADAIRE,
                "Vos conseils de la semaine " + semaine, message.toString());
    }

    /** Rappel unique par contact prénatal manqué (plan OMS calculé par le moteur de règles). */
    void rappelerCpnEnRetard(Grossesse grossesse) {
        PlanCpnDto plan = intelligenceClinique.planCpn(grossesse);
        for (MoteurReglesCliniques.ContactCpn contact : plan.getContacts()) {
            if (!MoteurReglesCliniques.CPN_EN_RETARD.equals(contact.statut())) {
                continue;
            }
            String titre = "Consultation prénatale n°" + contact.numero() + " à programmer";
            if (notificationRepository.existsByDestinataireIdAndGrossesseIdAndTitre(
                    grossesse.getFemmeEnceinte().getId(), grossesse.getId(), titre)) {
                continue;
            }
            notificationHelper.envoyer(grossesse.getFemmeEnceinte(), grossesse, TypeNotification.RAPPEL_CONSULTATION,
                    titre, "La consultation prénatale prévue vers " + contact.semaineCible()
                            + " semaines n'a pas encore eu lieu. Prenez rendez-vous dans votre établissement de santé.");
        }
    }

    private static String libelle(String typeRendezVous) {
        return switch (typeRendezVous) {
            case TypeRendezVous.CONSULTATION_PRENATALE -> "consultation prénatale";
            case TypeRendezVous.ECHOGRAPHIE -> "échographie";
            case TypeRendezVous.VACCINATION -> "vaccination";
            case TypeRendezVous.EXAMEN_LABORATOIRE -> "examen de laboratoire";
            default -> "rendez-vous médical";
        };
    }
}
