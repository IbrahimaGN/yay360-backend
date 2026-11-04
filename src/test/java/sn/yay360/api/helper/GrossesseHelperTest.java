package sn.yay360.api.helper;

import sn.yay360.api.dto.DeclarerGrossesseRequest;
import sn.yay360.api.dto.GrossesseDto;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.mapper.GrossesseMapper;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import sn.yay360.api.repo.InvitationConjointRepository;
import sn.yay360.api.dto.RattacherGrossesseRequest;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.repo.RendezVousRepository;
import java.util.List;
import sn.yay360.api.dto.DeclarerRisqueRequest;
import sn.yay360.api.dto.TerminerGrossesseRequest;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.repo.EtablissementSanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrossesseHelperTest {

    @Mock
    private GrossesseRepository grossesseRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private InvitationConjointRepository invitationConjointRepository;
    @Mock
    private EtablissementSanteRepository etablissementRepository;
    @Mock
    private NotificationHelper notificationHelper;
    @Mock
    private RendezVousRepository rendezVousRepository;

    private GrossesseMapper grossesseMapper;
    private CalculObstetricalHelper calcul;
    private GrossesseHelper grossesseHelper;

    private Utilisateur femme;

    @BeforeEach
    void setUp() {
        grossesseMapper = new GrossesseMapper();
        calcul = new CalculObstetricalHelper();
        DossierFamilialHelper dossierFamilialHelper =
                new DossierFamilialHelper(utilisateurRepository, invitationConjointRepository,
                        mock(sn.yay360.api.service.EmailService.class));
        AccesDossierHelper acces = new AccesDossierHelper(
                utilisateurRepository, grossesseRepository, etablissementRepository, dossierFamilialHelper);
        grossesseHelper = new GrossesseHelper(grossesseRepository, utilisateurRepository, etablissementRepository,
                rendezVousRepository, grossesseMapper, calcul, dossierFamilialHelper, acces, notificationHelper);

        femme = new Utilisateur();
        femme.setId(1L);
        femme.setNom("Ndiaye");
        femme.setPrenom("Fatou");
        femme.setEmail("fatou.ndiaye@yay360.sn");
        femme.setRole(RoleUtilisateur.FEMME_ENCEINTE);
        femme.setTelephone("+221770000001");
    }

    @Test
    void declarer_devraitEchouerSiGrossesseActiveExisteDeja() {
        DeclarerGrossesseRequest request = new DeclarerGrossesseRequest();
        request.setDateDernieresRegles(LocalDate.now().minusWeeks(5));

        when(utilisateurRepository.findByEmailIgnoreCase(femme.getEmail())).thenReturn(Optional.of(femme));
        when(grossesseRepository.findByFemmeEnceinteIdAndStatut(femme.getId(), StatutGrossesse.EN_COURS))
                .thenReturn(Optional.of(new Grossesse()));

        assertThatThrownBy(() -> grossesseHelper.declarer(femme.getEmail(), request))
                .isInstanceOf(OperationNonAutoriseeException.class);

        verify(grossesseRepository, never()).save(any());
    }

    @Test
    void declarer_devraitCalculerLaDpaAutomatiquement() {
        LocalDate ddr = LocalDate.now().minusWeeks(10);
        DeclarerGrossesseRequest request = new DeclarerGrossesseRequest();
        request.setDateDernieresRegles(ddr);

        when(utilisateurRepository.findByEmailIgnoreCase(femme.getEmail())).thenReturn(Optional.of(femme));
        when(grossesseRepository.findByFemmeEnceinteIdAndStatut(femme.getId(), StatutGrossesse.EN_COURS))
                .thenReturn(Optional.empty());
        when(grossesseRepository.save(any(Grossesse.class))).thenAnswer(invocation -> {
            Grossesse g = invocation.getArgument(0);
            g.setId(100L);
            return g;
        });

        GrossesseDto dto = grossesseHelper.declarer(femme.getEmail(), request);

        assertThat(dto.getDatePrevueAccouchement()).isEqualTo(ddr.plusDays(280));
        assertThat(dto.getSemaineActuelle()).isEqualTo(11); // 10 semaines révolues -> semaine 11
        assertThat(dto.getTrimestre()).isEqualTo("T1");
        assertThat(dto.getStatut()).isEqualTo(StatutGrossesse.EN_COURS);
    }

    @Test
    void declarer_devraitEchouerSiDateDernieresReglesTropAncienne() {
        DeclarerGrossesseRequest request = new DeclarerGrossesseRequest();
        request.setDateDernieresRegles(LocalDate.now().minusWeeks(50));

        when(utilisateurRepository.findByEmailIgnoreCase(femme.getEmail())).thenReturn(Optional.of(femme));
        when(grossesseRepository.findByFemmeEnceinteIdAndStatut(femme.getId(), StatutGrossesse.EN_COURS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> grossesseHelper.declarer(femme.getEmail(), request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== CLOISONNEMENT SOIGNANT ↔ PATIENTE ====================

    private Utilisateur medecin() {
        Utilisateur medecin = new Utilisateur();
        medecin.setId(50L);
        medecin.setEmail("dr@yay360.sn");
        medecin.setRole(RoleUtilisateur.MEDECIN);
        medecin.setActif(true);
        when(utilisateurRepository.findByEmailIgnoreCase("dr@yay360.sn")).thenReturn(Optional.of(medecin));
        return medecin;
    }

    private Grossesse grossesseEnCours() {
        Grossesse grossesse = new Grossesse();
        grossesse.setId(10L);
        grossesse.setFemmeEnceinte(femme);
        grossesse.setDateDernieresRegles(LocalDate.now().minusWeeks(20));
        grossesse.setDatePrevueAccouchement(LocalDate.now().minusWeeks(20).plusDays(280));
        grossesse.setStatut(StatutGrossesse.EN_COURS);
        grossesse.setGrossesseARisque(false);
        when(grossesseRepository.findById(10L)).thenReturn(Optional.of(grossesse));
        return grossesse;
    }

    @Test
    void obtenirParId_refuseUnSoignantQuiNeSuitPasLaPatiente() {
        medecin();
        grossesseEnCours();
        when(grossesseRepository.estSuivieParProfessionnel(10L, 50L)).thenReturn(false);

        assertThatThrownBy(() -> grossesseHelper.obtenirParId("dr@yay360.sn", 10L))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("ne suivez pas");
    }

    @Test
    void obtenirParId_autoriseLeSoignantQuiSuitLaPatiente() {
        medecin();
        grossesseEnCours();
        when(grossesseRepository.estSuivieParProfessionnel(10L, 50L)).thenReturn(true);

        assertThat(grossesseHelper.obtenirParId("dr@yay360.sn", 10L).getId()).isEqualTo(10L);
    }

    @Test
    void declarerARisque_refuseUneGrossesseCloturee() {
        medecin();
        Grossesse grossesse = grossesseEnCours();
        grossesse.setStatut(StatutGrossesse.TERMINEE);
        when(grossesseRepository.estSuivieParProfessionnel(10L, 50L)).thenReturn(true);
        DeclarerRisqueRequest request = new DeclarerRisqueRequest();
        request.setMotif("Prééclampsie");

        assertThatThrownBy(() -> grossesseHelper.declarerARisque("dr@yay360.sn", 10L, request))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("clôturée");
        verify(grossesseRepository, never()).save(any());
    }

    @Test
    void declarerARisque_marqueLaGrossesseEtPrevientLaPatiente() {
        medecin();
        grossesseEnCours();
        when(grossesseRepository.estSuivieParProfessionnel(10L, 50L)).thenReturn(true);
        when(grossesseRepository.save(any(Grossesse.class))).thenAnswer(i -> i.getArgument(0));
        DeclarerRisqueRequest request = new DeclarerRisqueRequest();
        request.setMotif("Hypertension gravidique");

        GrossesseDto dto = grossesseHelper.declarerARisque("dr@yay360.sn", 10L, request);

        assertThat(dto.getGrossesseARisque()).isTrue();
        assertThat(dto.getMotifRisque()).isEqualTo("Hypertension gravidique");
        verify(notificationHelper).envoyer(eq(femme), any(), anyString(), anyString(), anyString());
    }

    @Test
    void terminer_enregistreLaDateDeFinEtRefuseUneDateAvantLesDernieresRegles() {
        medecin();
        Grossesse grossesse = grossesseEnCours();
        when(grossesseRepository.estSuivieParProfessionnel(10L, 50L)).thenReturn(true);
        TerminerGrossesseRequest incoherente = new TerminerGrossesseRequest();
        incoherente.setStatut(StatutGrossesse.TERMINEE);
        incoherente.setDateFin(LocalDate.now().minusWeeks(30));

        assertThatThrownBy(() -> grossesseHelper.terminer("dr@yay360.sn", 10L, incoherente))
                .isInstanceOf(IllegalArgumentException.class);

        when(grossesseRepository.save(any(Grossesse.class))).thenAnswer(i -> i.getArgument(0));
        TerminerGrossesseRequest request = new TerminerGrossesseRequest();
        request.setStatut(StatutGrossesse.TERMINEE);
        grossesseHelper.terminer("dr@yay360.sn", 10L, request);

        assertThat(grossesse.getStatut()).isEqualTo(StatutGrossesse.TERMINEE);
        assertThat(grossesse.getDateFin()).isEqualTo(LocalDate.now());
    }

    @Test
    void rattacher_refuseUnEtablissementDontLeSoignantNEstPasMembre() {
        medecin();
        grossesseEnCours();
        when(etablissementRepository.existsByIdAndProfessionnelsId(3L, 50L)).thenReturn(false);
        assertThatThrownBy(() -> grossesseHelper.rattacher("dr@yay360.sn", 10L, rattachement("+221770000001")))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("membre");
    }

    private RattacherGrossesseRequest rattachement(String telephone) {
        RattacherGrossesseRequest request = new RattacherGrossesseRequest();
        request.setEtablissementId(3L);
        request.setTelephonePatiente(telephone);
        return request;
    }

    @Test
    void rattacher_refuseUneGrossesseDejaSuivieParUneAutreEquipe() {
        medecin();
        grossesseEnCours(); // sans établissement, mais suivie par un autre soignant (rendez-vous)
        when(etablissementRepository.existsByIdAndProfessionnelsId(3L, 50L)).thenReturn(true);
        when(grossesseRepository.estSuivieParProfessionnel(10L, 50L)).thenReturn(false);
        when(utilisateurRepository.findSoignantsSuivant(10L)).thenReturn(List.of(new Utilisateur()));

        assertThatThrownBy(() -> grossesseHelper.rattacher("dr@yay360.sn", 10L, rattachement("+221770000001")))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("autre équipe");
    }

    @Test
    void rattacher_exigeLeTelephoneDeLaPatiente() {
        medecin();
        grossesseEnCours();
        when(etablissementRepository.existsByIdAndProfessionnelsId(3L, 50L)).thenReturn(true);

        assertThatThrownBy(() -> grossesseHelper.rattacher("dr@yay360.sn", 10L, rattachement("+221779999999")))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("téléphone");
    }

    @Test
    void rattacher_uneGrossesseSansSuiviAvecPreuveDePresence() {
        medecin();
        Grossesse grossesse = grossesseEnCours();
        EtablissementSante etablissement = new EtablissementSante();
        etablissement.setId(3L);
        etablissement.setNom("Centre de santé");
        etablissement.setActif(true);
        when(etablissementRepository.existsByIdAndProfessionnelsId(3L, 50L)).thenReturn(true);
        when(etablissementRepository.findById(3L)).thenReturn(Optional.of(etablissement));
        when(utilisateurRepository.findSoignantsSuivant(10L)).thenReturn(List.of());
        when(grossesseRepository.save(any(Grossesse.class))).thenAnswer(i -> i.getArgument(0));

        grossesseHelper.rattacher("dr@yay360.sn", 10L, rattachement("+221770000001"));

        assertThat(grossesse.getEtablissementSuivi()).isSameAs(etablissement);
    }

    @Test
    void sansSuivi_quandLEtablissementChoisiNAAucunSoignant() {
        Grossesse grossesse = new Grossesse();
        grossesse.setId(10L);
        EtablissementSante vide = new EtablissementSante();
        vide.setId(4L);
        grossesse.setEtablissementSuivi(vide);
        when(utilisateurRepository.findSoignantsSuivant(10L)).thenReturn(List.of());
        AccesDossierHelper accesReel = new AccesDossierHelper(utilisateurRepository, grossesseRepository,
                etablissementRepository, mock(DossierFamilialHelper.class));

        // L'alerte d'une telle patiente reste prenable par tous les soignants
        assertThat(accesReel.estSansSuivi(grossesse)).isTrue();
    }

    @Test
    void terminer_annuleLesRendezVousEncorePrevus() {
        medecin();
        grossesseEnCours();
        when(grossesseRepository.estSuivieParProfessionnel(10L, 50L)).thenReturn(true);
        when(grossesseRepository.save(any(Grossesse.class))).thenAnswer(i -> i.getArgument(0));
        RendezVous prevu = new RendezVous();
        prevu.setStatut("CONFIRME");
        RendezVous passe = new RendezVous();
        passe.setStatut("TERMINE");
        when(rendezVousRepository.findByGrossesseIdOrderByDateHeureDesc(10L)).thenReturn(List.of(prevu, passe));
        TerminerGrossesseRequest request = new TerminerGrossesseRequest();
        request.setStatut(StatutGrossesse.INTERROMPUE);

        grossesseHelper.terminer("dr@yay360.sn", 10L, request);

        assertThat(prevu.getStatut()).isEqualTo("ANNULE");
        assertThat(passe.getStatut()).isEqualTo("TERMINE");
    }
}
