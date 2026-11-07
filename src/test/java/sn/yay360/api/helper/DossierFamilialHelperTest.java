package sn.yay360.api.helper;

import sn.yay360.api.dto.InvitationConjointRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.model.InvitationConjoint;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.InvitationConjointRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import sn.yay360.api.security.JetonSecurise;
import sn.yay360.api.service.EmailService;
import sn.yay360.api.dto.InvitationConjointResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DossierFamilialHelperTest {

    private UtilisateurRepository utilisateurRepository;
    private InvitationConjointRepository invitationRepository;
    private DossierFamilialHelper helper;
    private EmailService emailService;
    private Utilisateur femme;

    @BeforeEach
    void setUp() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        invitationRepository = mock(InvitationConjointRepository.class);
        emailService = mock(EmailService.class);
        helper = new DossierFamilialHelper(utilisateurRepository, invitationRepository, emailService);
        femme = new Utilisateur();
        femme.setId(7L);
        femme.setEmail("awa@example.com");
        femme.setRole(RoleUtilisateur.FEMME_ENCEINTE);
        when(utilisateurRepository.findByEmailIgnoreCase(femme.getEmail())).thenReturn(Optional.of(femme));
        when(utilisateurRepository.findByFemmeAssocieeId(7L)).thenReturn(Optional.empty());
        when(utilisateurRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(invitationRepository.existsByEmailIgnoreCaseAndAccepteeFalseAndDateExpirationAfter(
                anyString(), any(LocalDateTime.class))).thenReturn(false);
    }

    @Test
    void inviterConjointCreeUneInvitationLieeAvecExpirationDe72Heures() {
        InvitationConjointRequest request = new InvitationConjointRequest();
        request.setEmail("  Mari@Example.com ");

        ArgumentCaptor<InvitationConjoint> captor = ArgumentCaptor.forClass(InvitationConjoint.class);
        InvitationConjointResponse reponse = helper.inviterConjoint(femme.getEmail(), request);

        verify(invitationRepository).save(captor.capture());
        InvitationConjoint invitation = captor.getValue();
        assertThat(invitation.getFemme()).isSameAs(femme);
        assertThat(invitation.getEmail()).isEqualTo("mari@example.com");
        // Le code est remis à la femme, seule son empreinte SHA-256 est stockée
        assertThat(reponse.getCode()).isNotBlank();
        assertThat(invitation.getToken()).isEqualTo(JetonSecurise.hacher(reponse.getCode()));
        assertThat(invitation.getToken()).doesNotContain(reponse.getCode());
        verify(emailService).envoyer(eq("mari@example.com"), anyString(), anyString(), contains(reponse.getCode()));
        assertThat(invitation.getDateExpiration()).isAfter(LocalDateTime.now().plusHours(71));
        assertThat(invitation.getDateExpiration()).isBefore(LocalDateTime.now().plusHours(73));
        assertThat(invitation.isAcceptee()).isFalse();
    }

    @Test
    void inviterConjointRefuseUnEmailDejaAssocie() {
        InvitationConjointRequest request = new InvitationConjointRequest();
        request.setEmail("mari@example.com");
        when(utilisateurRepository.existsByEmailIgnoreCase("mari@example.com")).thenReturn(true);

        assertThatThrownBy(() -> helper.inviterConjoint(femme.getEmail(), request))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("existe déjà");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void inviterConjointRefuseUnSecondConjointPourLeDossier() {
        InvitationConjointRequest request = new InvitationConjointRequest();
        request.setEmail("mari@example.com");
        when(utilisateurRepository.findByFemmeAssocieeId(7L)).thenReturn(Optional.of(new Utilisateur()));

        assertThatThrownBy(() -> helper.inviterConjoint(femme.getEmail(), request))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("déjà associé");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void aAccesAuDossierAutoriseSeulementLaFemmeEtSonConjointLie() {
        Utilisateur conjointLie = new Utilisateur();
        conjointLie.setId(8L);
        conjointLie.setRole(RoleUtilisateur.CONJOINT);
        conjointLie.setFemmeAssociee(femme);
        Utilisateur conjointNonLie = new Utilisateur();
        conjointNonLie.setId(9L);
        conjointNonLie.setRole(RoleUtilisateur.CONJOINT);

        assertThat(helper.aAccesAuDossier(femme, femme)).isTrue();
        assertThat(helper.aAccesAuDossier(conjointLie, femme)).isTrue();
        assertThat(helper.aAccesAuDossier(conjointNonLie, femme)).isFalse();
    }
}
