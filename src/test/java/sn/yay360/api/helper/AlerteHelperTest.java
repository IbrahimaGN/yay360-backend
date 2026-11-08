package sn.yay360.api.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sn.yay360.api.dto.AlerteDto;
import sn.yay360.api.dto.DeclarerSymptomeRequest;
import sn.yay360.api.dto.GrossesseDto;
import sn.yay360.api.dto.PriseEnChargeRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.mapper.AlerteMapper;
import sn.yay360.api.model.AlerteMedicale;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.NiveauGravite;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutAlerte;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.TypeSymptome;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.UtilisateurRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AlerteHelperTest {

    private AlerteMedicaleRepository alerteRepository;
    private UtilisateurRepository utilisateurRepository;
    private GrossesseHelper grossesseHelper;
    private NotificationHelper notificationHelper;
    private DossierFamilialHelper dossierFamilialHelper;
    private AccesDossierHelper acces;
    private AlerteHelper helper;

    private Utilisateur femme;
    private Utilisateur medecin;
    private Grossesse grossesse;

    @BeforeEach
    void setUp() {
        alerteRepository = mock(AlerteMedicaleRepository.class);
        utilisateurRepository = mock(UtilisateurRepository.class);
        grossesseHelper = mock(GrossesseHelper.class);
        notificationHelper = mock(NotificationHelper.class);
        dossierFamilialHelper = mock(DossierFamilialHelper.class);
        acces = mock(AccesDossierHelper.class);
        helper = new AlerteHelper(alerteRepository, utilisateurRepository, grossesseHelper, notificationHelper,
                new AlerteMapper(), dossierFamilialHelper, acces, new MoteurReglesCliniques());

        femme = utilisateur(1L, RoleUtilisateur.FEMME_ENCEINTE);
        medecin = utilisateur(50L, RoleUtilisateur.MEDECIN);
        grossesse = new Grossesse();
        grossesse.setId(10L);
        grossesse.setFemmeEnceinte(femme);
        grossesse.setStatut(StatutGrossesse.EN_COURS);
        grossesse.setGrossesseARisque(false);
        grossesse.setDateDernieresRegles(LocalDate.now().minusWeeks(24));

        when(acces.utilisateur("dr@yay360.sn")).thenReturn(medecin);
        when(alerteRepository.save(any(AlerteMedicale.class))).thenAnswer(i -> {
            AlerteMedicale a = i.getArgument(0);
            if (a.getId() == null) {
                a.setId(99L);
            }
            return a;
        });
        GrossesseDto dto = new GrossesseDto();
        dto.setSemaineActuelle(25);
        dto.setAgeGestationnelJours(24 * 7); // 24 SA révolues
        when(grossesseHelper.versDtoCalcule(grossesse)).thenReturn(dto);
    }

    private static Utilisateur utilisateur(Long id, String role) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setRole(role);
        u.setPrenom("P" + id);
        u.setNom("N" + id);
        u.setActif(true);
        return u;
    }

    private AlerteMedicale alerte(String statut) {
        AlerteMedicale alerte = new AlerteMedicale();
        alerte.setId(5L);
        alerte.setGrossesse(grossesse);
        alerte.setFemmeEnceinte(femme);
        alerte.setStatut(statut);
        alerte.setNiveauGravite(NiveauGravite.URGENT);
        alerte.setTypeSymptome(TypeSymptome.SAIGNEMENTS);
        when(alerteRepository.findById(5L)).thenReturn(Optional.of(alerte));
        return alerte;
    }

    // ==================== DÉCLARATION ET TRIAGE ====================

    @Test
    void declarerSymptome_appliqueLeTriageContextuelEtTraceLaJustification() {
        when(dossierFamilialHelper.trouverFemmeAssociee("awa@yay360.sn")).thenReturn(femme);
        when(grossesseHelper.trouverActiveParEmail("awa@yay360.sn")).thenReturn(grossesse);
        when(dossierFamilialHelper.aAccesAuDossier(femme, femme)).thenReturn(true);
        DeclarerSymptomeRequest request = new DeclarerSymptomeRequest();
        request.setTypeSymptome(TypeSymptome.TROUBLES_VISUELS);

        AlerteDto dto = helper.declarerSymptome("awa@yay360.sn", request);

        assertThat(dto.getNiveauGravite()).isEqualTo(NiveauGravite.URGENT); // ELEVE → URGENT après 20 SA
        assertThat(dto.getOrigine()).isEqualTo(AlerteHelper.ORIGINE_PATIENTE);
        assertThat(dto.getJustificationTriage()).anyMatch(j -> j.contains("prééclampsie"));
    }

    @Test
    void declarerSymptome_sansSuiviPrevientTousLesSoignantsSansDonneeIdentifiante() {
        when(dossierFamilialHelper.trouverFemmeAssociee("awa@yay360.sn")).thenReturn(femme);
        when(grossesseHelper.trouverActiveParEmail("awa@yay360.sn")).thenReturn(grossesse);
        when(dossierFamilialHelper.aAccesAuDossier(femme, femme)).thenReturn(true);
        when(utilisateurRepository.findSoignantsSuivant(10L)).thenReturn(List.of());
        when(utilisateurRepository.findByRoleInAndActifTrue(any())).thenReturn(List.of(medecin));
        DeclarerSymptomeRequest request = new DeclarerSymptomeRequest();
        request.setTypeSymptome(TypeSymptome.SAIGNEMENTS);

        helper.declarerSymptome("awa@yay360.sn", request);

        verify(notificationHelper).envoyerSansCopieConjoint(eq(medecin), eq(grossesse), anyString(),
                argThat(titre -> titre.contains("sans suivi") && !titre.contains("P1")),
                argThat(message -> !message.contains("P1") && !message.contains("N1")));
    }

    @Test
    void declarerSymptome_refuseUneGrossesseCloturee() {
        grossesse.setStatut(StatutGrossesse.TERMINEE);
        when(dossierFamilialHelper.trouverFemmeAssociee("awa@yay360.sn")).thenReturn(femme);
        when(grossesseHelper.trouverActiveParEmail("awa@yay360.sn")).thenReturn(grossesse);
        when(dossierFamilialHelper.aAccesAuDossier(femme, femme)).thenReturn(true);
        DeclarerSymptomeRequest request = new DeclarerSymptomeRequest();
        request.setTypeSymptome(TypeSymptome.AUTRE);

        assertThatThrownBy(() -> helper.declarerSymptome("awa@yay360.sn", request))
                .isInstanceOf(OperationNonAutoriseeException.class);
        verify(alerteRepository, never()).save(any());
    }

    // ==================== CYCLE DE VIE ====================

    @Test
    void prendreEnCharge_passeEnPriseEnChargeEtDateLaReaction() {
        AlerteMedicale alerte = alerte(StatutAlerte.NOUVELLE);
        when(acces.suit(medecin, grossesse)).thenReturn(true);

        helper.prendreEnCharge("dr@yay360.sn", 5L);

        assertThat(alerte.getStatut()).isEqualTo(StatutAlerte.PRISE_EN_CHARGE);
        assertThat(alerte.getProfessionnelPriseEnCharge()).isSameAs(medecin);
        assertThat(alerte.getDatePriseEnCharge()).isNotNull();
    }

    @Test
    void prendreEnCharge_refuseUneAlerteDejaPriseOuResolue() {
        when(acces.suit(medecin, grossesse)).thenReturn(true);
        for (String statut : List.of(StatutAlerte.PRISE_EN_CHARGE, StatutAlerte.RESOLUE)) {
            alerte(statut);
            assertThatThrownBy(() -> helper.prendreEnCharge("dr@yay360.sn", 5L))
                    .isInstanceOf(OperationNonAutoriseeException.class);
        }
    }

    @Test
    void prendreEnCharge_refuseUnePatienteSuivieParUneAutreEquipe() {
        alerte(StatutAlerte.NOUVELLE);
        when(acces.suit(medecin, grossesse)).thenReturn(false);
        when(acces.estSansSuivi(grossesse)).thenReturn(false);

        assertThatThrownBy(() -> helper.prendreEnCharge("dr@yay360.sn", 5L))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("autre équipe");
    }

    @Test
    void prendreEnCharge_autoriseUneAlerteOrpheline() {
        AlerteMedicale alerte = alerte(StatutAlerte.NOUVELLE);
        when(acces.suit(medecin, grossesse)).thenReturn(false);
        when(acces.estSansSuivi(grossesse)).thenReturn(true);

        helper.prendreEnCharge("dr@yay360.sn", 5L);

        assertThat(alerte.getStatut()).isEqualTo(StatutAlerte.PRISE_EN_CHARGE);
    }

    @Test
    void resoudre_refuseUneAlerteNonPriseEnCharge() {
        alerte(StatutAlerte.NOUVELLE);
        PriseEnChargeRequest request = new PriseEnChargeRequest();
        request.setNotesPriseEnCharge("Vue en consultation");

        assertThatThrownBy(() -> helper.resoudre("dr@yay360.sn", 5L, request))
                .isInstanceOf(OperationNonAutoriseeException.class);
    }

    @Test
    void resoudre_parLeSoignantQuiAPrisEnChargeEnregistreLesObservations() {
        AlerteMedicale alerte = alerte(StatutAlerte.PRISE_EN_CHARGE);
        alerte.setProfessionnelPriseEnCharge(medecin);
        PriseEnChargeRequest request = new PriseEnChargeRequest();
        request.setNotesPriseEnCharge("  Saignement minime, repos prescrit.  ");

        helper.resoudre("dr@yay360.sn", 5L, request);

        assertThat(alerte.getStatut()).isEqualTo(StatutAlerte.RESOLUE);
        assertThat(alerte.getNotesPriseEnCharge()).isEqualTo("Saignement minime, repos prescrit.");
        assertThat(alerte.getDateResolution()).isNotNull();
        assertThat(alerte.getProfessionnelResolution()).isSameAs(medecin);
        verify(notificationHelper).envoyer(eq(femme), eq(grossesse), anyString(), anyString(), anyString());
    }

    @Test
    void resoudre_parUnAutreSoignantQuiSuitLaPatiente_distingueLePreneurDuResolveur() {
        Utilisateur sageFemme = utilisateur(60L, RoleUtilisateur.SAGE_FEMME);
        AlerteMedicale alerte = alerte(StatutAlerte.PRISE_EN_CHARGE);
        alerte.setProfessionnelPriseEnCharge(sageFemme);
        when(acces.suit(medecin, grossesse)).thenReturn(true);
        PriseEnChargeRequest request = new PriseEnChargeRequest();
        request.setNotesPriseEnCharge("TA contrôlée, consignes remises.");

        AlerteDto resultat = helper.resoudre("dr@yay360.sn", 5L, request);

        assertThat(resultat.getProfessionnelNomComplet()).isEqualTo("P60 N60");
        assertThat(resultat.getResoluParNomComplet()).isEqualTo("P50 N50");
    }

    @Test
    void resoudre_refuseUnSoignantQuiNeSuitPasLaPatiente() {
        AlerteMedicale alerte = alerte(StatutAlerte.PRISE_EN_CHARGE);
        alerte.setProfessionnelPriseEnCharge(utilisateur(60L, RoleUtilisateur.SAGE_FEMME));
        when(acces.suit(medecin, grossesse)).thenReturn(false);
        PriseEnChargeRequest request = new PriseEnChargeRequest();
        request.setNotesPriseEnCharge("Tentative");

        assertThatThrownBy(() -> helper.resoudre("dr@yay360.sn", 5L, request))
                .isInstanceOf(OperationNonAutoriseeException.class);
        assertThat(alerte.getProfessionnelResolution()).isNull();
    }

    @Test
    void listerParStatut_refuseUnStatutInconnu() {
        assertThatThrownBy(() -> helper.listerParStatut("dr@yay360.sn", "PIRATE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
