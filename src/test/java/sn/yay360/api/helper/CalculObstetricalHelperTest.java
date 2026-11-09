package sn.yay360.api.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CalculObstetricalHelperTest {

    private final CalculObstetricalHelper calcul = new CalculObstetricalHelper();

    @Test
    void calculerDatePrevueAccouchement_devraitAjouter280Jours() {
        LocalDate ddr = LocalDate.of(2026, 1, 1);
        LocalDate dpa = calcul.calculerDatePrevueAccouchement(ddr);
        assertThat(dpa).isEqualTo(ddr.plusDays(280));
    }

    @Test
    void calculerAgeGestationnelJours_devraitEtreZeroSiDdrAujourdhui() {
        int ageJours = calcul.calculerAgeGestationnelJours(LocalDate.now());
        assertThat(ageJours).isZero();
    }

    @Test
    void calculerSemaineActuelle_devraitEtreUnSiDdrAujourdhui() {
        int semaine = calcul.calculerSemaineActuelle(LocalDate.now());
        assertThat(semaine).isEqualTo(1);
    }

    @Test
    void calculerSemaineActuelle_devraitEtrePlafonneeA40() {
        LocalDate ddrTresAncienne = LocalDate.now().minusWeeks(60);
        int semaine = calcul.calculerSemaineActuelle(ddrTresAncienne);
        assertThat(semaine).isEqualTo(40);
    }

    @ParameterizedTest
    @CsvSource({
            "1, T1",
            "13, T1",
            "14, T2",
            "27, T2",
            "28, T3",
            "40, T3"
    })
    void calculerTrimestre_devraitRetournerLeBonTrimestre(int semaine, String trimestreAttendu) {
        assertThat(calcul.calculerTrimestre(semaine)).isEqualTo(trimestreAttendu);
    }

    @Test
    void calculerJoursRestants_devraitEtreZeroSiDpaDepassee() {
        LocalDate dpaPassee = LocalDate.now().minusDays(10);
        int joursRestants = calcul.calculerJoursRestants(dpaPassee);
        assertThat(joursRestants).isZero();
    }

    @Test
    void calculerJoursRestants_devraitCorrespondreAuNombreDeJours() {
        LocalDate dpaFuture = LocalDate.now().plusDays(30);
        int joursRestants = calcul.calculerJoursRestants(dpaFuture);
        assertThat(joursRestants).isEqualTo(30);
    }

    @Test
    void periodeATerme_de37SA0jA41SA6j_autourDuTermeEstime() {
        LocalDate ddr = LocalDate.of(2026, 5, 25);

        assertThat(calcul.calculerDatePrevueAccouchement(ddr)).isEqualTo(LocalDate.of(2027, 3, 1));
        assertThat(CalculObstetricalHelper.debutPeriodeTerme(ddr)).isEqualTo(LocalDate.of(2027, 2, 8));  // + 259 j
        assertThat(CalculObstetricalHelper.finPeriodeTerme(ddr)).isEqualTo(LocalDate.of(2027, 3, 14));  // + 293 j
    }

    @Test
    void periodeATerme_absenteSansDateDesDernieresRegles() {
        assertThat(CalculObstetricalHelper.debutPeriodeTerme(null)).isNull();
        assertThat(CalculObstetricalHelper.finPeriodeTerme(null)).isNull();
    }
}
