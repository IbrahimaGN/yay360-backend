package sn.yay360.api.helper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sn.yay360.api.model.NiveauGravite;
import sn.yay360.api.model.ResultatBandelette;
import sn.yay360.api.model.TypeSymptome;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Moteur de règles cliniques explicables : la dimension « intelligente » de YAY360.
 *
 * <p>Chaque règle est nommée, sourcée et produit une justification lisible par le soignant.
 * Le moteur n'établit aucun diagnostic et ne décide jamais à la place du médecin : il signale,
 * hiérarchise et suggère.</p>
 *
 * <p>Références :</p>
 * <ul>
 *   <li>OMS (2016), <i>Recommandations concernant les soins prénatals pour que la grossesse soit
 *       une expérience positive</i> : modèle à 8 contacts (12, 20, 26, 30, 34, 36, 38 et 40 SA).</li>
 *   <li>Hypertension gravidique : PA ≥ 140/90 mmHg ; forme sévère : PA ≥ 160/110 mmHg.</li>
 *   <li>Rythme cardiaque fœtal normal : 110 à 160 battements/min.</li>
 *   <li>Règle de McDonald : entre 24 et 38 SA, hauteur utérine (cm) ≈ âge gestationnel (SA) ± 3 cm.</li>
 *   <li>Signes d'alerte de la prééclampsie après 20 SA : céphalées intenses, troubles visuels, œdèmes brutaux.</li>
 * </ul>
 */
@Component
public class MoteurReglesCliniques {

    /** Semaines cibles des 8 contacts prénatals recommandés par l'OMS. */
    public static final List<Integer> CONTACTS_CPN_OMS = List.of(12, 20, 26, 30, 34, 36, 38, 40);

    /** Tolérance avant de considérer un contact comme manqué. */
    static final int TOLERANCE_RETARD_SEMAINES = 2;

    private static final List<String> ORDRE_GRAVITE =
            List.of(NiveauGravite.MODERE, NiveauGravite.ELEVE, NiveauGravite.URGENT);

    private static final Set<String> SIGNES_PREECLAMPSIE = Set.of(
            TypeSymptome.MAUX_DE_TETE_INTENSES, TypeSymptome.TROUBLES_VISUELS, TypeSymptome.OEDEMES_SOUDAINS);

    /** Mots-clés recherchés dans les antécédents (sans accents, en minuscules). */
    private static final Map<String, String> ANTECEDENTS_A_RISQUE = Map.ofEntries(
            Map.entry("cesarienne", "Utérus cicatriciel (antécédent de césarienne)"),
            Map.entry("hypertension", "Antécédent d'hypertension artérielle"),
            Map.entry("hta", "Antécédent d'hypertension artérielle"),
            Map.entry("eclampsie", "Antécédent de prééclampsie ou d'éclampsie"),
            Map.entry("diabete", "Diabète"),
            Map.entry("drepanocytose", "Drépanocytose"),
            Map.entry("fausse couche", "Antécédent de fausse couche"),
            Map.entry("prematur", "Antécédent d'accouchement prématuré"),
            Map.entry("mort-ne", "Antécédent de mort fœtale"),
            Map.entry("mort ne", "Antécédent de mort fœtale"),
            Map.entry("hemorragie", "Antécédent d'hémorragie obstétricale"),
            Map.entry("cardiopathie", "Cardiopathie"),
            Map.entry("vih", "Infection à VIH")
    );

    // ==================== 1. ANALYSE DES CONSTANTES D'UNE CONSULTATION ====================

    public record SignalClinique(String code, String niveau, String message) {
        @Override
        public String toString() {
            return "[" + niveau + "] " + message;
        }
    }

    /**
     * Analyse les constantes saisies lors d'une consultation prénatale.
     *
     * @param poidsPrecedentKg poids de la consultation précédente (ou null)
     * @param joursDepuisPrecedente jours écoulés depuis cette consultation (ou null)
     */
    public List<SignalClinique> analyserConstantes(int semaine, String tensionArterielle, Double temperatureC,
                                                   Integer battementsCoeurFoetal, Double hauteurUterineCm,
                                                   Double poidsKg, Double poidsPrecedentKg,
                                                   Long joursDepuisPrecedente) {
        List<SignalClinique> signaux = new ArrayList<>();

        int[] pa = lireTension(tensionArterielle);
        if (pa != null) {
            boolean apres20SA = semaine >= 20;
            if (pa[0] >= 160 || pa[1] >= 110) {
                signaux.add(new SignalClinique("HTA_SEVERE", NiveauGravite.URGENT,
                        "Pression artérielle " + tensionArterielle + " mmHg ≥ 160/110 : hypertension sévère"
                                + (apres20SA ? ", suspicion de prééclampsie sévère" : "")
                                + ". Évaluation médicale immédiate."));
            } else if (pa[0] >= 140 || pa[1] >= 90) {
                signaux.add(new SignalClinique("HTA", NiveauGravite.ELEVE,
                        "Pression artérielle " + tensionArterielle + " mmHg ≥ 140/90 : hypertension"
                                + (apres20SA ? " gravidique, rechercher une protéinurie (prééclampsie)" : "")
                                + "."));
            }
        }

        if (temperatureC != null) {
            if (temperatureC >= 39.5) {
                signaux.add(new SignalClinique("FIEVRE_ELEVEE", NiveauGravite.URGENT,
                        "Température " + temperatureC + " °C ≥ 39,5 °C : fièvre élevée, rechercher une infection."));
            } else if (temperatureC >= 38.0) {
                signaux.add(new SignalClinique("FIEVRE", NiveauGravite.ELEVE,
                        "Température " + temperatureC + " °C ≥ 38 °C : fièvre, rechercher une infection."));
            }
        }

        if (battementsCoeurFoetal != null && semaine >= 12
                && (battementsCoeurFoetal < 110 || battementsCoeurFoetal > 160)) {
            signaux.add(new SignalClinique("RCF_ANORMAL", NiveauGravite.URGENT,
                    "Rythme cardiaque fœtal " + battementsCoeurFoetal + "/min hors de la plage normale (110–160/min)."));
        }

        if (hauteurUterineCm != null && semaine >= 24 && semaine <= 38
                && Math.abs(hauteurUterineCm - semaine) > 3) {
            String sens = hauteurUterineCm < semaine ? "inférieure" : "supérieure";
            signaux.add(new SignalClinique("HU_DISCORDANTE", NiveauGravite.MODERE,
                    "Hauteur utérine " + hauteurUterineCm + " cm " + sens + " à l'attendu (≈ " + semaine
                            + " cm à " + semaine + " SA) : contrôle échographique conseillé"
                            + (hauteurUterineCm < semaine ? " (retard de croissance ?)." : " (macrosomie, hydramnios ?).")));
        }

        if (poidsKg != null && poidsPrecedentKg != null && joursDepuisPrecedente != null
                && joursDepuisPrecedente >= 5 && semaine >= 20) {
            double kgParSemaine = (poidsKg - poidsPrecedentKg) * 7.0 / joursDepuisPrecedente;
            if (kgParSemaine > 1.0) {
                signaux.add(new SignalClinique("PRISE_POIDS_RAPIDE", NiveauGravite.MODERE,
                        String.format(Locale.FRANCE, "Prise de poids de %.1f kg/semaine depuis la dernière consultation : "
                                + "rechercher des œdèmes (rétention hydrique, prééclampsie).", kgParSemaine)));
            }
        }
        return signaux;
    }

    // ==================== 1 bis. CONSTANTES COMPLÉMENTAIRES ====================

    /**
     * Règles sur les constantes demandées par la sage-femme interrogée (fréquence cardiaque maternelle,
     * protéinurie, glycosurie). Leurs seuils reprennent des recommandations publiées (voir
     * {@link #analyserConstantesComplementaires}). Elles peuvent être coupées par configuration
     * ({@code yay360.regles.complementaires.actives=false}) si un protocole local les contredit :
     * les valeurs sont alors seulement enregistrées et affichées.
     */
    @Value("${yay360.regles.complementaires.actives:true}")
    private boolean reglesComplementairesActives = true;

    void activerReglesComplementaires(boolean actives) {
        this.reglesComplementairesActives = actives;
    }

    public boolean reglesComplementairesActives() {
        return reglesComplementairesActives;
    }

    /**
     * Seuils et sources :
     * <ul>
     *   <li><b>Prééclampsie suspectée (URGENT)</b> : hypertension ≥ 140/90 mmHg à partir de 20 SA et
     *       protéinurie ≥ ++ à la bandelette (ISSHP ; ++ est le seuil retenu pour la bandelette quand le
     *       rapport protéines/créatinine n'est pas disponible).</li>
     *   <li><b>Protéinurie + avec hypertension à partir de 20 SA (ÉLEVÉ)</b> : une croix ne suffit pas à
     *       conclure ; quantification recommandée (rapport protéines/créatinine).</li>
     *   <li><b>Protéinurie ≥ ++ sans hypertension (MODÉRÉ)</b> : protéinurie significative à quantifier,
     *       surveillance de la pression artérielle.</li>
     *   <li><b>Glycosurie (MODÉRÉ)</b> : ≥ ++ une fois, ou ≥ + à deux consultations ou plus : test de
     *       dépistage du diabète gestationnel (NICE NG3). La bandelette ne dépiste pas elle-même le diabète
     *       gestationnel.</li>
     *   <li><b>Fréquence cardiaque maternelle</b> (bandes du score MEOWS recommandé par la CEMACH) :
     *       &gt; 120 ou &lt; 40/min : ÉLEVÉ ; 101 à 120 ou 40 à 49/min : MODÉRÉ.</li>
     * </ul>
     *
     * @param glycosuriesPositivesAnterieures consultations précédentes de la grossesse avec glycosurie ≥ +
     */
    public List<SignalClinique> analyserConstantesComplementaires(int semaine, String tensionArterielle,
                                                                  Integer frequenceCardiaqueMaternelle,
                                                                  String proteinurie, String glycosurie,
                                                                  long glycosuriesPositivesAnterieures) {
        List<SignalClinique> signaux = new ArrayList<>();
        if (!reglesComplementairesActives) {
            return signaux;
        }
        int[] pa = lireTension(tensionArterielle);
        boolean hypertensionApres20SA = pa != null && (pa[0] >= 140 || pa[1] >= 90) && semaine >= 20;
        String prot = ResultatBandelette.libelle(proteinurie);

        if (hypertensionApres20SA && ResultatBandelette.auMoins(proteinurie, ResultatBandelette.DEUX_CROIX)) {
            signaux.add(new SignalClinique("PREECLAMPSIE_SUSPECTEE", NiveauGravite.URGENT,
                    "Pression artérielle " + tensionArterielle + " mmHg ≥ 140/90 et protéinurie " + prot
                            + " à " + semaine + " SA : suspicion de prééclampsie (critères ISSHP). "
                            + "Évaluation médicale immédiate."));
        } else if (hypertensionApres20SA && ResultatBandelette.auMoins(proteinurie, ResultatBandelette.UNE_CROIX)) {
            signaux.add(new SignalClinique("PROTEINURIE_A_QUANTIFIER", NiveauGravite.ELEVE,
                    "Pression artérielle " + tensionArterielle + " mmHg ≥ 140/90 et protéinurie " + prot
                            + " à " + semaine + " SA : quantifier la protéinurie (rapport protéines/créatinine) "
                            + "pour confirmer ou écarter une prééclampsie."));
        } else if (ResultatBandelette.auMoins(proteinurie, ResultatBandelette.DEUX_CROIX)) {
            signaux.add(new SignalClinique("PROTEINURIE", NiveauGravite.MODERE,
                    "Protéinurie " + prot + " à la bandelette sans hypertension : quantifier la protéinurie "
                            + "et surveiller la pression artérielle."));
        }

        boolean glycosurieForte = ResultatBandelette.auMoins(glycosurie, ResultatBandelette.DEUX_CROIX);
        boolean glycosurieRepetee = ResultatBandelette.auMoins(glycosurie, ResultatBandelette.UNE_CROIX)
                && glycosuriesPositivesAnterieures >= 1;
        if (glycosurieForte || glycosurieRepetee) {
            signaux.add(new SignalClinique("GLYCOSURIE", NiveauGravite.MODERE,
                    "Glycosurie " + ResultatBandelette.libelle(glycosurie)
                            + (glycosurieForte ? "" : ", déjà positive à " + glycosuriesPositivesAnterieures
                                    + " consultation(s) précédente(s)")
                            + " : proposer un test de dépistage du diabète gestationnel (NICE NG3)."));
        }

        if (frequenceCardiaqueMaternelle != null) {
            int fc = frequenceCardiaqueMaternelle;
            if (fc > 120 || fc < 40) {
                signaux.add(new SignalClinique("FC_MATERNELLE_ANORMALE", NiveauGravite.ELEVE,
                        "Fréquence cardiaque maternelle " + fc + "/min " + (fc > 120 ? "> 120" : "< 40")
                                + "/min (seuil rouge MEOWS) : examen clinique rapide (hémorragie, infection, "
                                + "trouble du rythme ?)."));
            } else if (fc > 100 || fc < 50) {
                signaux.add(new SignalClinique("FC_MATERNELLE_LIMITE", NiveauGravite.MODERE,
                        "Fréquence cardiaque maternelle " + fc + "/min "
                                + (fc > 100 ? "entre 101 et 120" : "entre 40 et 49")
                                + "/min (seuil jaune MEOWS) : recontrôler et rechercher fièvre, anémie ou déshydratation."));
            }
        }
        return signaux;
    }

    public static String niveauMax(List<SignalClinique> signaux) {
        return signaux.stream().map(SignalClinique::niveau)
                .max(Comparator.comparingInt(ORDRE_GRAVITE::indexOf))
                .orElse(null);
    }

    /**
     * Lit « 120/80 » (mmHg) en [120, 80] ; null si la valeur est absente ou physiologiquement
     * incohérente (saisie en cmHg comme « 16/11 », valeurs inversées…).
     */
    public static int[] lireTension(String tension) {
        if (tension == null || !tension.matches("\\d{2,3}/\\d{2,3}")) {
            return null;
        }
        String[] parties = tension.split("/");
        int systolique = Integer.parseInt(parties[0]);
        int diastolique = Integer.parseInt(parties[1]);
        boolean plausible = systolique >= 60 && systolique <= 260
                && diastolique >= 30 && diastolique <= 160
                && systolique > diastolique;
        return plausible ? new int[]{systolique, diastolique} : null;
    }

    // ==================== 2. TRIAGE CONTEXTUALISÉ DES SYMPTÔMES ====================

    public record ResultatTriage(String niveau, List<String> justifications) {
    }

    /** Niveau de base de chaque symptôme (cahier des charges §4.7), avant ajustement au contexte. */
    private static final Map<String, String> NIVEAU_DE_BASE = Map.ofEntries(
            Map.entry(TypeSymptome.SAIGNEMENTS, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.DOULEURS_ABDOMINALES, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.PERTE_LIQUIDE, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.DIMINUTION_MOUVEMENTS_BEBE, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.FIEVRE_ELEVEE, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.CONVULSIONS, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.DIFFICULTES_RESPIRATOIRES, NiveauGravite.URGENT),
            Map.entry(TypeSymptome.MAUX_DE_TETE_INTENSES, NiveauGravite.ELEVE),
            Map.entry(TypeSymptome.OEDEMES_SOUDAINS, NiveauGravite.ELEVE),
            Map.entry(TypeSymptome.TROUBLES_VISUELS, NiveauGravite.ELEVE),
            Map.entry(TypeSymptome.VOMISSEMENTS_SEVERES, NiveauGravite.MODERE),
            Map.entry(TypeSymptome.AUTRE, NiveauGravite.MODERE)
    );

    /**
     * Point d'entrée unique du triage d'un symptôme déclaré : niveau de base de la table §4.7,
     * puis ajustements au contexte de la grossesse ({@link #trier}).
     *
     * @param alertesSeptDerniersJours alertes déjà déclarées sur cette grossesse ces 7 derniers jours
     */
    public ResultatTriage trierSymptome(String typeSymptome, int semaine, boolean grossesseARisque,
                                        long alertesSeptDerniersJours) {
        return trier(typeSymptome, NIVEAU_DE_BASE.getOrDefault(typeSymptome, NiveauGravite.MODERE),
                semaine, grossesseARisque, alertesSeptDerniersJours);
    }

    /**
     * Triage d'un symptôme déclaré : niveau de base (table du cahier des charges §4.7), ajusté
     * selon le contexte de la grossesse. Chaque ajustement est justifié.
     *
     * @param alertesSeptDerniersJours alertes déjà déclarées sur cette grossesse ces 7 derniers jours
     */
    ResultatTriage trier(String typeSymptome, String niveauDeBase, int semaine, boolean grossesseARisque,
                         long alertesSeptDerniersJours) {
        List<String> justifications = new ArrayList<>();
        String niveau = niveauDeBase;
        justifications.add("Niveau de base pour « " + typeSymptome + " » : " + niveauDeBase + ".");

        if (SIGNES_PREECLAMPSIE.contains(typeSymptome) && semaine >= 20 && !NiveauGravite.URGENT.equals(niveau)) {
            niveau = NiveauGravite.URGENT;
            justifications.add("Signe d'alerte de prééclampsie après 20 SA (" + semaine + " SA) : passage en URGENT.");
        }
        if (TypeSymptome.PERTE_LIQUIDE.equals(typeSymptome) && semaine < 37) {
            justifications.add("Perte de liquide avant 37 SA : rupture prématurée des membranes possible.");
        }
        if (grossesseARisque && !NiveauGravite.URGENT.equals(niveau)) {
            niveau = superieur(niveau);
            justifications.add("Grossesse classée à risque : niveau relevé à " + niveau + ".");
        }
        if (alertesSeptDerniersJours >= 2 && !NiveauGravite.URGENT.equals(niveau)) {
            niveau = superieur(niveau);
            justifications.add("Signalements répétés (" + (alertesSeptDerniersJours + 1)
                    + " en 7 jours) : niveau relevé à " + niveau + ".");
        }
        return new ResultatTriage(niveau, justifications);
    }

    static String superieur(String niveau) {
        int index = ORDRE_GRAVITE.indexOf(niveau);
        return ORDRE_GRAVITE.get(Math.min(index + 1, ORDRE_GRAVITE.size() - 1));
    }

    // ==================== 3. PLAN DES CONSULTATIONS PRÉNATALES (OMS) ====================

    public record ContactCpn(int numero, int semaineCible, LocalDate dateCible, String statut,
                             LocalDate dateRealisation) {
    }

    public static final String CPN_REALISE = "REALISE";
    public static final String CPN_PLANIFIE = "PLANIFIE";
    public static final String CPN_A_VENIR = "A_VENIR";
    public static final String CPN_EN_RETARD = "EN_RETARD";

    /**
     * Construit le plan des 8 contacts OMS et rapproche les consultations réalisées et les
     * rendez-vous planifiés. Un contact est réalisé par une consultation faite dans sa fenêtre
     * (après le contact précédent, au plus tard 2 semaines après la semaine cible).
     */
    public List<ContactCpn> planCpn(LocalDate dateDernieresRegles, List<LocalDate> datesConsultations,
                                    List<LocalDate> datesRendezVousActifs, LocalDate aujourdhui) {
        int semaineActuelle = (int) (ChronoUnit.DAYS.between(dateDernieresRegles, aujourdhui) / 7);

        // Chaque consultation valide le plus ancien contact non encore réalisé, à condition que la
        // fenêtre de ce contact soit ouverte (cible − 2 SA ; dès le début pour le 1er contact) :
        //  - une première consultation tardive (16 SA) valide bien le contact n°1 ;
        //  - une consultation ne valide jamais par anticipation un contact futur.
        LocalDate[] realisations = new LocalDate[CONTACTS_CPN_OMS.size()];
        for (LocalDate consultation : datesConsultations.stream().sorted().toList()) {
            for (int i = 0; i < CONTACTS_CPN_OMS.size(); i++) {
                if (realisations[i] == null) {
                    if (!consultation.isBefore(ouverture(dateDernieresRegles, i))) {
                        realisations[i] = consultation;
                    }
                    break; // seul le plus ancien contact non réalisé est candidat
                }
            }
        }

        List<ContactCpn> plan = new ArrayList<>();
        for (int i = 0; i < CONTACTS_CPN_OMS.size(); i++) {
            int cible = CONTACTS_CPN_OMS.get(i);
            LocalDate debut = ouverture(dateDernieresRegles, i);
            LocalDate fin = dateDernieresRegles.plusWeeks(cible + (long) TOLERANCE_RETARD_SEMAINES);
            LocalDate dateCible = dateDernieresRegles.plusWeeks(cible);
            LocalDate realisee = realisations[i];

            String statut;
            if (realisee != null) {
                statut = CPN_REALISE;
            } else if (datesRendezVousActifs.stream().anyMatch(d -> !d.isBefore(debut) && d.isBefore(fin)
                    && !d.isBefore(aujourdhui))) {
                statut = CPN_PLANIFIE;
            } else if (semaineActuelle >= cible + TOLERANCE_RETARD_SEMAINES) {
                statut = CPN_EN_RETARD;
            } else {
                statut = CPN_A_VENIR;
            }
            plan.add(new ContactCpn(i + 1, cible, dateCible, statut, realisee));
        }
        return plan;
    }

    private static LocalDate ouverture(LocalDate dateDernieresRegles, int indexContact) {
        return indexContact == 0 ? dateDernieresRegles
                : dateDernieresRegles.plusWeeks(CONTACTS_CPN_OMS.get(indexContact) - (long) TOLERANCE_RETARD_SEMAINES);
    }

    // ==================== 4. FACTEURS DE RISQUE (SUGGESTION AU MÉDECIN) ====================

    /**
     * Facteurs de risque détectés à partir du dossier. Simple aide à la décision :
     * seul le médecin peut classer une grossesse à risque (§4.2).
     */
    public List<String> facteursDeRisque(LocalDate dateNaissanceMere, LocalDate dateDernieresRegles,
                                         Integer nombreGrossessesPrecedentes, String antecedents,
                                         List<String> codesSignauxConsultations, long alertesUrgentes) {
        List<String> facteurs = new ArrayList<>();
        if (dateNaissanceMere != null) {
            int age = Period.between(dateNaissanceMere, dateDernieresRegles).getYears();
            if (age < 18) {
                facteurs.add("Grossesse précoce (" + age + " ans)");
            } else if (age > 35) {
                facteurs.add("Âge maternel avancé (" + age + " ans)");
            }
        }
        if (nombreGrossessesPrecedentes != null && nombreGrossessesPrecedentes >= 5) {
            facteurs.add("Grande multiparité (" + nombreGrossessesPrecedentes + " grossesses antérieures)");
        }
        if (antecedents != null && !antecedents.isBlank()) {
            String texte = sansAccents(antecedents);
            ANTECEDENTS_A_RISQUE.forEach((motCle, libelle) -> {
                if (texte.contains(motCle) && !facteurs.contains(libelle)) {
                    facteurs.add(libelle);
                }
            });
        }
        if (codesSignauxConsultations.stream().anyMatch(c -> c.startsWith("HTA"))) {
            facteurs.add("Hypertension relevée en consultation");
        }
        if (codesSignauxConsultations.contains("RCF_ANORMAL")) {
            facteurs.add("Rythme cardiaque fœtal anormal relevé en consultation");
        }
        if (alertesUrgentes >= 2) {
            facteurs.add(alertesUrgentes + " alertes urgentes signalées pendant la grossesse");
        }
        return facteurs;
    }

    static String sansAccents(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
