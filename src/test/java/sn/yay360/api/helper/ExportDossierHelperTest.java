package sn.yay360.api.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sn.yay360.api.dto.GrossesseDto;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.mapper.AlerteMapper;
import sn.yay360.api.mapper.ConsultationMapper;
import sn.yay360.api.mapper.RendezVousMapper;
import sn.yay360.api.model.*;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.RendezVousRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExportDossierHelperTest {

    private GrossesseHelper grossesseHelper;
    private AccesDossierHelper acces;
    private RendezVousRepository rendezVous;
    private DossierPdfRenderer renderer;
    private ExportDossierHelper helper;
    private Grossesse grossesse;
    private Utilisateur soignant;

    @BeforeEach
    void setUp() {
        grossesseHelper = mock(GrossesseHelper.class);
        acces = mock(AccesDossierHelper.class);
        rendezVous = mock(RendezVousRepository.class);
        renderer = mock(DossierPdfRenderer.class);
        IntelligenceCliniqueHelper intelligence = mock(IntelligenceCliniqueHelper.class);
        helper = new ExportDossierHelper(grossesseHelper, intelligence, acces, mock(ConsultationRepository.class),
                mock(AlerteMedicaleRepository.class), rendezVous, new ConsultationMapper(), new AlerteMapper(),
                new RendezVousMapper(), renderer);

        Utilisateur femme = new Utilisateur();
        femme.setId(1L);
        femme.setPrenom("Awa");
        femme.setNom("Démo");
        femme.setDateNaissance(LocalDate.of(1996, 5, 12));
        grossesse = new Grossesse();
        grossesse.setId(3L);
        grossesse.setFemmeEnceinte(femme);
        grossesse.setStatut(StatutGrossesse.EN_COURS);
        soignant = new Utilisateur();
        soignant.setId(50L);
        soignant.setRole(RoleUtilisateur.SAGE_FEMME);

        when(grossesseHelper.trouverActiveParEmail("awa@exemple.sn")).thenReturn(grossesse);
        when(grossesseHelper.trouverParId(3L)).thenReturn(grossesse);
        when(grossesseHelper.versDtoCalcule(grossesse)).thenReturn(new GrossesseDto());
        when(acces.utilisateur("sf@exemple.sn")).thenReturn(soignant);
        when(renderer.rendre(any())).thenReturn("%PDF-fictif".getBytes());
    }

    private RendezVous rdv(long id, String statut, LocalDateTime quand) {
        RendezVous r = new RendezVous();
        r.setId(id);
        r.setGrossesse(grossesse);
        r.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        r.setStatut(statut);
        r.setType(TypeRendezVous.ECHOGRAPHIE);
        r.setDateHeure(quand);
        return r;
    }

    @Test
    void laPatienteExporteSaGrossesseEnCours_avecSeulementLesRendezVousActifsAVenir() {
        LocalDateTime maintenant = LocalDateTime.now();
        when(rendezVous.findByGrossesseIdOrderByDateHeureDesc(3L)).thenReturn(List.of(
                rdv(1, StatutRendezVous.CONFIRME, maintenant.plusDays(20)),
                rdv(2, StatutRendezVous.ANNULE, maintenant.plusDays(10)),
                rdv(3, StatutRendezVous.PLANIFIE, maintenant.plusDays(5)),
                rdv(4, StatutRendezVous.TERMINE, maintenant.minusDays(5))));

        byte[] pdf = helper.exporterMaGrossesse("awa@exemple.sn");

        assertThat(new String(pdf)).startsWith("%PDF");
        ArgumentCaptor<DossierPdfRenderer.Dossier> dossier = ArgumentCaptor.forClass(DossierPdfRenderer.Dossier.class);
        verify(renderer).rendre(dossier.capture());
        assertThat(dossier.getValue().patienteNomComplet()).isEqualTo("Awa Démo");
        assertThat(dossier.getValue().rendezVousAVenir()).extracting(r -> r.getId()).containsExactly(3L, 1L);
    }

    @Test
    void unSoignantQuiNeSuitPasLaPatiente_estRefuse() {
        when(acces.suit(soignant, grossesse)).thenReturn(false);

        assertThatThrownBy(() -> helper.exporterParId("sf@exemple.sn", 3L))
                .isInstanceOf(OperationNonAutoriseeException.class);
        verifyNoInteractions(renderer);
    }

    @Test
    void unSoignantQuiSuitLaPatiente_exporte() {
        when(acces.suit(soignant, grossesse)).thenReturn(true);

        assertThat(helper.exporterParId("sf@exemple.sn", 3L)).isNotEmpty();
        verify(renderer).rendre(any());
    }

    @Test
    void grossesseClose_pasDePlanDesConsultations() {
        grossesse.setStatut(StatutGrossesse.TERMINEE);
        when(acces.suit(soignant, grossesse)).thenReturn(true);

        helper.exporterParId("sf@exemple.sn", 3L);

        ArgumentCaptor<DossierPdfRenderer.Dossier> dossier = ArgumentCaptor.forClass(DossierPdfRenderer.Dossier.class);
        verify(renderer).rendre(dossier.capture());
        assertThat(dossier.getValue().planCpn()).isNull();
    }
}
