package sn.yay360.api.helper;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sn.yay360.api.helper.MoteurReglesCliniques.ContactCpn;
import sn.yay360.api.helper.MoteurReglesCliniques.SignalClinique;
import sn.yay360.api.model.NiveauGravite;
import sn.yay360.api.model.TypeSymptome;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoteurReglesCliniquesTest {

    private final MoteurReglesCliniques moteur = new MoteurReglesCliniques();

    private List<SignalClinique> analyser(int semaine, String ta, Double temperature, Integer rcf, Double hu) {
        return moteur.analyserConstantes(semaine, ta, temperature, rcf, hu, null, null, null);
    }

    @Nested
    class AnalyseDesConstantes {

        @Test
        void constantesNormalesNeProduisentAucunSignal() {
            assertThat(analyser(28, "115/75", 37.0, 140, 28.0)).isEmpty();
        }

        @ParameterizedTest(name = "TA {0} à {1} SA → {2}")
        @CsvSource({
                "139/89, 30, ",
                "140/85, 30, HTA",
                "130/90, 30, HTA",
                "160/100, 30, HTA_SEVERE",
                "150/110, 30, HTA_SEVERE"
        })
        void seuilsDeTensionArterielle(String ta, int semaine, String codeAttendu) {
            List<SignalClinique> signaux = analyser(semaine, ta, null, null, null);
            if (codeAttendu == null) {
                assertThat(signaux).isEmpty();
            } else {
                assertThat(signaux).extracting(SignalClinique::code).containsExactly(codeAttendu);
            }
        }

        @Test
        void hypertensionSevereApres20SAEvoqueLaPreeclampsieEtEstUrgente() {
            SignalClinique signal = analyser(32, "165/112", null, null, null).get(0);

            assertThat(signal.niveau()).isEqualTo(NiveauGravite.URGENT);
            assertThat(signal.message()).contains("prééclampsie");
        }

        @Test
        void hypertensionAvant20SANEvoquePasLaPreeclampsie() {
            assertThat(analyser(14, "145/92", null, null, null).get(0).message()).doesNotContain("prééclampsie");
        }

        @ParameterizedTest(name = "{0} °C → {1}")
        @CsvSource({"37.9, ", "38.0, ELEVE", "39.4, ELEVE", "39.5, URGENT"})
        void seuilsDeFievre(double temperature, String niveauAttendu) {
            List<SignalClinique> signaux = analyser(20, null, temperature, null, null);
            if (niveauAttendu == null) {
                assertThat(signaux).isEmpty();
            } else {
                assertThat(signaux).extracting(SignalClinique::niveau).containsExactly(niveauAttendu);
            }
        }

        @ParameterizedTest(name = "RCF {0}/min → anormal : {1}")
        @CsvSource({"109, true", "110, false", "160, false", "161, true"})
        void rythmeCardiaqueFoetalHorsDe110A160(int rcf, boolean anormal) {
            assertThat(analyser(30, null, null, rcf, null)).hasSize(anormal ? 1 : 0);
        }

        @Test
        void rythmeCardiaqueFoetalNonInterpreteAvant12SA() {
            assertThat(analyser(10, null, null, 180, null)).isEmpty();
        }

        @Test
        void hauteurUterineDiscordanteSelonLaRegleDeMcDonald() {
            assertThat(analyser(30, null, null, null, 30.0)).isEmpty();
            assertThat(analyser(30, null, null, null, 27.0)).isEmpty();          // tolérance de 3 cm
            SignalClinique petite = analyser(30, null, null, null, 25.0).get(0);  // 5 cm de moins
            assertThat(petite.code()).isEqualTo("HU_DISCORDANTE");
            assertThat(petite.message()).contains("retard de croissance");
            assertThat(analyser(20, null, null, null, 10.0)).isEmpty();          // non interprétée avant 24 SA
        }

        @Test
        void prisDePoidsRapideApres20SA() {
            List<SignalClinique> signaux = moteur.analyserConstantes(28, null, null, null, null,
                    70.0, 67.0, 14L); // 3 kg en 2 semaines = 1,5 kg/semaine
            assertThat(signaux).extracting(SignalClinique::code).containsExactly("PRISE_POIDS_RAPIDE");

            assertThat(moteur.analyserConstantes(28, null, null, null, null, 68.0, 67.0, 14L)).isEmpty();
        }

        @ParameterizedTest(name = "TA « {0} » ignorée (incohérente)")
        @CsvSource({"16/11", "80/120", "300/90", "120/20"})
        void uneTensionIncoherenteNEstPasInterpretee(String ta) {
            assertThat(MoteurReglesCliniques.lireTension(ta)).isNull();
        }

        @Test
        void leNiveauMaximalEstRetenu() {
            List<SignalClinique> signaux = analyser(30, "142/88", 38.2, 100, null);
            assertThat(MoteurReglesCliniques.niveauMax(signaux)).isEqualTo(NiveauGravite.URGENT);
        }
    }

    @Nested
    class TriageContextualise {

        @Test
        void sansContexteLeNiveauDeBaseEstConserve() {
            var triage = moteur.trier(TypeSymptome.VOMISSEMENTS_SEVERES, NiveauGravite.MODERE, 10, false, 0);

            assertThat(triage.niveau()).isEqualTo(NiveauGravite.MODERE);
            assertThat(triage.justifications()).hasSize(1);
        }

        @Test
        void signesDePreeclampsieApres20SAPassentEnUrgent() {
            var avant = moteur.trier(TypeSymptome.MAUX_DE_TETE_INTENSES, NiveauGravite.ELEVE, 18, false, 0);
            var apres = moteur.trier(TypeSymptome.MAUX_DE_TETE_INTENSES, NiveauGravite.ELEVE, 24, false, 0);

            assertThat(avant.niveau()).isEqualTo(NiveauGravite.ELEVE);
            assertThat(apres.niveau()).isEqualTo(NiveauGravite.URGENT);
            assertThat(apres.justifications()).anyMatch(j -> j.contains("prééclampsie"));
        }

        @Test
        void uneGrossesseARisqueReleveLeNiveau() {
            assertThat(moteur.trier(TypeSymptome.AUTRE, NiveauGravite.MODERE, 15, true, 0).niveau())
                    .isEqualTo(NiveauGravite.ELEVE);
        }

        @Test
        void desSignalementsRepetesRelevantLeNiveau() {
            assertThat(moteur.trier(TypeSymptome.AUTRE, NiveauGravite.MODERE, 15, false, 1).niveau())
                    .isEqualTo(NiveauGravite.MODERE);
            assertThat(moteur.trier(TypeSymptome.AUTRE, NiveauGravite.MODERE, 15, false, 2).niveau())
                    .isEqualTo(NiveauGravite.ELEVE);
        }

        @Test
        void lesAjustementsSeCumulentSansDepasserUrgent() {
            var triage = moteur.trier(TypeSymptome.AUTRE, NiveauGravite.MODERE, 15, true, 3);
            assertThat(triage.niveau()).isEqualTo(NiveauGravite.URGENT);

            assertThat(moteur.trier(TypeSymptome.SAIGNEMENTS, NiveauGravite.URGENT, 30, true, 5).niveau())
                    .isEqualTo(NiveauGravite.URGENT);
        }

        @Test
        void pertesDeLiquideAvant37SAEvoquentUneRupturePrematuree() {
            assertThat(moteur.trier(TypeSymptome.PERTE_LIQUIDE, NiveauGravite.URGENT, 33, false, 0).justifications())
                    .anyMatch(j -> j.contains("rupture prématurée"));
        }
    }

    @Nested
    class PlanDesConsultationsPrenatales {

        private final LocalDate ddr = LocalDate.of(2026, 1, 5);

        @Test
        void leGabaritOmsComporteHuitContacts() {
            List<ContactCpn> plan = moteur.planCpn(ddr, List.of(), List.of(), ddr.plusWeeks(5));

            assertThat(plan).extracting(ContactCpn::semaineCible).containsExactly(12, 20, 26, 30, 34, 36, 38, 40);
            assertThat(plan).extracting(ContactCpn::statut).containsOnly(MoteurReglesCliniques.CPN_A_VENIR);
            assertThat(plan.get(0).dateCible()).isEqualTo(ddr.plusWeeks(12));
        }

        @Test
        void detecteLesConsultationsRealiseesEtCellesEnRetard() {
            LocalDate aujourdhui = ddr.plusWeeks(25);
            List<ContactCpn> plan = moteur.planCpn(ddr, List.of(ddr.plusWeeks(11)), List.of(), aujourdhui);

            assertThat(plan.get(0).statut()).isEqualTo(MoteurReglesCliniques.CPN_REALISE);
            assertThat(plan.get(0).dateRealisation()).isEqualTo(ddr.plusWeeks(11));
            assertThat(plan.get(1).statut()).isEqualTo(MoteurReglesCliniques.CPN_EN_RETARD); // 20 SA manquée
            assertThat(plan.get(2).statut()).isEqualTo(MoteurReglesCliniques.CPN_A_VENIR);   // 26 SA
        }

        @Test
        void unRendezVousFuturDansLaFenetreMarqueLeContactPlanifie() {
            LocalDate aujourdhui = ddr.plusWeeks(18);
            List<ContactCpn> plan = moteur.planCpn(ddr, List.of(ddr.plusWeeks(12)),
                    List.of(ddr.plusWeeks(20)), aujourdhui);

            assertThat(plan.get(1).statut()).isEqualTo(MoteurReglesCliniques.CPN_PLANIFIE);
        }

        @Test
        void uneConsultationNeValidePasUnContactFutur() {
            // Consultations à 12, 20 et 26 SA puis deux à 27 SA : le contact de 30 SA n'est pas validé
            LocalDate aujourdhui = ddr.plusWeeks(27);
            List<ContactCpn> plan = moteur.planCpn(ddr, List.of(ddr.plusWeeks(12), ddr.plusWeeks(20),
                    ddr.plusWeeks(26), aujourdhui, aujourdhui), List.of(), aujourdhui);

            assertThat(plan.subList(0, 3)).extracting(ContactCpn::statut)
                    .containsOnly(MoteurReglesCliniques.CPN_REALISE);
            assertThat(plan.get(3).statut()).isEqualTo(MoteurReglesCliniques.CPN_A_VENIR);
        }

        @Test
        void unePremiereConsultationTardiveValideLePremierContact() {
            // Cas fréquent : première consultation prénatale à 16 SA
            List<ContactCpn> plan = moteur.planCpn(ddr, List.of(ddr.plusWeeks(16)), List.of(), ddr.plusWeeks(17));

            assertThat(plan.get(0).statut()).isEqualTo(MoteurReglesCliniques.CPN_REALISE);
            assertThat(plan.get(1).statut()).isEqualTo(MoteurReglesCliniques.CPN_A_VENIR);
        }

        @Test
        void uneConsultationNeValideQuUnSeulContact() {
            List<ContactCpn> plan = moteur.planCpn(ddr, List.of(ddr.plusWeeks(19)), List.of(), ddr.plusWeeks(19));

            assertThat(plan.stream().filter(c -> MoteurReglesCliniques.CPN_REALISE.equals(c.statut()))).hasSize(1);
        }
    }

    @Nested
    class FacteursDeRisque {

        private final LocalDate ddr = LocalDate.of(2026, 6, 1);

        @Test
        void aucunFacteurPourUneGrossesseSansParticularite() {
            assertThat(moteur.facteursDeRisque(LocalDate.of(1998, 1, 1), ddr, 1, "RAS", List.of(), 0)).isEmpty();
        }

        @Test
        void detecteAgeParitéAntecedentsEtSignaux() {
            List<String> facteurs = moteur.facteursDeRisque(LocalDate.of(1988, 1, 1), ddr, 5,
                    "Césarienne en 2020, DIABÈTE gestationnel", List.of("HTA", "RCF_ANORMAL"), 2);

            assertThat(facteurs).contains(
                    "Âge maternel avancé (38 ans)",
                    "Grande multiparité (5 grossesses antérieures)",
                    "Utérus cicatriciel (antécédent de césarienne)",
                    "Diabète",
                    "Hypertension relevée en consultation",
                    "Rythme cardiaque fœtal anormal relevé en consultation",
                    "2 alertes urgentes signalées pendant la grossesse");
        }

        @Test
        void grossessePrecoce() {
            assertThat(moteur.facteursDeRisque(LocalDate.of(2010, 1, 1), ddr, 0, null, List.of(), 0))
                    .containsExactly("Grossesse précoce (16 ans)");
        }
    }

    @Nested
    class TriageParTypeDeSymptome {

        @Test
        void appliqueLeNiveauDeBaseDeLaTableDuCahierDesCharges() {
            assertThat(moteur.trierSymptome(TypeSymptome.SAIGNEMENTS, 10, false, 0).niveau()).isEqualTo(NiveauGravite.URGENT);
            assertThat(moteur.trierSymptome(TypeSymptome.MAUX_DE_TETE_INTENSES, 10, false, 0).niveau()).isEqualTo(NiveauGravite.ELEVE);
            assertThat(moteur.trierSymptome(TypeSymptome.VOMISSEMENTS_SEVERES, 10, false, 0).niveau()).isEqualTo(NiveauGravite.MODERE);
        }

        @Test
        void unTypeInconnuEstTraiteCommeModere() {
            var triage = moteur.trierSymptome("TYPE_INCONNU", 10, false, 0);

            assertThat(triage.niveau()).isEqualTo(NiveauGravite.MODERE);
            assertThat(triage.justifications()).hasSize(1);
        }

        @Test
        void ajusteEnsuiteLeNiveauAuContexte() {
            var triage = moteur.trierSymptome(TypeSymptome.MAUX_DE_TETE_INTENSES, 24, false, 0);

            assertThat(triage.niveau()).isEqualTo(NiveauGravite.URGENT);
            assertThat(triage.justifications()).anySatisfy(j -> assertThat(j).contains("prééclampsie"));
        }
    }
}
