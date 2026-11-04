package sn.yay360.api.helper;

import sn.yay360.api.dto.InvitationConjointRequest;
import sn.yay360.api.dto.InvitationConjointResponse;
import sn.yay360.api.security.JetonSecurise;
import sn.yay360.api.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.model.InvitationConjoint;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.InvitationConjointRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Transactional
public class DossierFamilialHelper {

    private final UtilisateurRepository utilisateurRepository;
    private final InvitationConjointRepository invitationRepository;
    private final EmailService emailService;

    static final long DUREE_VALIDITE_HEURES = 72;

    @Value("${yay360.frontend-url:http://localhost:4200}")
    private String frontendUrl = "http://localhost:4200";

    @PreAuthorize("hasAuthority('FEMME_ENCEINTE')")
    @Transactional
    public InvitationConjointResponse inviterConjoint(String emailFemme, InvitationConjointRequest request) {
        Utilisateur femme = utilisateurRepository.findByEmailIgnoreCase(emailFemme)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + emailFemme));
        String emailConjoint = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (utilisateurRepository.existsByEmailIgnoreCase(emailConjoint)) {
            throw new OperationNonAutoriseeException("Un compte existe déjà avec cet email");
        }
        if (utilisateurRepository.findByFemmeAssocieeId(femme.getId()).isPresent()) {
            throw new OperationNonAutoriseeException("Un conjoint est déjà associé à votre dossier");
        }
        if (invitationRepository.existsByEmailIgnoreCaseAndAccepteeFalseAndDateExpirationAfter(
                emailConjoint, LocalDateTime.now())) {
            throw new OperationNonAutoriseeException("Une invitation est déjà en attente pour cet email");
        }

        String code = JetonSecurise.generer();
        InvitationConjoint invitation = new InvitationConjoint();
        invitation.setFemme(femme);
        invitation.setEmail(emailConjoint);
        invitation.setToken(JetonSecurise.hacher(code)); // seule l'empreinte est stockée
        invitation.setDateExpiration(LocalDateTime.now().plusHours(DUREE_VALIDITE_HEURES));
        invitation.setAcceptee(false);
        invitationRepository.save(invitation);

        String lien = frontendUrl + "/accepter-invitation-conjoint?token=" + code;
        emailService.envoyer(emailConjoint, "YAY360 — Invitation à suivre la grossesse de " + femme.getPrenom(),
                femme.getPrenom() + " " + femme.getNom() + " vous invite à rejoindre son dossier de suivi de grossesse"
                        + " sur YAY360.\n\nCréez votre compte avec ce lien (valable " + DUREE_VALIDITE_HEURES
                        + " heures, utilisable une seule fois) :\n" + lien,
                lien);

        boolean emailEnvoye = emailService.estConfigure();
        String message = emailEnvoye
                ? "Invitation envoyée par email. Le code est valable 72 heures."
                : "Invitation créée. Transmettez ce code à votre conjoint : il est valable 72 heures.";
        return new InvitationConjointResponse(message, code, invitation.getDateExpiration(), emailEnvoye);
    }

    public Utilisateur trouverFemmeAssociee(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
        if (RoleUtilisateur.FEMME_ENCEINTE.equals(utilisateur.getRole())) {
            return utilisateur;
        }
        if (RoleUtilisateur.CONJOINT.equals(utilisateur.getRole()) && utilisateur.getFemmeAssociee() != null) {
            return utilisateur.getFemmeAssociee();
        }
        throw new OperationNonAutoriseeException("Aucun dossier maternel n'est associé à cet utilisateur");
    }

    public boolean aAccesAuDossier(Utilisateur utilisateur, Utilisateur femme) {
        return utilisateur.getId().equals(femme.getId())
                || (RoleUtilisateur.CONJOINT.equals(utilisateur.getRole())
                && utilisateur.getFemmeAssociee() != null
                && utilisateur.getFemmeAssociee().getId().equals(femme.getId()));
    }
}
