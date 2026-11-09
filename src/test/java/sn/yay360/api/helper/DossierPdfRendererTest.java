package sn.yay360.api.helper;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import sn.yay360.api.dto.AlerteDto;
import sn.yay360.api.dto.ConsultationDto;
import sn.yay360.api.dto.GrossesseDto;
import sn.yay360.api.dto.PlanCpnDto;
import sn.yay360.api.dto.RendezVousDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Données fictives uniquement. */
class DossierPdfRendererTest {

    private final DossierPdfRenderer renderer = new DossierPdfRenderer();

    static String texte(byte[] pdf) throws Exception {
        PdfReader lecteur = new PdfReader(pdf);
        StringBuilder sb = new StringBuilder();
        PdfTextExtractor extracteur = new PdfTextExtractor(lecteur);
        for (int page = 1; page <= lecteur.getNumberOfPages(); page++) {
            sb.append(extracteur.getTextFromPage(page)).append('\n');
        }
        return sb.toString();
    }

    static DossierPdfRenderer.Dossier dossier() {
        GrossesseDto g = new GrossesseDto();
        g.setId(3L);
        g.setStatut("EN_COURS");
        g.setDateDernieresRegles(LocalDate.of(2026, 5, 25));
        g.setDatePrevueAccouchement(LocalDate.of(2027, 3, 1));
        g.setDebutPeriodeTerme(LocalDate.of(2027, 2, 8));
        g.setFinPeriodeTerme(LocalDate.of(2027, 3, 14));
        g.setSemaineActuelle(24);
        g.setTrimestre("T2");
        g.setGrossesseARisque(true);
        g.setMotifRisque("Tension élevée (fictif)");
        g.setEtablissementSuiviNom("Centre de santé Démo");

        ConsultationDto c = new ConsultationDto();
        c.setDateConsultation(LocalDateTime.of(2026, 10, 20, 9, 0));
        c.setTensionArterielle("145/95");
        c.setPoidsKg(64.0);
        c.setProteinurie("DEUX_CROIX");
        c.setGlycosurie("NEGATIVE");
        c.setProfessionnelNomComplet("Sage-femme Démo");
        c.setSignauxCliniques(List.of("[ELEVE] Pression artérielle 145/95 mmHg ≥ 140/90 : hypertension"));

        AlerteDto a = new AlerteDto();
        a.setDateDeclaration(LocalDateTime.of(2026, 10, 22, 8, 0));
        a.setTypeSymptome("DIFFICULTES_RESPIRATOIRES");
        a.setNiveauGravite("URGENT");
        a.setStatut("RESOLUE");
        a.setResoluParNomComplet("Médecin Démo");

        RendezVousDto r = new RendezVousDto();
        r.setDateHeure(LocalDateTime.of(2026, 11, 30, 10, 0));
        r.setType("ECHOGRAPHIE");
        r.setStatut("CONFIRME");
        r.setEtablissementNom("Centre de santé Démo");

        PlanCpnDto plan = new PlanCpnDto(3L, 24, List.of(
                new MoteurReglesCliniques.ContactCpn(1, 12, LocalDate.of(2026, 8, 17), "REALISE", LocalDate.of(2026, 8, 18))),
                1, 0, "OMS 2016");
        return new DossierPdfRenderer.Dossier(g, "Awa Démo", LocalDate.of(1996, 5, 12), "+221770000001",
                List.of(c), List.of(a), List.of(r), plan, LocalDateTime.of(2026, 11, 10, 9, 30));
    }

    @Test
    void produitUnPdfLisibleAvecTousLesBlocsDuDossier() throws Exception {
        byte[] pdf = renderer.rendre(dossier());

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String texte = texte(pdf);
        assertThat(texte)
                .contains("Dossier de suivi de grossesse")
                .contains("Awa Démo")
                .contains("Terme estimé").contains("1 mars 2027")
                .contains("8 février 2027").contains("14 mars 2027")
                .contains("Oui : Tension élevée (fictif)")
                .contains("145/95")
                .contains("Prot. ++ / Gly. négative")
                .contains("Difficultés à respirer").contains("Résolue par Médecin Démo")
                .contains("Échographie")
                .contains("1 sur 8 réalisées")
                .contains("ne remplace ni le")
                .contains("Page 1");
    }

    @Test
    void lesCaracteresAbsentsDeLaPoliceSontRemplaces() throws Exception {
        assertThat(renderer.t("TA ≥ 140/90 et ≤ 160")).isEqualTo("TA >= 140/90 et <= 160");
        assertThat(texte(renderer.rendre(dossier()))).contains(">= 140/90").doesNotContain("≥");
        assertThat(texte(renderer.rendre(dossier()))).contains("Élevé : Pression artérielle").doesNotContain("[ELEVE]");
    }

    @Test
    void dossierVide_messagesExplicites() throws Exception {
        DossierPdfRenderer.Dossier d = dossier();
        DossierPdfRenderer.Dossier vide = new DossierPdfRenderer.Dossier(d.grossesse(), d.patienteNomComplet(),
                d.patienteDateNaissance(), null, List.of(), List.of(), List.of(), null, d.genereLe());

        String texte = texte(renderer.rendre(vide));

        assertThat(texte).contains("Aucune consultation enregistrée.").contains("Aucun signalement.")
                .contains("Aucun rendez-vous à venir.");
    }
}
