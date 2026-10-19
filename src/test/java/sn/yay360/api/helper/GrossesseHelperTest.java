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

    private GrossesseMapper grossesseMapper;
    private CalculObstetricalHelper calcul;
    private GrossesseHelper grossesseHelper;

    private Utilisateur femme;

    @BeforeEach
    void setUp() {
        grossesseMapper = new GrossesseMapper();
        calcul = new CalculObstetricalHelper();
        DossierFamilialHelper dossierFamilialHelper =
                new DossierFamilialHelper(utilisateurRepository, invitationConjointRepository);
        grossesseHelper = new GrossesseHelper(
                grossesseRepository, utilisateurRepository, grossesseMapper, calcul, dossierFamilialHelper);

        femme = new Utilisateur();
        femme.setId(1L);
        femme.setNom("Ndiaye");
        femme.setPrenom("Fatou");
        femme.setEmail("fatou.ndiaye@yay360.sn");
        femme.setRole(RoleUtilisateur.FEMME_ENCEINTE);
    }

    @Test
    void declarer_devraitEchouerSiGrossesseActiveExisteDeja() {
        DeclarerGrossesseRequest request = new DeclarerGrossesseRequest();
        request.setDateDernieresRegles(LocalDate.now().minusWeeks(5));

        when(utilisateurRepository.findByEmail(femme.getEmail())).thenReturn(Optional.of(femme));
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

        when(utilisateurRepository.findByEmail(femme.getEmail())).thenReturn(Optional.of(femme));
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

        when(utilisateurRepository.findByEmail(femme.getEmail())).thenReturn(Optional.of(femme));
        when(grossesseRepository.findByFemmeEnceinteIdAndStatut(femme.getId(), StatutGrossesse.EN_COURS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> grossesseHelper.declarer(femme.getEmail(), request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
