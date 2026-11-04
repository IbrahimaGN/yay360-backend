package sn.yay360.api.helper;

import sn.yay360.api.dto.AccepterInvitationConjointRequest;
import sn.yay360.api.dto.ChangePasswordRequest;
import sn.yay360.api.dto.ForgotPasswordRequest;
import sn.yay360.api.dto.LoginRequest;
import sn.yay360.api.dto.LoginResponse;
import sn.yay360.api.dto.RegisterRequest;
import sn.yay360.api.dto.ResetPasswordRequest;
import sn.yay360.api.exception.AuthentificationException;
import sn.yay360.api.exception.EmailDejaUtiliseException;
import sn.yay360.api.mapper.UtilisateurMapper;
import sn.yay360.api.model.InvitationConjoint;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.InvitationConjointRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import sn.yay360.api.security.JetonSecurise;
import sn.yay360.api.security.JwtUtil;
import sn.yay360.api.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthHelperTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private InvitationConjointRepository invitationConjointRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private EmailService emailService;

    private AuthHelper authHelper;

    @BeforeEach
    void setUp() {
        authHelper = new AuthHelper(utilisateurRepository, invitationConjointRepository, passwordEncoder,
                jwtUtil, new UtilisateurMapper(), emailService);
    }

    // ==================== INSCRIPTION ====================

    private RegisterRequest inscriptionValide() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("Awa.Diop@yay360.sn ");
        request.setNom("Diop");
        request.setPrenom("Awa");
        request.setPassword("motdepasse123");
        request.setTelephone("+221771234567");
        request.setDateNaissance(LocalDate.of(2000, 1, 1));
        request.setRole(RoleUtilisateur.FEMME_ENCEINTE);
        return request;
    }

    @Test
    void inscrire_creeUneFemmeEnceinteAvecEmailNormaliseEtMotDePasseHache() {
        when(utilisateurRepository.existsByEmailIgnoreCase("awa.diop@yay360.sn")).thenReturn(false);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("hash-encode");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.genererToken(eq(1L), eq("awa.diop@yay360.sn"), eq(RoleUtilisateur.FEMME_ENCEINTE), any())).thenReturn("jwt");

        LoginResponse response = authHelper.inscrire(inscriptionValide());

        assertThat(response.getToken()).isEqualTo("jwt");
        assertThat(response.getEmail()).isEqualTo("awa.diop@yay360.sn");
        assertThat(response.getRole()).isEqualTo(RoleUtilisateur.FEMME_ENCEINTE);
        verify(utilisateurRepository).save(argThat(u -> "hash-encode".equals(u.getMotDePasse())));
    }

    @Test
    void inscrire_refuseLesRolesProfessionnelsEtAdmin() {
        for (String role : new String[]{RoleUtilisateur.ADMIN, RoleUtilisateur.MEDECIN, RoleUtilisateur.SAGE_FEMME}) {
            RegisterRequest request = inscriptionValide();
            request.setRole(role);

            assertThatThrownBy(() -> authHelper.inscrire(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("réservée aux femmes enceintes");
        }
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void inscrire_sansRoleCreeUneFemmeEnceinte() {
        RegisterRequest request = inscriptionValide();
        request.setRole(null);
        when(utilisateurRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authHelper.inscrire(request);

        verify(utilisateurRepository).save(argThat(u -> RoleUtilisateur.FEMME_ENCEINTE.equals(u.getRole())));
    }

    @Test
    void inscrire_echoueSiEmailDejaUtilise() {
        when(utilisateurRepository.existsByEmailIgnoreCase("awa.diop@yay360.sn")).thenReturn(true);

        assertThatThrownBy(() -> authHelper.inscrire(inscriptionValide()))
                .isInstanceOf(EmailDejaUtiliseException.class);

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void inscrire_echoueSiFemmeEnceinteAMoinsDe13Ans() {
        RegisterRequest request = inscriptionValide();
        request.setDateNaissance(LocalDate.now().minusYears(10));
        when(utilisateurRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authHelper.inscrire(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("13 ans");

        verify(utilisateurRepository, never()).save(any());
    }

    // ==================== CONNEXION ====================

    private Utilisateur compte(boolean actif) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(5L);
        utilisateur.setEmail("awa@yay360.sn");
        utilisateur.setMotDePasse("hash");
        utilisateur.setRole(RoleUtilisateur.FEMME_ENCEINTE);
        utilisateur.setActif(actif);
        return utilisateur;
    }

    private LoginRequest login(String motDePasse) {
        LoginRequest request = new LoginRequest();
        request.setEmail("AWA@yay360.sn");
        request.setPassword(motDePasse);
        return request;
    }

    @Test
    void connecter_reussitAvecLesBonsIdentifiantsEtRemetLesEchecsAZero() {
        Utilisateur utilisateur = compte(true);
        utilisateur.setTentativesConnexionEchouees(3);
        when(utilisateurRepository.findPourConnexion("awa@yay360.sn")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("bon-mdp1", "hash")).thenReturn(true);
        when(jwtUtil.genererToken(eq(5L), eq("awa@yay360.sn"), eq(RoleUtilisateur.FEMME_ENCEINTE), any())).thenReturn("jwt");

        LoginResponse response = authHelper.connecter(login("bon-mdp1"));

        assertThat(response.getToken()).isEqualTo("jwt");
        assertThat(utilisateur.getTentativesConnexionEchouees()).isZero();
    }

    @Test
    void connecter_mauvaisMotDePasseDonneUnMessageGeneriqueEtCompteLEchec() {
        Utilisateur utilisateur = compte(true);
        when(utilisateurRepository.findPourConnexion("awa@yay360.sn")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("faux", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authHelper.connecter(login("faux")))
                .isInstanceOf(AuthentificationException.class)
                .hasMessage(AuthHelper.IDENTIFIANTS_INCORRECTS);

        assertThat(utilisateur.getTentativesConnexionEchouees()).isEqualTo(1);
        verify(jwtUtil, never()).genererToken(any(), any(), any(), any());
    }

    @Test
    void connecter_emailInconnuDonneLeMemeMessageQuUnMauvaisMotDePasse() {
        when(utilisateurRepository.findPourConnexion("awa@yay360.sn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authHelper.connecter(login("x")))
                .isInstanceOf(AuthentificationException.class)
                .hasMessage(AuthHelper.IDENTIFIANTS_INCORRECTS);
    }

    @Test
    void connecter_compteDesactiveNeLeRevelePasSansLeBonMotDePasse() {
        Utilisateur utilisateur = compte(false);
        when(utilisateurRepository.findPourConnexion("awa@yay360.sn")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("faux", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authHelper.connecter(login("faux")))
                .hasMessage(AuthHelper.IDENTIFIANTS_INCORRECTS);
    }

    @Test
    void connecter_compteDesactiveAvecLeBonMotDePasseEstRefuse() {
        Utilisateur utilisateur = compte(false);
        when(utilisateurRepository.findPourConnexion("awa@yay360.sn")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("bon-mdp1", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authHelper.connecter(login("bon-mdp1")))
                .isInstanceOf(AuthentificationException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void connecter_verrouilleLeCompteApres5EchecsPuisRefuseMemeLeBonMotDePasse() {
        Utilisateur utilisateur = compte(true);
        utilisateur.setTentativesConnexionEchouees(4);
        when(utilisateurRepository.findPourConnexion("awa@yay360.sn")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("faux", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authHelper.connecter(login("faux")))
                .isInstanceOf(AuthentificationException.class);
        assertThat(utilisateur.getVerrouilleJusqua()).isAfter(LocalDateTime.now().plusMinutes(14));

        assertThatThrownBy(() -> authHelper.connecter(login("bon-mdp1")))
                .isInstanceOf(AuthentificationException.class)
                .hasMessage(AuthHelper.IDENTIFIANTS_INCORRECTS); // verrouillage non révélé
        verify(passwordEncoder, never()).matches(eq("bon-mdp1"), eq("hash"));
    }

    // ==================== MOT DE PASSE OUBLIÉ / RÉINITIALISATION ====================

    @Test
    void motDePasseOublie_emailInconnuNeLevePasDErreurEtNEnvoieRien() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("inconnu@yay360.sn");
        when(utilisateurRepository.findByEmailIgnoreCase("inconnu@yay360.sn")).thenReturn(Optional.empty());

        authHelper.motDePasseOublie(request);

        verifyNoInteractions(emailService);
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void motDePasseOublie_stockeUneEmpreinteEtEnvoieLeJetonEnClairParEmail() {
        Utilisateur utilisateur = compte(true);
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("awa@yay360.sn");
        when(utilisateurRepository.findByEmailIgnoreCase("awa@yay360.sn")).thenReturn(Optional.of(utilisateur));

        authHelper.motDePasseOublie(request);

        ArgumentCaptor<String> lien = ArgumentCaptor.forClass(String.class);
        verify(emailService).envoyer(eq("awa@yay360.sn"), anyString(), anyString(), lien.capture());
        String jeton = lien.getValue().substring(lien.getValue().indexOf("token=") + 6);
        assertThat(utilisateur.getResetToken()).isEqualTo(JetonSecurise.hacher(jeton));
        assertThat(utilisateur.getResetTokenExpiration()).isAfter(LocalDateTime.now().plusMinutes(59));
    }

    @Test
    void reinitialiserMotDePasse_consommeLeJetonEtInvalideLesAnciensJwt() {
        Utilisateur utilisateur = compte(true);
        utilisateur.setResetToken(JetonSecurise.hacher("jeton-clair"));
        utilisateur.setResetTokenExpiration(LocalDateTime.now().plusMinutes(30));
        when(utilisateurRepository.findByResetToken(JetonSecurise.hacher("jeton-clair")))
                .thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.encode("nouveau-mdp1")).thenReturn("nouveau-hash");
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("jeton-clair");
        request.setNouveauMotDePasse("nouveau-mdp1");

        authHelper.reinitialiserMotDePasse(request);

        assertThat(utilisateur.getMotDePasse()).isEqualTo("nouveau-hash");
        assertThat(utilisateur.getResetToken()).isNull();
        assertThat(utilisateur.getDateChangementMotDePasse()).isNotNull();
        assertThat(utilisateur.getVersionJeton()).isEqualTo(1); // anciens jetons révoqués
    }

    @Test
    void reinitialiserMotDePasse_refuseUnJetonExpire() {
        Utilisateur utilisateur = compte(true);
        utilisateur.setResetToken(JetonSecurise.hacher("vieux"));
        utilisateur.setResetTokenExpiration(LocalDateTime.now().minusMinutes(1));
        when(utilisateurRepository.findByResetToken(JetonSecurise.hacher("vieux"))).thenReturn(Optional.of(utilisateur));
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("vieux");
        request.setNouveauMotDePasse("nouveau-mdp1");

        assertThatThrownBy(() -> authHelper.reinitialiserMotDePasse(request))
                .isInstanceOf(AuthentificationException.class)
                .hasMessageContaining("expiré");
        assertThat(utilisateur.getResetToken()).isNull();
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changerMotDePasse_refuseUnAncienMotDePasseIncorrect() {
        Utilisateur utilisateur = compte(true);
        when(utilisateurRepository.findByEmailIgnoreCase("awa@yay360.sn")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("faux", "hash")).thenReturn(false);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setAncienMotDePasse("faux");
        request.setNouveauMotDePasse("nouveau-mdp1");

        assertThatThrownBy(() -> authHelper.changerMotDePasse("awa@yay360.sn", request))
                .isInstanceOf(AuthentificationException.class);
        verify(utilisateurRepository, never()).save(any());
    }

    // ==================== INVITATION DU CONJOINT ====================

    @Test
    void accepterInvitation_creeUnCompteConjointLieAuDossier() {
        Utilisateur femme = new Utilisateur();
        femme.setId(42L);
        InvitationConjoint invitation = new InvitationConjoint();
        invitation.setFemme(femme);
        invitation.setEmail("mari@example.com");
        invitation.setDateExpiration(LocalDateTime.now().plusHours(1));

        AccepterInvitationConjointRequest request = new AccepterInvitationConjointRequest();
        request.setToken("code-invitation");
        request.setEmail("MARI@example.com");
        request.setNom("Diop");
        request.setPrenom("Moussa");
        request.setPassword("secret123");
        request.setTelephone("+221771234567");
        request.setDateNaissance(LocalDate.of(1990, 1, 1));

        when(invitationConjointRepository.findByTokenAndAccepteeFalse(JetonSecurise.hacher("code-invitation")))
                .thenReturn(Optional.of(invitation));
        when(utilisateurRepository.existsByEmailIgnoreCase("mari@example.com")).thenReturn(false);
        when(utilisateurRepository.findByFemmeAssocieeId(42L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur conjoint = invocation.getArgument(0);
            conjoint.setId(43L);
            return conjoint;
        });
        when(jwtUtil.genererToken(eq(43L), eq("mari@example.com"), eq(RoleUtilisateur.CONJOINT), any())).thenReturn("conjoint-jwt");

        LoginResponse response = authHelper.accepterInvitationConjoint(request);

        assertThat(response.getToken()).isEqualTo("conjoint-jwt");
        assertThat(response.getRole()).isEqualTo(RoleUtilisateur.CONJOINT);
        verify(utilisateurRepository).save(argThat(conjoint ->
                RoleUtilisateur.CONJOINT.equals(conjoint.getRole())
                        && conjoint.getFemmeAssociee().getId().equals(42L)
                        && "mari@example.com".equals(conjoint.getEmail())));
        assertThat(invitation.isAcceptee()).isTrue();
        verify(invitationConjointRepository).save(invitation);
    }

    @Test
    void accepterInvitation_refuseUneInvitationExpiree() {
        InvitationConjoint invitation = new InvitationConjoint();
        invitation.setEmail("mari@example.com");
        invitation.setDateExpiration(LocalDateTime.now().minusMinutes(1));
        AccepterInvitationConjointRequest request = new AccepterInvitationConjointRequest();
        request.setToken("expire");
        request.setEmail("mari@example.com");

        when(invitationConjointRepository.findByTokenAndAccepteeFalse(JetonSecurise.hacher("expire")))
                .thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> authHelper.accepterInvitationConjoint(request))
                .isInstanceOf(AuthentificationException.class)
                .hasMessageContaining("expirée");

        verify(utilisateurRepository, never()).save(any());
    }
}
