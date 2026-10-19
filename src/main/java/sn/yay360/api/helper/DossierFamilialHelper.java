package sn.yay360.api.helper;

import sn.yay360.api.dto.InvitationConjointRequest;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DossierFamilialHelper {

    private final UtilisateurRepository utilisateurRepository;
    private final InvitationConjointRepository invitationRepository;

    @PreAuthorize("hasAuthority('FEMME_ENCEINTE')")
    @Transactional
    public void inviterConjoint(String emailFemme, InvitationConjointRequest request) {
        Utilisateur femme = utilisateurRepository.findByEmail(emailFemme)
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

        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        InvitationConjoint invitation = new InvitationConjoint();
        invitation.setFemme(femme);
        invitation.setEmail(emailConjoint);
        invitation.setToken(token);
        invitation.setDateExpiration(LocalDateTime.now().plusHours(72));
        invitation.setAcceptee(false);
        invitationRepository.save(invitation);

        System.out.println("=== [YAY360] Invitation du conjoint (envoi email à intégrer) ===");
        System.out.println("Destinataire : " + emailConjoint);
        System.out.println("Lien/code d'acceptation : " + token);
        System.out.println("Expiration : " + invitation.getDateExpiration());
        System.out.println("=================================================================");
    }

    public Utilisateur trouverFemmeAssociee(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
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
