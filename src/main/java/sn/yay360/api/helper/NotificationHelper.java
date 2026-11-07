package sn.yay360.api.helper;

import sn.yay360.api.dto.NotificationDto;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.NotificationMapper;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.Notification;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.NotificationRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gère l'envoi et la consultation des notifications intelligentes (§5.10) :
 * rappels de consultation, d'échographie, de vaccination, de médicaments,
 * conseils hebdomadaires et informations importantes.
 *
 * L'envoi réel (SMS/push) est simulé en console — comme l'OTP de wallet-service —
 * en attendant le branchement d'un fournisseur SMS/push en production.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class NotificationHelper {

    private static final Logger log = LoggerFactory.getLogger(NotificationHelper.class);

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationMapper notificationMapper;

    /** Notification destinée à un soignant : pas de copie au conjoint. */
    public Notification envoyerSansCopieConjoint(Utilisateur destinataire, Grossesse grossesse, String type,
                                                 String titre, String message) {
        Notification notification = new Notification();
        notification.setDestinataire(destinataire);
        notification.setGrossesse(grossesse);
        notification.setType(type);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setLue(false);
        return notificationRepository.save(notification);
    }

    /** Notification destinée à une patiente : copie automatique à son conjoint lié (§4.3). */
    public Notification envoyer(Utilisateur destinataire, Grossesse grossesse, String type, String titre, String message) {
        Notification notification = new Notification();
        notification.setDestinataire(destinataire);
        notification.setGrossesse(grossesse);
        notification.setType(type);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setLue(false);

        Notification enregistree = notificationRepository.save(notification);
        utilisateurRepository.findByFemmeAssocieeId(destinataire.getId()).ifPresent(conjoint -> {
            Notification copie = new Notification();
            copie.setDestinataire(conjoint);
            copie.setGrossesse(grossesse);
            copie.setType(type);
            copie.setTitre(titre);
            copie.setMessage(message);
            copie.setLue(false);
            notificationRepository.save(copie);
        });

        // Pas de donnée personnelle ni médicale dans les logs : identifiant et type uniquement
        log.debug("[YAY360] Notification enregistrée -> destinataireId={}, type={}", destinataire.getId(), type);

        return enregistree;
    }

    public List<NotificationDto> mesNotifications(String email) {
        Utilisateur utilisateur = trouverUtilisateur(email);
        return notificationRepository.findByDestinataireIdOrderByDateEnvoiDesc(utilisateur.getId()).stream()
                .map(notificationMapper::versDto)
                .toList();
    }

    public List<NotificationDto> mesNotificationsNonLues(String email) {
        Utilisateur utilisateur = trouverUtilisateur(email);
        return notificationRepository.findByDestinataireIdAndLueFalseOrderByDateEnvoiDesc(utilisateur.getId()).stream()
                .map(notificationMapper::versDto)
                .toList();
    }

    public long compterNonLues(String email) {
        Utilisateur utilisateur = trouverUtilisateur(email);
        return notificationRepository.countByDestinataireIdAndLueFalse(utilisateur.getId());
    }

    public NotificationDto marquerLue(String email, Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable avec l'id : " + id));

        Utilisateur utilisateur = trouverUtilisateur(email);
        if (!notification.getDestinataire().getId().equals(utilisateur.getId())) {
            throw new OperationNonAutoriseeException("Vous n'avez pas accès à cette notification");
        }

        notification.setLue(true);
        notification.setDateLecture(LocalDateTime.now());
        return notificationMapper.versDto(notificationRepository.save(notification));
    }

    public void marquerToutesLues(String email) {
        Utilisateur utilisateur = trouverUtilisateur(email);
        List<Notification> nonLues = notificationRepository
                .findByDestinataireIdAndLueFalseOrderByDateEnvoiDesc(utilisateur.getId());
        LocalDateTime maintenant = LocalDateTime.now();
        nonLues.forEach(n -> {
            n.setLue(true);
            n.setDateLecture(maintenant);
        });
        notificationRepository.saveAll(nonLues);
    }

    private Utilisateur trouverUtilisateur(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
    }
}
