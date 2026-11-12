package sn.yay360.api.helper;

import org.junit.jupiter.api.Test;
import sn.yay360.api.dto.ConseilDto;
import sn.yay360.api.mapper.ConseilMapper;
import sn.yay360.api.model.ConseilGrossesse;
import sn.yay360.api.repo.ConseilGrossesseRepository;
import sn.yay360.api.repo.UtilisateurRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Conseils bornés à 40 semaines et délai moyen de prise en charge du tableau de bord. */
class ConseilEtTableauDeBordTest {

    private static ConseilGrossesse conseil(int debut, Integer fin) {
        ConseilGrossesse c = new ConseilGrossesse();
        c.setSemaineDebut(debut);
        c.setSemaineFin(fin);
        c.setTitre("Conseil fictif");
        return c;
    }

    @Test
    void lesConseilsSontBornesAQuaranteSemaines_etUneFinAbsenteResteAbsente() {
        ConseilGrossesseRepository repository = mock(ConseilGrossesseRepository.class);
        when(repository.findAll()).thenReturn(List.of(conseil(38, 42), conseil(10, null)));
        ConseilHelper helper = new ConseilHelper(repository, mock(UtilisateurRepository.class),
                new ConseilMapper(), new CalculObstetricalHelper());

        List<ConseilDto> conseils = helper.listerTous();

        assertThat(conseils).extracting(ConseilDto::getSemaineFin).containsExactly(40, null);
    }

    @Test
    void delaiMoyenDePriseEnCharge_enMinutesArrondiAuDixieme() {
        LocalDateTime declaration = LocalDateTime.of(2026, 11, 10, 9, 0);
        List<Object[]> delais = List.of(
                new Object[]{declaration, declaration.plusMinutes(10)},
                new Object[]{declaration, declaration.plusMinutes(25)});

        assertThat(DashboardHelper.delaiMoyenMinutes(delais)).isEqualTo(17.5);
        assertThat(DashboardHelper.delaiMoyenMinutes(List.of())).isNull();
    }
}
