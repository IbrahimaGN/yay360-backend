package sn.yay360.api.helper;

import sn.yay360.api.dto.LoginRequest;
import sn.yay360.api.dto.LoginResponse;
import sn.yay360.api.dto.RegisterRequest;
import sn.yay360.api.dto.AccepterInvitationConjointRequest;
import sn.yay360.api.exception.AuthentificationException;
import sn.yay360.api.exception.EmailDejaUtiliseException;
import sn.yay360.api.mapper.UtilisateurMapper;
import sn.yay360.api.model.InvitationConjoint;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.UtilisateurRepository;
import sn.yay360.api.repo.InvitationConjointRepository;
import sn.yay360.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;

    private UtilisateurMapper utilisateurMapper;
    private AuthHelper authHelper;

    @BeforeEach
    void setUp() {
        utilisateurMapper = new UtilisateurMapper();
        authHelper = new AuthHelper(utilisateurRepository, invitationConjointRepository, passwordEncoder,
                authenticationManager, jwtUtil, utilisateurMapper);
    }

    @Test
    void inscrire_devraitEchouerSiEmailDejaUtilise() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existe@yay360.sn");
        request.setNom("Diop");
        request.setPrenom("Awa");
        request.setPassword("motdepasse123");
        request.setRole(RoleUtilisateur.FEMME_ENCEINTE);

        when(utilisateurRepository.existsByEmail("existe@yay360.sn")).thenReturn(true);

        assertThatThrownBy(() -> authHelper.inscrire(request))
                .isInstanceOf(EmailDejaUtiliseException.class);

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void inscrire_devraitCreerUnUtilisateurEtRetournerUnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("awa.diop@yay360.sn");
        request.setNom("Diop");
        request.setPrenom("Awa");
        request.setPassword("motdepasse123");
        request.setTelephone("+221771234567");
        request.setDateNaissance(java.time.LocalDate.of(2000, 1, 1));
        request.setRole(RoleUtilisateur.FEMME_ENCEINTE);

        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-encode");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.genererToken(anyLong(), anyString(), anyString())).thenReturn("fake-jwt-token");

        LoginResponse response = authHelper.inscrire(request);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(response.getEmail()).isEqualTo("awa.diop@yay360.sn");
        assertThat(response.getRole()).isEqualTo(RoleUtilisateur.FEMME_ENCEINTE);
    }

    @Test
    void connecter_devraitEchouerSiCompteDesactive() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("inactif@yay360.sn");
        utilisateur.setActif(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("inactif@yay360.sn");
        request.setPassword("motdepasse123");

        when(utilisateurRepository.findByEmail("inactif@yay360.sn")).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> authHelper.connecter(request))
                .isInstanceOf(AuthentificationException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void connecter_devraitEchouerSiUtilisateurIntrouvable() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inconnu@yay360.sn");
        request.setPassword("motdepasse123");

        when(utilisateurRepository.findByEmail("inconnu@yay360.sn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authHelper.connecter(request))
                .isInstanceOf(AuthentificationException.class);
    }

    @Test
    void accepterInvitationCreeUnCompteConjointLieAuDossier() {
        Utilisateur femme = new Utilisateur();
        femme.setId(42L);
        InvitationConjoint invitation = new InvitationConjoint();
        invitation.setFemme(femme);
        invitation.setEmail("mari@example.com");
        invitation.setDateExpiration(LocalDateTime.now().plusHours(1));

        AccepterInvitationConjointRequest request = new AccepterInvitationConjointRequest();
        request.setToken("invitation-token");
        request.setEmail("MARI@example.com");
        request.setNom("Diop");
        request.setPrenom("Moussa");
        request.setPassword("secret123");
        request.setTelephone("+221771234567");
        request.setDateNaissance(LocalDate.of(1990, 1, 1));

        when(invitationConjointRepository.findByTokenAndAccepteeFalse("invitation-token"))
                .thenReturn(Optional.of(invitation));
        when(utilisateurRepository.existsByEmailIgnoreCase("mari@example.com")).thenReturn(false);
        when(utilisateurRepository.findByFemmeAssocieeId(42L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur conjoint = invocation.getArgument(0);
            conjoint.setId(43L);
            return conjoint;
        });
        when(jwtUtil.genererToken(43L, "mari@example.com", RoleUtilisateur.CONJOINT)).thenReturn("conjoint-jwt");

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
    void accepterInvitationRefuseUneInvitationExpiree() {
        InvitationConjoint invitation = new InvitationConjoint();
        invitation.setEmail("mari@example.com");
        invitation.setDateExpiration(LocalDateTime.now().minusMinutes(1));
        AccepterInvitationConjointRequest request = new AccepterInvitationConjointRequest();
        request.setToken("expired-token");
        request.setEmail("mari@example.com");

        when(invitationConjointRepository.findByTokenAndAccepteeFalse("expired-token"))
                .thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> authHelper.accepterInvitationConjoint(request))
                .isInstanceOf(AuthentificationException.class)
                .hasMessageContaining("expirée");

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void inscrire_devraitEchouerSiFemmeEnceinteAMoinsDe13Ans() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("trop.jeune@yay360.sn");
        request.setNom("Diop");
        request.setPrenom("Awa");
        request.setPassword("motdepasse123");
        request.setTelephone("+221771234567");
        request.setDateNaissance(java.time.LocalDate.now().minusYears(10));
        request.setRole(RoleUtilisateur.FEMME_ENCEINTE);

        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authHelper.inscrire(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("13 ans");

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void inscrire_devraitEchouerSiMedecinAMoinsDe21Ans() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("jeune.medecin@yay360.sn");
        request.setNom("Sarr");
        request.setPrenom("Moussa");
        request.setPassword("motdepasse123");
        request.setTelephone("+221781234567");
        request.setDateNaissance(java.time.LocalDate.now().minusYears(18));
        request.setRole(RoleUtilisateur.MEDECIN);

        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authHelper.inscrire(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("21 ans");

        verify(utilisateurRepository, never()).save(any());
    }
}
