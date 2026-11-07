package sn.yay360.api.config;

import sn.yay360.api.helper.MoteurReglesCliniques;
import sn.yay360.api.model.AlerteMedicale;
import sn.yay360.api.model.Consultation;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.Notification;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutAlerte;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.StatutRendezVous;
import sn.yay360.api.model.TypeNotification;
import sn.yay360.api.model.TypeRendezVous;
import sn.yay360.api.model.TypeSymptome;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.NotificationRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Jeu de données de DÉMONSTRATION, entièrement fictif (activé par YAY360_DEMO_DATA_ENABLED, jamais en production).
 *
 * <ul>
 *   <li>6 médecins et 6 sages-femmes répartis dans les 5 établissements ;</li>
 *   <li>10 patientes, chacune avec son conjoint, et leurs grossesses (dont 3 à risque, 2 grossesses passées) ;</li>
 *   <li>consultations prénatales aux semaines du calendrier OMS, rendez-vous passés et à venir, alertes, notifications.</li>
 * </ul>
 *
 * Les noms sont fictifs. Les constantes de consultation sont des valeurs de démonstration sans valeur médicale :
 * leurs signaux, les niveaux de gravité et les justifications des alertes sont calculés par le
 * {@link MoteurReglesCliniques}, exactement comme pour une saisie réelle (rien n'est choisi à la main).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JeuDeDonneesDemo {

    /** Tous les comptes fictifs se terminent par ce suffixe (facile à retrouver ou à supprimer). */
    static final String SUFFIXE_EMAIL = ".demo@yay360.sn";
    /** Compte témoin : s'il existe, le jeu est déjà chargé. */
    static final String EMAIL_TEMOIN = "awa.ndiaye" + SUFFIXE_EMAIL;

    private static final String MENTION = "Donnée fictive de démonstration.";
    private static final String ORIGINE_PATIENTE = "PATIENTE";
    private static final String ORIGINE_ANALYSE = "ANALYSE_AUTOMATIQUE";
    private static final String TYPE_CONSTANTES_ANORMALES = "CONSTANTES_ANORMALES";
    private static final DateTimeFormatter JOUR_HEURE = DateTimeFormatter.ofPattern("dd/MM 'à' HH'h'mm", Locale.FRENCH);

    /** Ordre des établissements (créés par {@link DataInitializer}). */
    static final List<String> ETABLISSEMENTS = List.of(
            "Hôpital Principal de Dakar",
            "Centre de Santé Roi Baudouin",
            "Hôpital Régional de Thiès",
            "Centre de Santé de Mbour",
            "Poste de Santé de Ngor");

    private record Personne(String prenom, String nom, LocalDate naissance) {
    }

    /** Un soignant et l'index de son établissement dans {@link #ETABLISSEMENTS}. */
    private record ProfilSoignant(Personne personne, int etablissement) {
    }

    /**
     * Une patiente, son conjoint et sa grossesse en cours.
     *
     * @param etablissement index dans {@link #ETABLISSEMENTS}, ou -1 si aucun établissement choisi
     * @param motifRisque   motif posé par le médecin (grossesse à risque), ou null
     */
    private record ProfilPatiente(Personne femme, Personne conjoint, int semaines, int etablissement,
                                  int grossessesPrecedentes, double poidsInitialKg, String motifRisque,
                                  boolean grossessePassee) {
        boolean aRisque() {
            return motifRisque != null;
        }
    }

    private static final List<ProfilSoignant> MEDECINS = List.of(
            new ProfilSoignant(new Personne("Mamadou", "Diallo", LocalDate.of(1975, 3, 14)), 0),
            new ProfilSoignant(new Personne("Aminata", "Sow", LocalDate.of(1982, 7, 2)), 0),
            new ProfilSoignant(new Personne("Cheikh", "Ndiaye", LocalDate.of(1979, 11, 21)), 1),
            new ProfilSoignant(new Personne("Fatou Kiné", "Fall", LocalDate.of(1986, 1, 30)), 2),
            new ProfilSoignant(new Personne("Ousmane", "Sarr", LocalDate.of(1971, 5, 9)), 3),
            new ProfilSoignant(new Personne("Mariama", "Ba", LocalDate.of(1988, 9, 17)), 4));

    private static final List<ProfilSoignant> SAGES_FEMMES = List.of(
            new ProfilSoignant(new Personne("Ndèye Fatou", "Diop", LocalDate.of(1990, 4, 11)), 0),
            new ProfilSoignant(new Personne("Khady", "Mbaye", LocalDate.of(1987, 12, 3)), 0),
            new ProfilSoignant(new Personne("Aïssatou", "Faye", LocalDate.of(1992, 6, 25)), 1),
            new ProfilSoignant(new Personne("Coumba", "Guèye", LocalDate.of(1985, 2, 14)), 2),
            new ProfilSoignant(new Personne("Seynabou", "Diouf", LocalDate.of(1993, 10, 8)), 3),
            new ProfilSoignant(new Personne("Astou", "Cissé", LocalDate.of(1989, 8, 19)), 4));

    private static final List<ProfilPatiente> PATIENTES = List.of(
            new ProfilPatiente(new Personne("Awa", "Ndiaye", LocalDate.of(1996, 5, 12)),
                    new Personne("Ibrahima", "Fall", LocalDate.of(1990, 2, 3)), 24, 0, 1, 62.0,
                    "Tension artérielle élevée relevée en consultation. " + MENTION, false),
            new ProfilPatiente(new Personne("Fatou", "Diagne", LocalDate.of(2000, 9, 23)),
                    new Personne("Abdoulaye", "Diop", LocalDate.of(1995, 7, 15)), 8, 0, 0, 55.0, null, false),
            new ProfilPatiente(new Personne("Mariama", "Sy", LocalDate.of(1989, 1, 8)),
                    new Personne("Modou", "Guèye", LocalDate.of(1984, 11, 30)), 32, 1, 2, 68.0,
                    "Fièvre relevée en consultation. " + MENTION, false),
            new ProfilPatiente(new Personne("Aminata", "Thiam", LocalDate.of(1998, 3, 17)),
                    new Personne("Babacar", "Sène", LocalDate.of(1993, 4, 22)), 16, 1, 1, 59.0, null, false),
            new ProfilPatiente(new Personne("Khadija", "Ba", LocalDate.of(1987, 12, 1)),
                    new Personne("Pape", "Ndao", LocalDate.of(1982, 8, 9)), 36, 2, 3, 71.0, null, true),
            new ProfilPatiente(new Personne("Ndèye Sokhna", "Mbengue", LocalDate.of(2001, 6, 29)),
                    new Personne("Alioune Badara", "Mboup", LocalDate.of(1996, 1, 18)), 12, 2, 0, 53.0, null, false),
            new ProfilPatiente(new Personne("Rokhaya", "Seck", LocalDate.of(1993, 10, 4)),
                    new Personne("Serigne", "Faye", LocalDate.of(1988, 3, 12)), 28, 3, 1, 64.0,
                    "Hauteur utérine discordante relevée en consultation. " + MENTION, false),
            new ProfilPatiente(new Personne("Bineta", "Kane", LocalDate.of(1995, 2, 20)),
                    new Personne("Lamine", "Diatta", LocalDate.of(1991, 12, 27)), 20, 3, 2, 60.0, null, true),
            new ProfilPatiente(new Personne("Dieynaba", "Touré", LocalDate.of(1994, 8, 14)),
                    new Personne("Omar", "Sall", LocalDate.of(1989, 6, 5)), 38, 4, 1, 66.0, null, false),
            new ProfilPatiente(new Personne("Coumba", "Ndour", LocalDate.of(2002, 4, 2)),
                    new Personne("El Hadji", "Kébé", LocalDate.of(1997, 9, 11)), 6, -1, 0, 52.0, null, false));

    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementSanteRepository etablissementSanteRepository;
    private final GrossesseRepository grossesseRepository;
    private final RendezVousRepository rendezVousRepository;
    private final ConsultationRepository consultationRepository;
    private final AlerteMedicaleRepository alerteMedicaleRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final MoteurReglesCliniques moteur;
    private final JdbcTemplate jdbcTemplate;

    @Value("${yay360.demo-data.password:}")
    private String motDePasseDemo;

    /** Compteur des numéros de téléphone fictifs. */
    private int numeroTelephone;

    /** Contexte d'une patiente pendant le chargement. */
    private record Dossier(ProfilPatiente profil, Utilisateur femme, Utilisateur conjoint, Grossesse grossesse,
                           EtablissementSante etablissement, Utilisateur medecin, Utilisateur sageFemme) {
    }

    public void charger() {
        if (utilisateurRepository.existsByEmailIgnoreCase(EMAIL_TEMOIN)) {
            log.info("[YAY360] Jeu de démonstration déjà présent : aucun ajout");
            return;
        }
        if (motDePasseDemo == null || motDePasseDemo.isBlank()) {
            motDePasseDemo = DataInitializer.genererMotDePasse();
            log.warn("[YAY360] Mot de passe des comptes de démonstration généré : {} "
                    + "(définissez YAY360_DEMO_PASSWORD pour le fixer)", motDePasseDemo);
        }
        numeroTelephone = 0;

        List<EtablissementSante> etablissements = etablissementsParOrdre();
        List<Utilisateur> medecins = creerSoignants(MEDECINS, RoleUtilisateur.MEDECIN, etablissements);
        List<Utilisateur> sagesFemmes = creerSoignants(SAGES_FEMMES, RoleUtilisateur.SAGE_FEMME, etablissements);

        List<Dossier> dossiers = new ArrayList<>();
        for (int i = 0; i < PATIENTES.size(); i++) {
            dossiers.add(creerDossier(i, PATIENTES.get(i), etablissements, medecins, sagesFemmes));
        }
        for (int i = 0; i < dossiers.size(); i++) {
            creerSuiviPrenatal(i, dossiers.get(i));
        }
        creerRendezVousAVenir(dossiers);
        creerSignalements(dossiers);

        log.info("[YAY360] Jeu de démonstration fictif chargé : {} médecins, {} sages-femmes, {} patientes et {} conjoints "
                        + "(comptes *{}, mot de passe YAY360_DEMO_PASSWORD)",
                medecins.size(), sagesFemmes.size(), dossiers.size(), dossiers.size(), SUFFIXE_EMAIL);
    }

    // ==================== COMPTES ====================

    private List<EtablissementSante> etablissementsParOrdre() {
        Map<String, EtablissementSante> parNom = new HashMap<>();
        etablissementSanteRepository.findAll().forEach(e -> parNom.put(e.getNom(), e));
        List<EtablissementSante> liste = new ArrayList<>();
        ETABLISSEMENTS.forEach(nom -> liste.add(parNom.get(nom)));
        return liste;
    }

    private List<Utilisateur> creerSoignants(List<ProfilSoignant> profils, String role,
                                             List<EtablissementSante> etablissements) {
        List<Utilisateur> soignants = new ArrayList<>();
        for (ProfilSoignant profil : profils) {
            Utilisateur soignant = creerCompte(profil.personne(), role, null);
            soignants.add(soignant);
            EtablissementSante etablissement = etablissements.get(profil.etablissement());
            if (etablissement != null) {
                if (etablissement.getProfessionnels() == null) {
                    etablissement.setProfessionnels(new ArrayList<>());
                }
                etablissement.getProfessionnels().add(soignant);
                etablissementSanteRepository.save(etablissement);
            }
        }
        return soignants;
    }

    private Utilisateur creerCompte(Personne personne, String role, Utilisateur femmeAssociee) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setPrenom(personne.prenom());
        utilisateur.setNom(personne.nom().toUpperCase(Locale.FRENCH));
        utilisateur.setEmail(email(personne));
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasseDemo));
        utilisateur.setTelephone(String.format("+22177001%04d", ++numeroTelephone));
        utilisateur.setDateNaissance(personne.naissance());
        utilisateur.setRole(role);
        utilisateur.setFemmeAssociee(femmeAssociee);
        utilisateur.setActif(true);
        return utilisateurRepository.save(utilisateur);
    }

    /** « Ndèye Fatou Diop » → ndeye.fatou.diop.demo@yay360.sn */
    private static String email(Personne personne) {
        String brut = (personne.prenom() + " " + personne.nom()).toLowerCase(Locale.FRENCH);
        String sansAccents = Normalizer.normalize(brut, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.trim().replaceAll("[^a-z]+", ".") + SUFFIXE_EMAIL;
    }

    // ==================== DOSSIERS ====================

    private Dossier creerDossier(int i, ProfilPatiente profil, List<EtablissementSante> etablissements,
                                 List<Utilisateur> medecins, List<Utilisateur> sagesFemmes) {
        Utilisateur femme = creerCompte(profil.femme(), RoleUtilisateur.FEMME_ENCEINTE, null);
        Utilisateur conjoint = creerCompte(profil.conjoint(), RoleUtilisateur.CONJOINT, femme);

        EtablissementSante etablissement = profil.etablissement() >= 0 ? etablissements.get(profil.etablissement()) : null;
        Utilisateur medecin = profil.etablissement() >= 0 ? soignantDe(MEDECINS, medecins, profil.etablissement(), i) : null;
        Utilisateur sageFemme = profil.etablissement() >= 0 ? soignantDe(SAGES_FEMMES, sagesFemmes, profil.etablissement(), i) : null;

        if (profil.grossessePassee()) {
            creerGrossessePassee(femme, etablissement, profil.grossessesPrecedentes());
        }

        LocalDate ddr = LocalDate.now().minusWeeks(profil.semaines()).minusDays(i % 5);
        Grossesse grossesse = new Grossesse();
        grossesse.setFemmeEnceinte(femme);
        grossesse.setDateDernieresRegles(ddr);
        grossesse.setDatePrevueAccouchement(ddr.plusDays(280));
        grossesse.setStatut(StatutGrossesse.EN_COURS);
        grossesse.setGrossesseARisque(profil.aRisque());
        grossesse.setMotifRisque(profil.motifRisque());
        grossesse.setNombreGrossessesPrecedentes(profil.grossessesPrecedentes());
        grossesse.setAntecedents(i % 3 == 0 ? "Aucun antécédent particulier déclaré. " + MENTION : null);
        grossesse.setEtablissementSuivi(etablissement);
        grossesse = grossesseRepository.save(grossesse);

        return new Dossier(profil, femme, conjoint, grossesse, etablissement, medecin, sageFemme);
    }

    /** Soignant de l'établissement ; s'il y en a deux, alternance selon la patiente. */
    private static Utilisateur soignantDe(List<ProfilSoignant> profils, List<Utilisateur> soignants,
                                          int etablissement, int patiente) {
        List<Utilisateur> candidats = new ArrayList<>();
        for (int k = 0; k < profils.size(); k++) {
            if (profils.get(k).etablissement() == etablissement) {
                candidats.add(soignants.get(k));
            }
        }
        return candidats.isEmpty() ? null : candidats.get(patiente % candidats.size());
    }

    private void creerGrossessePassee(Utilisateur femme, EtablissementSante etablissement, int rang) {
        LocalDate ddr = LocalDate.now().minusYears(rang).minusWeeks(40);
        Grossesse passee = new Grossesse();
        passee.setFemmeEnceinte(femme);
        passee.setDateDernieresRegles(ddr);
        passee.setDatePrevueAccouchement(ddr.plusDays(280));
        passee.setStatut(StatutGrossesse.TERMINEE);
        passee.setDateFin(ddr.plusWeeks(39).plusDays(3));
        passee.setGrossesseARisque(false);
        passee.setNombreGrossessesPrecedentes(Math.max(rang - 1, 0));
        passee.setEtablissementSuivi(etablissement);
        grossesseRepository.save(passee);
    }

    // ==================== CONSULTATIONS PASSÉES ====================

    /**
     * Une consultation (et son rendez-vous terminé) à chaque contact OMS déjà passé. Les signaux sont calculés
     * par le moteur de règles ; s'il en trouve, une alerte « analyse automatique » est tracée, comme en réel.
     */
    private void creerSuiviPrenatal(int i, Dossier d) {
        if (d.etablissement() == null) {
            return; // pas encore suivie : aucun soignant ne l'a vue
        }
        Consultation precedente = null;
        int rang = 0;
        for (int semaine : MoteurReglesCliniques.CONTACTS_CPN_OMS) {
            if (semaine >= d.profil().semaines()) {
                break;
            }
            LocalDateTime date = d.grossesse().getDateDernieresRegles().plusWeeks(semaine).plusDays(1)
                    .atTime(8 + (i + semaine) % 5, (i % 2) * 30);
            Utilisateur soignant = rang++ % 2 == 0 ? d.sageFemme() : d.medecin();

            RendezVous rdv = nouveauRendezVous(d, soignant, TypeRendezVous.CONSULTATION_PRENATALE,
                    StatutRendezVous.TERMINE, date, "Consultation prénatale (contact OMS de " + semaine + " SA). " + MENTION);

            Constantes c = constantes(i, d.profil(), semaine);
            Double poidsPrecedent = precedente != null ? precedente.getPoidsKg() : null;
            Long jours = precedente != null
                    ? ChronoUnit.DAYS.between(precedente.getDateConsultation().toLocalDate(), date.toLocalDate()) : null;
            List<MoteurReglesCliniques.SignalClinique> signaux = moteur.analyserConstantes(semaine, c.tension(),
                    c.temperature(), c.bcf(), c.hauteurUterine(), c.poids(), poidsPrecedent, jours);

            Consultation consultation = new Consultation();
            consultation.setGrossesse(d.grossesse());
            consultation.setFemmeEnceinte(d.femme());
            consultation.setProfessionnel(soignant);
            consultation.setRendezVous(rdv);
            consultation.setEtablissement(d.etablissement());
            consultation.setDateConsultation(date);
            consultation.setPoidsKg(c.poids());
            consultation.setTensionArterielle(c.tension());
            consultation.setTemperatureC(c.temperature());
            consultation.setHauteurUterineCm(c.hauteurUterine());
            consultation.setBattementsCoeurFoetal(c.bcf());
            consultation.setObservations("Consultation prénatale de démonstration : constantes fictives, sans valeur médicale.");
            if (!signaux.isEmpty()) {
                consultation.setSignauxCliniques(String.join("\n",
                        signaux.stream().map(s -> s.code() + "|" + s.niveau() + "|" + s).toList()));
            }
            precedente = consultationRepository.save(consultation);

            if (!signaux.isEmpty()) {
                tracerAlerteAutomatique(d, soignant, signaux, date);
            }
        }
    }

    private record Constantes(double poids, String tension, double temperature, Double hauteurUterine, Integer bcf) {
    }

    /** Constantes de démonstration : valeurs habituelles, sauf les trois cas prévus pour illustrer les règles. */
    private static Constantes constantes(int i, ProfilPatiente profil, int semaine) {
        List<String> tensions = List.of("110/70", "115/75", "120/80", "105/65", "118/76");
        double poids = Math.round((profil.poidsInitialKg() + 0.4 * (semaine - 12)) * 10) / 10.0;
        String tension = tensions.get((i + semaine) % tensions.size());
        double temperature = Math.round((36.6 + ((i + semaine) % 5) * 0.1) * 10) / 10.0;
        // Hauteur utérine ≈ nombre de semaines (écart ≤ 1 cm, sous le seuil de discordance du moteur)
        Double hauteurUterine = semaine >= 20 ? (double) (semaine + (i % 3) - 1) : null;
        Integer bcf = semaine >= 12 ? 135 + (i * 7 + semaine) % 20 : null;

        if (i == 0 && semaine >= 20) {
            tension = "145/95";          // Awa Ndiaye : signal « HTA » → grossesse déclarée à risque
        } else if (i == 2 && semaine == 30) {
            temperature = 38.3;          // Mariama Sy : signal « fièvre »
        } else if (i == 6 && semaine == 26) {
            hauteurUterine = 18.0;       // Rokhaya Seck : signal « hauteur utérine discordante »
        }
        return new Constantes(poids, tension, temperature, hauteurUterine, bcf);
    }

    private void tracerAlerteAutomatique(Dossier d, Utilisateur soignant,
                                         List<MoteurReglesCliniques.SignalClinique> signaux, LocalDateTime date) {
        boolean ancienne = date.isBefore(LocalDateTime.now().minusDays(14));
        AlerteMedicale alerte = new AlerteMedicale();
        alerte.setGrossesse(d.grossesse());
        alerte.setFemmeEnceinte(d.femme());
        alerte.setTypeSymptome(TYPE_CONSTANTES_ANORMALES);
        alerte.setDescription("Constantes anormales détectées lors de la consultation.");
        alerte.setNiveauGravite(MoteurReglesCliniques.niveauMax(signaux));
        alerte.setOrigine(ORIGINE_ANALYSE);
        alerte.setJustificationTriage(String.join("\n", signaux.stream().map(Object::toString).toList()));
        alerte.setProfessionnelPriseEnCharge(soignant);
        alerte.setDatePriseEnCharge(date);
        if (ancienne) {
            alerte.setStatut(StatutAlerte.RESOLUE);
            alerte.setDateResolution(date.plusDays(1));
            alerte.setNotesPriseEnCharge("Constantes contrôlées, suivi adapté. " + MENTION);
        } else {
            alerte.setStatut(StatutAlerte.PRISE_EN_CHARGE);
        }
        AlerteMedicale enregistree = alerteMedicaleRepository.saveAndFlush(alerte);
        antidater("alertes_medicales", "date_declaration", enregistree.getId(), date);
    }

    // ==================== RENDEZ-VOUS ====================

    private void creerRendezVousAVenir(List<Dossier> dossiers) {
        LocalDateTime maintenant = LocalDateTime.now();
        for (int i = 0; i < dossiers.size(); i++) {
            Dossier d = dossiers.get(i);
            if (d.etablissement() == null) {
                continue;
            }
            // Prochain suivi prénatal, réparti sur les deux semaines à venir
            LocalDateTime date = maintenant.toLocalDate().plusDays(1 + i).atTime(9 + i % 6, 30);
            Utilisateur soignant = d.profil().aRisque() ? d.medecin() : d.sageFemme();
            RendezVous prochain = nouveauRendezVous(d, soignant, TypeRendezVous.CONSULTATION_PRENATALE,
                    i % 2 == 0 ? StatutRendezVous.CONFIRME : StatutRendezVous.PLANIFIE, date,
                    "Prochaine consultation prénatale. " + MENTION);
            notifier(d.femme(), d.grossesse(), TypeNotification.RAPPEL_CONSULTATION, "Rappel de rendez-vous",
                    "Votre consultation prénatale est prévue le " + prochain.getDateHeure().format(JOUR_HEURE) + ".",
                    false, maintenant.minusHours(2));
            notifier(d.conjoint(), d.grossesse(), TypeNotification.RAPPEL_CONSULTATION, "Rappel de rendez-vous",
                    "Consultation prénatale prévue le " + prochain.getDateHeure().format(JOUR_HEURE) + ".",
                    false, maintenant.minusHours(2));
            notifier(d.femme(), d.grossesse(), TypeNotification.CONSEIL_HEBDOMADAIRE,
                    "Conseils de la semaine " + d.profil().semaines(),
                    "De nouveaux repères sont disponibles pour votre semaine de grossesse.", true, maintenant.minusDays(2));
        }

        // Situations variées pour le planning des soignants
        nouveauRendezVous(dossiers.get(0), dossiers.get(0).medecin(), TypeRendezVous.CONSULTATION_PRENATALE,
                StatutRendezVous.CONFIRME, maintenant.plusHours(2).truncatedTo(ChronoUnit.HOURS),
                "Contrôle de la tension artérielle. " + MENTION);
        nouveauRendezVous(dossiers.get(6), dossiers.get(6).medecin(), TypeRendezVous.CONSULTATION_PRENATALE,
                StatutRendezVous.PLANIFIE, maintenant.plusHours(3).truncatedTo(ChronoUnit.HOURS),
                "Contrôle de la hauteur utérine. " + MENTION);
        nouveauRendezVous(dossiers.get(7), dossiers.get(7).medecin(), TypeRendezVous.ECHOGRAPHIE,
                StatutRendezVous.CONFIRME, maintenant.toLocalDate().plusDays(3).atTime(11, 0),
                "Échographie du 2e trimestre. " + MENTION);
        nouveauRendezVous(dossiers.get(2), dossiers.get(2).medecin(), TypeRendezVous.ECHOGRAPHIE,
                StatutRendezVous.CONFIRME, maintenant.toLocalDate().plusDays(5).atTime(10, 0),
                "Échographie du 3e trimestre. " + MENTION);
        nouveauRendezVous(dossiers.get(4), dossiers.get(4).sageFemme(), TypeRendezVous.EXAMEN_LABORATOIRE,
                StatutRendezVous.PLANIFIE, maintenant.toLocalDate().plusDays(4).atTime(8, 30),
                "Bilan sanguin de fin de grossesse. " + MENTION);
        nouveauRendezVous(dossiers.get(3), dossiers.get(3).sageFemme(), TypeRendezVous.VACCINATION,
                StatutRendezVous.PLANIFIE, maintenant.toLocalDate().plusDays(8).atTime(9, 0),
                "Vaccination prévue au calendrier. " + MENTION);
        // Demande de la patiente, sans soignant attribué
        nouveauRendezVous(dossiers.get(1), null, TypeRendezVous.CONSULTATION_PRENATALE,
                StatutRendezVous.PLANIFIE, maintenant.toLocalDate().plusDays(6).atTime(10, 0),
                "Demande de la patiente. " + MENTION);

        // Historique : annulé, absence, et deux rendez-vous passés encore à clôturer
        RendezVous annule = nouveauRendezVous(dossiers.get(3), dossiers.get(3).sageFemme(),
                TypeRendezVous.CONSULTATION_PRENATALE, StatutRendezVous.ANNULE,
                maintenant.toLocalDate().minusDays(5).atTime(10, 0), "Consultation de suivi. " + MENTION);
        annule.setNotesAnnulation("Annulé à la demande de la patiente. " + MENTION);
        rendezVousRepository.save(annule);
        nouveauRendezVous(dossiers.get(5), dossiers.get(5).sageFemme(), TypeRendezVous.CONSULTATION_PRENATALE,
                StatutRendezVous.ABSENT, maintenant.toLocalDate().minusDays(3).atTime(15, 0),
                "Consultation de suivi. " + MENTION);
        nouveauRendezVous(dossiers.get(4), dossiers.get(4).medecin(), TypeRendezVous.CONSULTATION_PRENATALE,
                StatutRendezVous.CONFIRME, maintenant.toLocalDate().minusDays(1).atTime(10, 0),
                "Consultation de fin de grossesse. " + MENTION);
        nouveauRendezVous(dossiers.get(8), dossiers.get(8).sageFemme(), TypeRendezVous.CONSULTATION_PRENATALE,
                StatutRendezVous.CONFIRME, maintenant.minusHours(2).truncatedTo(ChronoUnit.HOURS),
                "Consultation de fin de grossesse. " + MENTION);

        // Patiente sans établissement : invitation à en choisir un
        Dossier sansSuivi = dossiers.get(9);
        notifier(sansSuivi.femme(), sansSuivi.grossesse(), TypeNotification.INFORMATION, "Choisissez votre établissement",
                "Choisissez un établissement de suivi pour que l'équipe soignante puisse suivre votre grossesse.",
                false, maintenant.minusDays(1));
    }

    private RendezVous nouveauRendezVous(Dossier d, Utilisateur soignant, String type, String statut,
                                         LocalDateTime date, String motif) {
        RendezVous rdv = new RendezVous();
        rdv.setGrossesse(d.grossesse());
        rdv.setFemmeEnceinte(d.femme());
        rdv.setProfessionnel(soignant);
        rdv.setEtablissement(d.etablissement());
        rdv.setDateHeure(date);
        rdv.setType(type);
        rdv.setStatut(statut);
        rdv.setMotif(motif);
        rdv.setRappelEnvoye(date.isBefore(LocalDateTime.now().plusDays(1)));
        return rendezVousRepository.save(rdv);
    }

    // ==================== SIGNALEMENTS (PATIENTE OU CONJOINT) ====================

    private record Signalement(int patiente, String typeSymptome, String statut, long heuresAvant,
                               boolean parMedecin, String description) {
    }

    private static final List<Signalement> SIGNALEMENTS = List.of(
            new Signalement(0, TypeSymptome.MAUX_DE_TETE_INTENSES, StatutAlerte.NOUVELLE, 3, false,
                    "Maux de tête depuis ce matin. " + MENTION),
            new Signalement(2, TypeSymptome.DIMINUTION_MOUVEMENTS_BEBE, StatutAlerte.NOUVELLE, 1, false,
                    "Le bébé bouge moins depuis hier soir. " + MENTION),
            new Signalement(7, TypeSymptome.AUTRE, StatutAlerte.NOUVELLE, 5, false,
                    "Fatigue inhabituelle. " + MENTION),
            new Signalement(6, TypeSymptome.OEDEMES_SOUDAINS, StatutAlerte.PRISE_EN_CHARGE, 26, false,
                    "Gonflement des mains et du visage. " + MENTION),
            new Signalement(8, TypeSymptome.PERTE_LIQUIDE, StatutAlerte.PRISE_EN_CHARGE, 2, true,
                    "Signalé par le conjoint. " + MENTION),
            new Signalement(3, TypeSymptome.VOMISSEMENTS_SEVERES, StatutAlerte.RESOLUE, 6 * 24, false,
                    "Vomissements répétés. " + MENTION),
            new Signalement(4, TypeSymptome.DOULEURS_ABDOMINALES, StatutAlerte.RESOLUE, 10 * 24, true,
                    "Douleurs au bas du ventre. " + MENTION),
            new Signalement(1, TypeSymptome.FIEVRE_ELEVEE, StatutAlerte.RESOLUE, 3 * 24, true,
                    "Fièvre depuis la veille. " + MENTION));

    /** Le niveau et ses justifications viennent du moteur de triage, comme pour un signalement réel. */
    private void creerSignalements(List<Dossier> dossiers) {
        Map<Long, List<LocalDateTime>> datesParGrossesse = new HashMap<>();
        // Du plus ancien au plus récent, pour compter correctement les alertes des 7 jours précédents
        List<Signalement> ordre = new ArrayList<>(SIGNALEMENTS);
        ordre.sort((a, b) -> Long.compare(b.heuresAvant(), a.heuresAvant()));

        for (Signalement s : ordre) {
            Dossier d = dossiers.get(s.patiente());
            LocalDateTime date = LocalDateTime.now().minusHours(s.heuresAvant());
            List<LocalDateTime> precedentes = datesParGrossesse.computeIfAbsent(
                    d.grossesse().getId() == null ? (long) s.patiente() : d.grossesse().getId(), k -> new ArrayList<>());
            long recentes = precedentes.stream().filter(p -> p.isAfter(date.minusDays(7))).count();
            int semaine = (int) (ChronoUnit.DAYS.between(d.grossesse().getDateDernieresRegles(), date.toLocalDate()) / 7);

            MoteurReglesCliniques.ResultatTriage triage = moteur.trierSymptome(s.typeSymptome(),
                    semaine, Boolean.TRUE.equals(d.grossesse().getGrossesseARisque()), recentes);
            precedentes.add(date);

            AlerteMedicale alerte = new AlerteMedicale();
            alerte.setGrossesse(d.grossesse());
            alerte.setFemmeEnceinte(d.femme());
            alerte.setTypeSymptome(s.typeSymptome());
            alerte.setDescription(s.description());
            alerte.setNiveauGravite(triage.niveau());
            alerte.setOrigine(ORIGINE_PATIENTE);
            alerte.setJustificationTriage(String.join("\n", triage.justifications()));
            alerte.setStatut(s.statut());
            if (!StatutAlerte.NOUVELLE.equals(s.statut())) {
                alerte.setProfessionnelPriseEnCharge(s.parMedecin() ? d.medecin() : d.sageFemme());
                alerte.setDatePriseEnCharge(date.plusMinutes(40));
                alerte.setNotesPriseEnCharge("Patiente rappelée par l'équipe et examinée. " + MENTION);
            }
            if (StatutAlerte.RESOLUE.equals(s.statut())) {
                alerte.setDateResolution(date.plusHours(6));
                alerte.setNotesPriseEnCharge("Situation contrôlée lors de l'examen, consignes remises à la patiente. " + MENTION);
            }
            AlerteMedicale enregistree = alerteMedicaleRepository.saveAndFlush(alerte);
            antidater("alertes_medicales", "date_declaration", enregistree.getId(), date);

            notifier(d.femme(), d.grossesse(), TypeNotification.ALERTE_MEDICALE, "Signalement transmis",
                    "Votre signalement a été transmis à votre équipe de santé.",
                    !StatutAlerte.NOUVELLE.equals(s.statut()), date);
            if (StatutAlerte.NOUVELLE.equals(s.statut())) {
                String patiente = d.femme().getPrenom() + " " + d.femme().getNom();
                for (Utilisateur soignant : List.of(d.medecin(), d.sageFemme())) {
                    notifier(soignant, d.grossesse(), TypeNotification.ALERTE_MEDICALE, "Nouvelle alerte : " + patiente,
                            "Un symptôme a été signalé. Consultez l'alerte pour la prendre en charge.", false, date);
                }
            }
        }
    }

    // ==================== OUTILS ====================

    private void notifier(Utilisateur destinataire, Grossesse grossesse, String type, String titre, String message,
                          boolean lue, LocalDateTime date) {
        if (destinataire == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setDestinataire(destinataire);
        notification.setGrossesse(grossesse);
        notification.setType(type);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setLue(lue);
        if (lue) {
            notification.setDateLecture(date.plusHours(1));
        }
        Notification enregistree = notificationRepository.saveAndFlush(notification);
        antidater("notifications", "date_envoi", enregistree.getId(), date);
    }

    /** Les dates d'envoi et de déclaration sont fixées à l'insertion : on les antidate pour un historique crédible. */
    private void antidater(String table, String colonne, Long id, LocalDateTime date) {
        if (id == null) {
            return;
        }
        jdbcTemplate.update("UPDATE " + table + " SET " + colonne + " = ? WHERE id = ?", Timestamp.valueOf(date), id);
    }
}
