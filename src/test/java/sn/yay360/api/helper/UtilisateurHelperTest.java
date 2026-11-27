package sn.yay360.api.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.yay360.api.dto.CreationProfessionnelResponse;
import sn.yay360.api.dto.CreerProfessionnelRequest;
import sn.yay360.api.exception.EmailDejaUtiliseException;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.mapper.UtilisateurMapper;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import sn.yay360.api.service.EmailService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UtilisateurHelperTest {

    private static final String EMAIL_ADMIN = "admin@yay360.sn";
    private static final String EMAIL_ADMIN_HPD = "admin.hpd@yay360.sn";

    private UtilisateurRepository utilisateurRepository;
    private EtablissementSanteRepository etablissementRepository;
    private PasswordEncoder passwordEncoder;
    private UtilisateurHelper helper;
    private EtablissementSante hopitalPrincipal;
    private EtablissementSante roiBaudouin;

    @BeforeEach
    void setUp() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        etablissementRepository = mock(EtablissementSanteRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        PerimetreAdminHelper perimetre = new PerimetreAdminHelper(utilisateurRepository, etablissementRepository);
        helper = new UtilisateurHelper(utilisateurRepository, etablissementRepository, new UtilisateurMapper(),
                passwordEncoder, mock(EmailService.class), perimetre);
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("hash");

        hopitalPrincipal = etablissement(1L, "Hôpital Principal de Dakar");
        roiBaudouin = etablissement(2L, "Centre de Santé Roi Baudouin");
        compte(10L, EMAIL_ADMIN, RoleUtilisateur.ADMIN, null);
        compte(11L, EMAIL_ADMIN_HPD, RoleUtilisateur.ADMIN_ETABLISSEMENT, hopitalPrincipal);
    }

    private static EtablissementSante etablissement(Long id, String nom) {
        EtablissementSante e = new EtablissementSante();
        e.setId(id);
        e.setNom(nom);
        return e;
    }

    private Utilisateur compte(Long id, String email, String role, EtablissementSante administre) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        u.setEtablissementAdministre(administre);
        when(utilisateurRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(u));
        when(utilisateurRepository.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private CreerProfessionnelRequest demande(String role) {
        CreerProfessionnelRequest request = new CreerProfessionnelRequest();
        request.setNom("Sarr");
        request.setPrenom("Fatou");
        request.setEmail(" Fatou.Sarr@Hopital.sn");
        request.setTelephone("+221771234567");
        request.setDateNaissance(LocalDate.of(1985, 5, 1));
        request.setRole(role);
        return request;
    }

    // ==================== CRÉATION DES COMPTES ====================

    @Test
    void creerProfessionnel_genereUnMotDePasseTemporaireAChanger() {
        CreationProfessionnelResponse reponse = helper.creerProfessionnel(EMAIL_ADMIN_HPD, demande(RoleUtilisateur.MEDECIN));

        assertThat(reponse.getMotDePasseTemporaire()).hasSizeGreaterThanOrEqualTo(12);
        assertThat(reponse.getUtilisateur().getEmail()).isEqualTo("fatou.sarr@hopital.sn");
        assertThat(reponse.getUtilisateur().getRole()).isEqualTo(RoleUtilisateur.MEDECIN);
        verify(passwordEncoder).encode(reponse.getMotDePasseTemporaire());
        verify(utilisateurRepository).save(argThat(u -> Boolean.TRUE.equals(u.getMotDePasseTemporaire())
                && "hash".equals(u.getMotDePasse())));
    }

    @Test
    void creerProfessionnel_refuseUnSoignantDeMoinsDe21Ans() {
        CreerProfessionnelRequest request = demande(RoleUtilisateur.MEDECIN);
        request.setDateNaissance(LocalDate.now().minusYears(19));

        assertThatThrownBy(() -> helper.creerProfessionnel(EMAIL_ADMIN_HPD, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("21 ans");
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void creerProfessionnel_refuseUnEmailDejaUtilise() {
        when(utilisateurRepository.existsByEmailIgnoreCase("fatou.sarr@hopital.sn")).thenReturn(true);

        assertThatThrownBy(() -> helper.creerProfessionnel(EMAIL_ADMIN_HPD, demande(RoleUtilisateur.MEDECIN)))
                .isInstanceOf(EmailDejaUtiliseException.class);
    }

    @Test
    void creerAdminEtablissement_exigeUnEtablissement() {
        assertThatThrownBy(() -> helper.creerProfessionnel(EMAIL_ADMIN, demande(RoleUtilisateur.ADMIN_ETABLISSEMENT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("établissement est obligatoire");
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void creerAdminEtablissement_leRattacheALEtablissementChoisi() {
        when(etablissementRepository.findById(2L)).thenReturn(Optional.of(roiBaudouin));
        CreerProfessionnelRequest request = demande(RoleUtilisateur.ADMIN_ETABLISSEMENT);
        request.setEtablissementId(2L);

        CreationProfessionnelResponse reponse = helper.creerProfessionnel(EMAIL_ADMIN, request);

        assertThat(reponse.getUtilisateur().getEtablissementAdministreId()).isEqualTo(2L);
        assertThat(reponse.getUtilisateur().getEtablissementAdministreNom()).isEqualTo("Centre de Santé Roi Baudouin");
        assertThat(roiBaudouin.getProfessionnels()).isEmpty();
    }

    @Test
    void lAdministrateurDeLaPlateformeNeCreeAucunSoignant() {
        for (String role : List.of(RoleUtilisateur.MEDECIN, RoleUtilisateur.SAGE_FEMME)) {
            CreerProfessionnelRequest request = demande(role);
            request.setEtablissementId(1L);

            assertThatThrownBy(() -> helper.creerProfessionnel(EMAIL_ADMIN, request))
                    .isInstanceOf(OperationNonAutoriseeException.class)
                    .hasMessageContaining("uniquement des administrateurs");
        }
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void lAdministrateurDeLaPlateformeCreeUnAutreAdministrateurDeLaPlateforme() {
        CreationProfessionnelResponse reponse = helper.creerProfessionnel(EMAIL_ADMIN, demande(RoleUtilisateur.ADMIN));

        assertThat(reponse.getUtilisateur().getRole()).isEqualTo(RoleUtilisateur.ADMIN);
        assertThat(reponse.getUtilisateur().getEtablissementAdministreId()).isNull();
        verify(etablissementRepository, never()).findById(any());
    }

    @Test
    void unAdministrateurDeLaPlateformeNEstRattacheAAucunEtablissement() {
        CreerProfessionnelRequest request = demande(RoleUtilisateur.ADMIN);
        request.setEtablissementId(1L);

        assertThatThrownBy(() -> helper.creerProfessionnel(EMAIL_ADMIN, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void unEtablissementNAQuUnSeulAdministrateur() {
        when(etablissementRepository.findById(1L)).thenReturn(Optional.of(hopitalPrincipal));
        when(utilisateurRepository.existsByEtablissementAdministreId(1L)).thenReturn(true);
        CreerProfessionnelRequest request = demande(RoleUtilisateur.ADMIN_ETABLISSEMENT);
        request.setEtablissementId(1L);

        assertThatThrownBy(() -> helper.creerProfessionnel(EMAIL_ADMIN, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cet établissement a déjà un administrateur");
        verify(utilisateurRepository, never()).save(any());
    }

    // ==================== CRÉATION PAR UN ADMINISTRATEUR D'ÉTABLISSEMENT ====================

    @Test
    void adminEtablissement_creeUnSoignantRattacheDOfficeASonEtablissement() {
        CreerProfessionnelRequest request = demande(RoleUtilisateur.SAGE_FEMME);
        request.setEtablissementId(2L); // tentative d'un autre établissement : ignorée

        CreationProfessionnelResponse reponse = helper.creerProfessionnel(EMAIL_ADMIN_HPD, request);

        assertThat(hopitalPrincipal.getProfessionnels())
                .extracting(Utilisateur::getEmail).containsExactly("fatou.sarr@hopital.sn");
        assertThat(roiBaudouin.getProfessionnels()).isEmpty();
        assertThat(reponse.getUtilisateur().getEtablissementAdministreId()).isNull();
        verify(etablissementRepository, never()).findById(any());
    }

    @Test
    void adminEtablissement_neCreePasDAdministrateur() {
        for (String role : List.of(RoleUtilisateur.ADMIN, RoleUtilisateur.ADMIN_ETABLISSEMENT)) {
            assertThatThrownBy(() -> helper.creerProfessionnel(EMAIL_ADMIN_HPD, demande(role)))
                    .isInstanceOf(OperationNonAutoriseeException.class);
        }
        verify(utilisateurRepository, never()).save(any());
    }

    // ==================== ACTIVATION / DÉSACTIVATION ====================

    @Test
    void adminEtablissement_desactiveUnSoignantQuiNExerceQueChezLui() {
        Utilisateur medecin = compte(20L, "medecin@hpd.sn", RoleUtilisateur.MEDECIN, null);
        medecin.setActif(true);
        when(etablissementRepository.findByProfessionnelsId(20L)).thenReturn(List.of(hopitalPrincipal));

        assertThat(helper.desactiverCompte(20L, EMAIL_ADMIN_HPD).getActif()).isFalse();
    }

    @Test
    void adminEtablissement_neDesactivePasUnSoignantDUnAutreEtablissement() {
        compte(21L, "medecin@rb.sn", RoleUtilisateur.MEDECIN, null);
        when(etablissementRepository.findByProfessionnelsId(21L)).thenReturn(List.of(roiBaudouin));

        assertThatThrownBy(() -> helper.desactiverCompte(21L, EMAIL_ADMIN_HPD))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("n'exerce pas dans votre établissement");
    }

    @Test
    void adminEtablissement_neDesactivePasUnSoignantPartageAvecUnAutreEtablissement() {
        compte(22L, "medecin@deux.sn", RoleUtilisateur.MEDECIN, null);
        when(etablissementRepository.findByProfessionnelsId(22L)).thenReturn(List.of(hopitalPrincipal, roiBaudouin));

        assertThatThrownBy(() -> helper.desactiverCompte(22L, EMAIL_ADMIN_HPD))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("retirez-le");
    }

    @Test
    void adminEtablissement_neTouchePasAUnComptePatiente() {
        compte(23L, "awa@exemple.sn", RoleUtilisateur.FEMME_ENCEINTE, null);

        assertThatThrownBy(() -> helper.activerCompte(23L, EMAIL_ADMIN_HPD))
                .isInstanceOf(OperationNonAutoriseeException.class);
    }

    @Test
    void personneNeDesactiveSonPropreCompte() {
        assertThatThrownBy(() -> helper.desactiverCompte(10L, EMAIL_ADMIN))
                .isInstanceOf(OperationNonAutoriseeException.class);
    }

    @Test
    void adminEtablissement_listeLesSoignantsDeSonEtablissementParOrdreAlphabetique() {
        Utilisateur sow = compte(30L, "sow@hpd.sn", RoleUtilisateur.MEDECIN, null);
        sow.setNom("Sow");
        sow.setPrenom("Aminata");
        Utilisateur diallo = compte(31L, "diallo@hpd.sn", RoleUtilisateur.MEDECIN, null);
        diallo.setNom("Diallo");
        diallo.setPrenom("Mamadou");
        hopitalPrincipal.getProfessionnels().addAll(List.of(sow, diallo));

        assertThat(helper.listerSoignantsDeMonEtablissement(EMAIL_ADMIN_HPD))
                .extracting(u -> u.getNom()).containsExactly("Diallo", "Sow");
    }

    @Test
    void creerAdminEtablissement_refuseUnEtablissementInconnu() {
        CreerProfessionnelRequest request = demande(RoleUtilisateur.ADMIN_ETABLISSEMENT);
        request.setEtablissementId(99L);

        assertThatThrownBy(() -> helper.creerProfessionnel(EMAIL_ADMIN, request))
                .isInstanceOf(sn.yay360.api.exception.ResourceNotFoundException.class);
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void lAdministrateurDeLaPlateformeReactiveToutCompte() {
        Utilisateur patiente = compte(24L, "khady@exemple.sn", RoleUtilisateur.FEMME_ENCEINTE, null);
        patiente.setActif(false);

        assertThat(helper.activerCompte(24L, EMAIL_ADMIN).getActif()).isTrue();
        verify(etablissementRepository, never()).findByProfessionnelsId(any());
    }

    @Test
    void adminEtablissement_reactiveUnSoignantQuiNExerceQueChezLui() {
        Utilisateur sageFemme = compte(25L, "sf@hpd.sn", RoleUtilisateur.SAGE_FEMME, null);
        sageFemme.setActif(false);
        when(etablissementRepository.findByProfessionnelsId(25L)).thenReturn(List.of(hopitalPrincipal));

        assertThat(helper.activerCompte(25L, EMAIL_ADMIN_HPD).getActif()).isTrue();
    }

    @Test
    void listerParRole_refuseUnRoleInconnu() {
        assertThatThrownBy(() -> helper.listerParRole("PIRATE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
