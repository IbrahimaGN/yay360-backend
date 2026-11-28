package sn.yay360.api.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.yay360.api.dto.CreationProfessionnelResponse;
import sn.yay360.api.dto.CreerProfessionnelRequest;
import sn.yay360.api.exception.EmailDejaUtiliseException;
import sn.yay360.api.mapper.UtilisateurMapper;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.UtilisateurRepository;
import sn.yay360.api.service.EmailService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UtilisateurHelperTest {

    private UtilisateurRepository utilisateurRepository;
    private PasswordEncoder passwordEncoder;
    private UtilisateurHelper helper;

    @BeforeEach
    void setUp() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        helper = new UtilisateurHelper(utilisateurRepository, new UtilisateurMapper(), passwordEncoder,
                mock(EmailService.class));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("hash");
    }

    private CreerProfessionnelRequest medecin() {
        CreerProfessionnelRequest request = new CreerProfessionnelRequest();
        request.setNom("Sarr");
        request.setPrenom("Fatou");
        request.setEmail(" Fatou.Sarr@Hopital.sn");
        request.setTelephone("+221771234567");
        request.setDateNaissance(LocalDate.of(1985, 5, 1));
        request.setRole(RoleUtilisateur.MEDECIN);
        return request;
    }

    @Test
    void creerProfessionnel_genereUnMotDePasseTemporaireAChanger() {
        CreationProfessionnelResponse reponse = helper.creerProfessionnel(medecin());

        assertThat(reponse.getMotDePasseTemporaire()).hasSizeGreaterThanOrEqualTo(12);
        assertThat(reponse.getUtilisateur().getEmail()).isEqualTo("fatou.sarr@hopital.sn");
        assertThat(reponse.getUtilisateur().getRole()).isEqualTo(RoleUtilisateur.MEDECIN);
        verify(passwordEncoder).encode(reponse.getMotDePasseTemporaire());
        verify(utilisateurRepository).save(argThat(u -> Boolean.TRUE.equals(u.getMotDePasseTemporaire())
                && "hash".equals(u.getMotDePasse())));
    }

    @Test
    void creerProfessionnel_refuseUnSoignantDeMoinsDe21Ans() {
        CreerProfessionnelRequest request = medecin();
        request.setDateNaissance(LocalDate.now().minusYears(19));

        assertThatThrownBy(() -> helper.creerProfessionnel(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("21 ans");
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void creerProfessionnel_refuseUnEmailDejaUtilise() {
        when(utilisateurRepository.existsByEmailIgnoreCase("fatou.sarr@hopital.sn")).thenReturn(true);

        assertThatThrownBy(() -> helper.creerProfessionnel(medecin()))
                .isInstanceOf(EmailDejaUtiliseException.class);
    }

    @Test
    void listerParRole_refuseUnRoleInconnu() {
        assertThatThrownBy(() -> helper.listerParRole("PIRATE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
