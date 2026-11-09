package sn.yay360.api.helper;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import sn.yay360.api.dto.AlerteDto;
import sn.yay360.api.dto.ConsultationDto;
import sn.yay360.api.dto.GrossesseDto;
import sn.yay360.api.dto.PlanCpnDto;
import sn.yay360.api.dto.RendezVousDto;
import sn.yay360.api.model.ResultatBandelette;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mise en page du dossier de grossesse au format PDF (A4, police Poppins embarquée, charte YAY360).
 * Ne fait aucun contrôle d'accès : il reçoit des données déjà autorisées par {@link ExportDossierHelper}.
 */
@Component
public class DossierPdfRenderer {

    /** Contenu du dossier, rassemblé par le helper d'export. */
    public record Dossier(GrossesseDto grossesse, String patienteNomComplet, LocalDate patienteDateNaissance,
                          String patienteTelephone, List<ConsultationDto> consultations, List<AlerteDto> alertes,
                          List<RendezVousDto> rendezVousAVenir, PlanCpnDto planCpn, LocalDateTime genereLe) {
    }

    private static final Color TEAL = new Color(0x1A, 0x7F, 0x86);
    private static final Color TEAL_CLAIR = new Color(0xE8, 0xF5, 0xF6);
    private static final Color ENCRE = new Color(0x1F, 0x2D, 0x33);
    private static final Color GRIS = new Color(0x5A, 0x6B, 0x74);
    private static final Color ORANGE = new Color(0xB4, 0x5C, 0x0B);
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter JOUR_COURT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter JOUR_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm", Locale.FRENCH);

    static final String AVERTISSEMENT = "Ce document est une synthèse du suivi enregistré dans YAY360. Il ne remplace ni le "
            + "carnet de santé officiel ni l'avis d'un professionnel de santé. Le terme est une estimation (règle de Naegele) ; "
            + "les signaux et niveaux de gravité sont des aides au repérage fondées sur des règles, pas des diagnostics.";

    private static final Map<String, String> SYMPTOMES = Map.ofEntries(
            Map.entry("SAIGNEMENTS", "Saignements"), Map.entry("DOULEURS_ABDOMINALES", "Douleurs abdominales intenses"),
            Map.entry("PERTE_LIQUIDE", "Perte de liquide"), Map.entry("DIMINUTION_MOUVEMENTS_BEBE", "Le bébé bouge moins"),
            Map.entry("FIEVRE_ELEVEE", "Fièvre élevée"), Map.entry("CONVULSIONS", "Convulsions"),
            Map.entry("DIFFICULTES_RESPIRATOIRES", "Difficultés à respirer"),
            Map.entry("MAUX_DE_TETE_INTENSES", "Maux de tête intenses"), Map.entry("TROUBLES_VISUELS", "Troubles de la vue"),
            Map.entry("OEDEMES_SOUDAINS", "Gonflement soudain"), Map.entry("VOMISSEMENTS_SEVERES", "Vomissements sévères"),
            Map.entry("CONSTANTES_ANORMALES", "Constantes anormales (consultation)"), Map.entry("AUTRE", "Autre symptôme"));
    private static final Map<String, String> STATUTS = Map.ofEntries(
            Map.entry("NOUVELLE", "Nouvelle"), Map.entry("PRISE_EN_CHARGE", "Prise en charge"), Map.entry("RESOLUE", "Résolue"),
            Map.entry("EN_COURS", "En cours"), Map.entry("TERMINEE", "Terminée"), Map.entry("INTERROMPUE", "Interrompue"),
            Map.entry("PLANIFIE", "Planifié"), Map.entry("CONFIRME", "Confirmé"), Map.entry("REALISE", "Réalisé"),
            Map.entry("A_VENIR", "À venir"), Map.entry("EN_RETARD", "En retard"));
    private static final Map<String, String> NIVEAUX = Map.of("URGENT", "Urgent", "ELEVE", "Élevé", "MODERE", "Modéré");
    private static final Map<String, String> TYPES_RDV = Map.of("CONSULTATION_PRENATALE", "Consultation prénatale",
            "ECHOGRAPHIE", "Échographie", "VACCINATION", "Vaccination", "EXAMEN_LABORATOIRE", "Bilan biologique",
            "AUTRE", "Autre");
    private static final Map<String, String> ISSUES = Map.of("NAISSANCE_VIVANTE", "Accouchement, enfant né vivant",
            "MORT_NE", "Accouchement, enfant mort-né", "FAUSSE_COUCHE", "Fausse couche ou avortement incomplet",
            "GROSSESSE_EXTRA_UTERINE", "Grossesse extra-utérine", "MORT_FOETALE_IN_UTERO", "Mort fœtale in utero",
            "INTERRUPTION_THERAPEUTIQUE", "Interruption thérapeutique");

    private final BaseFont regulier = police("fonts/Poppins-Regular.ttf");
    private final BaseFont gras = police("fonts/Poppins-SemiBold.ttf");

    public byte[] rendre(Dossier d) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 48, 54);
        PdfWriter writer = PdfWriter.getInstance(doc, sortie);
        writer.setPageEvent(new PiedDePage());
        doc.addTitle("Dossier de suivi de grossesse — YAY360");
        doc.addCreator("YAY360");
        doc.open();

        GrossesseDto g = d.grossesse();
        // ── En-tête
        Paragraph marque = new Paragraph(t("YAY360"), f(gras, 11, TEAL));
        doc.add(marque);
        doc.add(new Paragraph(t("Dossier de suivi de grossesse"), f(gras, 20, ENCRE)));
        doc.add(new Paragraph(t("Généré le " + d.genereLe().format(JOUR_HEURE) + " · Document confidentiel : données de santé"),
                f(regulier, 9, GRIS)));
        espace(doc, 10);

        // ── Patiente et grossesse
        titre(doc, "Patiente");
        PdfPTable identite = grille(2);
        ligne(identite, "Nom", d.patienteNomComplet());
        ligne(identite, "Date de naissance", jour(d.patienteDateNaissance()));
        ligne(identite, "Téléphone", d.patienteTelephone());
        ligne(identite, "Établissement de suivi", g.getEtablissementSuiviNom() != null ? g.getEtablissementSuiviNom() : "Non choisi");
        doc.add(identite);

        titre(doc, "Grossesse");
        PdfPTable grossesse = grille(2);
        ligne(grossesse, "Statut", libelle(STATUTS, g.getStatut()));
        if (g.getIssue() != null) {
            ligne(grossesse, "Issue", libelle(ISSUES, g.getIssue()) + (g.getDateFin() != null ? " (" + jour(g.getDateFin()) + ")" : ""));
        }
        ligne(grossesse, "Dernières règles", jour(g.getDateDernieresRegles()));
        ligne(grossesse, "Terme estimé", jour(g.getDatePrevueAccouchement()));
        if (g.getDebutPeriodeTerme() != null && g.getFinPeriodeTerme() != null) {
            ligne(grossesse, "Période à terme", "du " + jour(g.getDebutPeriodeTerme()) + " au " + jour(g.getFinPeriodeTerme())
                    + " (37 SA à 41 SA + 6 j)");
        }
        if ("EN_COURS".equals(g.getStatut())) {
            ligne(grossesse, "Âge gestationnel", g.getSemaineActuelle() + "e semaine (trimestre " + g.getTrimestre() + ")");
        }
        ligne(grossesse, "Grossesse à risque", Boolean.TRUE.equals(g.getGrossesseARisque())
                ? "Oui" + (g.getMotifRisque() != null ? " : " + g.getMotifRisque() : "") : "Non");
        ligne(grossesse, "Grossesses précédentes", g.getNombreGrossessesPrecedentes() != null
                ? String.valueOf(g.getNombreGrossessesPrecedentes()) : "Non renseigné");
        if (g.getAntecedents() != null && !g.getAntecedents().isBlank()) {
            ligne(grossesse, "Antécédents", g.getAntecedents());
        }
        doc.add(grossesse);

        // ── Plan des consultations prénatales
        if (d.planCpn() != null) {
            titre(doc, "Consultations prénatales recommandées (OMS)");
            doc.add(new Paragraph(t(d.planCpn().getNombreRealises() + " sur 8 réalisées · " + d.planCpn().getNombreEnRetard()
                    + " en retard"), f(regulier, 9, GRIS)));
            PdfPTable plan = tableau(new float[]{1, 1.2f, 1.6f, 1.6f}, "N°", "Semaine", "Date cible", "Statut");
            for (MoteurReglesCliniques.ContactCpn c : d.planCpn().getContacts()) {
                cellules(plan, String.valueOf(c.numero()), c.semaineCible() + " SA", jourCourt(c.dateCible()),
                        libelle(STATUTS, c.statut()) + (c.dateRealisation() != null ? " le " + jourCourt(c.dateRealisation()) : ""));
            }
            doc.add(plan);
        }

        // ── Consultations
        titre(doc, "Consultations (" + d.consultations().size() + ")");
        if (d.consultations().isEmpty()) {
            vide(doc, "Aucune consultation enregistrée.");
        } else {
            PdfPTable consultations = tableau(new float[]{1.25f, 1.05f, 0.8f, 0.75f, 0.65f, 0.75f, 0.8f, 1.75f},
                    "Date", "TA (mmHg)", "Poids", "T°", "HU", "BCF", "FC mère", "Bandelette");
            for (ConsultationDto c : d.consultations()) {
                cellules(consultations, jourCourt(c.getDateConsultation().toLocalDate()), val(c.getTensionArterielle()),
                        val(c.getPoidsKg(), " kg"), val(c.getTemperatureC(), " °C"), val(c.getHauteurUterineCm(), " cm"),
                        val(c.getBattementsCoeurFoetal(), "/min"), val(c.getFrequenceCardiaqueMaternelle(), "/min"),
                        bandelette(c.getProteinurie(), c.getGlycosurie()));
            }
            doc.add(consultations);
            for (ConsultationDto c : d.consultations()) {
                if (c.getSignauxCliniques() != null && !c.getSignauxCliniques().isEmpty()) {
                    Paragraph p = new Paragraph(t("Signaux du " + jourCourt(c.getDateConsultation().toLocalDate()) + " (" + c.getProfessionnelNomComplet() + ") : "
                            + String.join(" ; ", c.getSignauxCliniques().stream().map(DossierPdfRenderer::signal).toList())), f(regulier, 8, ORANGE));
                    p.setSpacingBefore(3);
                    doc.add(p);
                }
            }
        }

        // ── Alertes
        titre(doc, "Signalements et alertes (" + d.alertes().size() + ")");
        if (d.alertes().isEmpty()) {
            vide(doc, "Aucun signalement.");
        } else {
            PdfPTable alertes = tableau(new float[]{1.3f, 2.2f, 0.9f, 1.1f, 2.2f}, "Date", "Motif", "Niveau", "Statut", "Suivi");
            for (AlerteDto a : d.alertes()) {
                String suivi = a.getResoluParNomComplet() != null ? "Résolue par " + a.getResoluParNomComplet()
                        : a.getProfessionnelNomComplet() != null ? "Prise en charge par " + a.getProfessionnelNomComplet() : "—";
                cellules(alertes, jourCourt(a.getDateDeclaration().toLocalDate()), libelle(SYMPTOMES, a.getTypeSymptome()),
                        libelle(NIVEAUX, a.getNiveauGravite()), libelle(STATUTS, a.getStatut()), suivi);
            }
            doc.add(alertes);
        }

        // ── Rendez-vous à venir
        titre(doc, "Rendez-vous à venir (" + d.rendezVousAVenir().size() + ")");
        if (d.rendezVousAVenir().isEmpty()) {
            vide(doc, "Aucun rendez-vous à venir.");
        } else {
            PdfPTable rdv = tableau(new float[]{1.6f, 1.6f, 2.4f, 1.1f}, "Date", "Type", "Lieu", "Statut");
            for (RendezVousDto r : d.rendezVousAVenir()) {
                cellules(rdv, r.getDateHeure().format(JOUR_HEURE), libelle(TYPES_RDV, r.getType()),
                        r.getEtablissementNom() != null ? r.getEtablissementNom() : "—", libelle(STATUTS, r.getStatut()));
            }
            doc.add(rdv);
        }

        // ── Avertissement
        espace(doc, 14);
        PdfPTable cadre = new PdfPTable(1);
        cadre.setWidthPercentage(100);
        PdfPCell cellule = new PdfPCell(new Phrase(t(AVERTISSEMENT), f(regulier, 8.5f, ENCRE)));
        cellule.setBackgroundColor(TEAL_CLAIR);
        cellule.setBorderColor(TEAL);
        cellule.setPadding(8);
        cadre.addCell(cellule);
        doc.add(cadre);

        doc.close();
        return sortie.toByteArray();
    }

    // ==================== Outils de mise en page ====================

    private void titre(Document doc, String texte) {
        Paragraph p = new Paragraph(t(texte), f(gras, 12.5f, TEAL));
        p.setSpacingBefore(14);
        p.setSpacingAfter(5);
        doc.add(p);
    }

    private void vide(Document doc, String texte) {
        doc.add(new Paragraph(t(texte), f(regulier, 9, GRIS)));
    }

    private static void espace(Document doc, float hauteur) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(hauteur);
        doc.add(p);
    }

    private PdfPTable grille(int colonnes) {
        PdfPTable table = new PdfPTable(colonnes);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2.4f});
        return table;
    }

    private void ligne(PdfPTable table, String etiquette, String valeur) {
        PdfPCell a = new PdfPCell(new Phrase(t(etiquette), f(regulier, 9, GRIS)));
        PdfPCell b = new PdfPCell(new Phrase(t(valeur != null ? valeur : "—"), f(regulier, 9.5f, ENCRE)));
        for (PdfPCell c : new PdfPCell[]{a, b}) {
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(new Color(0xE4, 0xEE, 0xF0));
            c.setPadding(4);
            table.addCell(c);
        }
    }

    private PdfPTable tableau(float[] largeurs, String... entetes) {
        PdfPTable table = new PdfPTable(largeurs.length);
        table.setWidthPercentage(100);
        table.setWidths(largeurs);
        table.setHeaderRows(1);
        for (String e : entetes) {
            PdfPCell c = new PdfPCell(new Phrase(t(e), f(gras, 8.5f, Color.WHITE)));
            c.setBackgroundColor(TEAL);
            c.setBorderColor(TEAL);
            c.setPadding(4);
            table.addCell(c);
        }
        return table;
    }

    private void cellules(PdfPTable table, String... valeurs) {
        for (String v : valeurs) {
            PdfPCell c = new PdfPCell(new Phrase(t(v), f(regulier, 8.5f, ENCRE)));
            c.setBorderColor(new Color(0xD6, 0xE2, 0xE5));
            c.setPadding(4);
            table.addCell(c);
        }
    }

    /** Remplace les caractères absents de la police (ex. « ≥ ») pour ne jamais produire de glyphe vide. */
    String t(String texte) {
        if (texte == null) {
            return "—";
        }
        String s = texte.replace("≥", ">=").replace("≤", "<=");
        StringBuilder sb = new StringBuilder(s.length());
        s.codePoints().forEach(cp -> sb.appendCodePoint(cp == '\n' || regulier.charExists(cp) ? cp : '?'));
        return sb.toString();
    }

    private static Font f(BaseFont base, float taille, Color couleur) {
        return new Font(base, taille, Font.NORMAL, couleur);
    }

    private static String libelle(Map<String, String> libelles, String code) {
        return code == null ? "—" : libelles.getOrDefault(code, code);
    }

    private static String jour(LocalDate date) {
        return date == null ? "—" : date.format(JOUR);
    }

    private static String jourCourt(LocalDate date) {
        return date == null ? "—" : date.format(JOUR_COURT);
    }

    private static String val(Object valeur) {
        return valeur == null ? "—" : String.valueOf(valeur);
    }

    private static String val(Number valeur, String unite) {
        if (valeur == null) {
            return "—";
        }
        String texte = valeur instanceof Double d && d == Math.rint(d) ? String.valueOf(d.longValue()) : String.valueOf(valeur);
        return texte.replace('.', ',') + unite;
    }

    /** « [ELEVE] message » → « Élevé : message ». */
    static String signal(String brut) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\[(URGENT|ELEVE|MODERE)]\\s*(.*)$", java.util.regex.Pattern.DOTALL).matcher(brut);
        return m.matches() ? NIVEAUX.get(m.group(1)) + " : " + m.group(2) : brut;
    }

    private static String bandelette(String proteinurie, String glycosurie) {
        if (proteinurie == null && glycosurie == null) {
            return "—";
        }
        return "Prot. " + (proteinurie != null ? ResultatBandelette.libelle(proteinurie) : "—")
                + " / Gly. " + (glycosurie != null ? ResultatBandelette.libelle(glycosurie) : "—");
    }

    private static BaseFont police(String chemin) {
        try (InputStream in = new ClassPathResource(chemin).getInputStream()) {
            byte[] octets = in.readAllBytes();
            String nom = chemin.substring(chemin.lastIndexOf('/') + 1);
            return BaseFont.createFont(nom, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, octets, null);
        } catch (IOException e) {
            throw new UncheckedIOException("Police introuvable : " + chemin, e);
        }
    }

    /** Pied de page : mention de confidentialité et numéro de page. */
    private final class PiedDePage extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(t("YAY360 · Dossier confidentiel, à ne transmettre qu'à un professionnel de santé"), f(regulier, 7.5f, GRIS)),
                    document.left(), document.bottom() - 22, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(t("Page " + writer.getPageNumber()), f(regulier, 7.5f, GRIS)),
                    document.right(), document.bottom() - 22, 0);
        }
    }
}
