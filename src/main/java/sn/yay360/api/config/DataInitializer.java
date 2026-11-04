package sn.yay360.api.config;

import sn.yay360.api.helper.CalculObstetricalHelper;
import sn.yay360.api.model.AlerteMedicale;
import sn.yay360.api.model.CategorieConseil;
import sn.yay360.api.model.ConseilGrossesse;
import sn.yay360.api.model.Consultation;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.EtapeGrossesse;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.NiveauGravite;
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
import sn.yay360.api.repo.ConseilGrossesseRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.EtapeGrossesseRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.NotificationRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final EtapeGrossesseRepository etapeGrossesseRepository;
    private final ConseilGrossesseRepository conseilGrossesseRepository;
    private final EtablissementSanteRepository etablissementSanteRepository;
    private final GrossesseRepository grossesseRepository;
    private final RendezVousRepository rendezVousRepository;
    private final ConsultationRepository consultationRepository;
    private final AlerteMedicaleRepository alerteMedicaleRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final CalculObstetricalHelper calcul;

    @Value("${yay360.demo-data.enabled:false}")
    private boolean demoDataEnabled;

    @Value("${yay360.admin.password:}")
    private String motDePasseAdmin;

    @Value("${yay360.demo-data.password:}")
    private String motDePasseDemo;

    @Override
    @Transactional
    public void run(String... args) {
        Utilisateur admin = seedAdmin();
        seedEtapesGrossesse();
        seedConseils(admin);
        seedEtablissements();
        if (demoDataEnabled) {
            seedDonneesDemo();
        }
    }

    // ==================== UTILISATEUR ADMIN ====================

    private Utilisateur seedAdmin() {
        String emailAdmin = "admin@yay360.sn";

        if (utilisateurRepository.existsByEmailIgnoreCase(emailAdmin)) {
            return utilisateurRepository.findByEmailIgnoreCase(emailAdmin).orElseThrow();
        }

        Utilisateur admin = new Utilisateur();
        admin.setNom("GNINGUE");
        admin.setPrenom("Ibrahima");
        admin.setEmail(emailAdmin);
        boolean motDePasseGenere = motDePasseAdmin == null || motDePasseAdmin.isBlank();
        String motDePasse = motDePasseGenere ? genererMotDePasse() : motDePasseAdmin;
        admin.setMotDePasse(passwordEncoder.encode(motDePasse));
        admin.setTelephone("+221785187505");
        admin.setDateNaissance(LocalDate.of(1999, 2, 21));
        admin.setRole(RoleUtilisateur.ADMIN);
        admin.setActif(true);

        Utilisateur enregistre = utilisateurRepository.save(admin);

        if (motDePasseGenere) {
            // Affiché une seule fois, à la création : à changer dès la première connexion.
            log.warn("[YAY360] Compte administrateur {} créé avec un mot de passe généré : {} "
                    + "(définissez YAY360_ADMIN_PASSWORD pour le fixer)", emailAdmin, motDePasse);
        } else {
            log.info("[YAY360] Compte administrateur {} créé (mot de passe issu de YAY360_ADMIN_PASSWORD)", emailAdmin);
        }

        return enregistre;
    }

    private static String genererMotDePasse() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom aleatoire = new SecureRandom();
        StringBuilder sb = new StringBuilder("Yay-");
        for (int i = 0; i < 16; i++) {
            sb.append(alphabet.charAt(aleatoire.nextInt(alphabet.length())));
        }
        return sb.append('9').toString();
    }

    // ==================== ETAPES DE GROSSESSE (1 A 40 SEMAINES) ====================

    void seedEtapesGrossesse() {
        if (etapeGrossesseRepository.count() > 0) {
            return;
        }

        for (int semaine = 1; semaine <= calcul.dureeGrossesseSemaines(); semaine++) {
            EtapeGrossesse etape = construireEtape(semaine);
            etapeGrossesseRepository.save(etape);
        }

        log.info("[YAY360] 40 étapes de grossesse initialisées (contenu administrable via /etapes-grossesse)");
    }

    private EtapeGrossesse construireEtape(int semaine) {
        EtapeGrossesse etape = new EtapeGrossesse();
        etape.setSemaine(semaine);
        etape.setTrimestre(calcul.calculerTrimestre(semaine));

        String trimestre = etape.getTrimestre();

        if ("T1".equals(trimestre)) {
            etape.setDeveloppementBebe("Semaine " + semaine + " : formation et développement précoce des organes essentiels de l'embryon/fœtus.");
            etape.setChangementsMere("Nausées possibles, fatigue, seins sensibles et changements hormonaux typiques du premier trimestre.");
            etape.setObjectifsSante("Débuter le suivi prénatal, prendre de l'acide folique, éviter alcool et tabac.");
            etape.setConseilsAlimentation("Privilégier une alimentation équilibrée et fractionnée pour limiter les nausées ; éviter les aliments crus à risque.");
            etape.setConseilsHydratation("Boire au moins 1,5 litre d'eau par jour, en petites quantités si nausées.");
            etape.setExercicesPhysiques("Marche légère et étirements doux si l'état général le permet.");
            etape.setConseilsSommeil("Se reposer dès que la fatigue se fait sentir, dormir sur le côté progressivement.");
            etape.setPrecautions("Éviter l'automédication ; consulter avant toute prise de médicament.");
            etape.setExamensRecommandes(semaine == 12
                    ? "Première échographie de datation et bilan prénatal complet (T1)."
                    : "Consultation prénatale de suivi selon calendrier.");
            etape.setSignesAlerte("Saignements importants, douleurs pelviennes intenses, fièvre élevée.");
        } else if ("T2".equals(trimestre)) {
            etape.setDeveloppementBebe("Semaine " + semaine + " : croissance rapide du fœtus, mouvements de plus en plus perceptibles.");
            etape.setChangementsMere("Ventre qui s'arrondit visiblement, regain d'énergie, possibles douleurs ligamentaires.");
            etape.setObjectifsSante("Maintenir un poids adapté, surveiller la tension artérielle et le glucose.");
            etape.setConseilsAlimentation("Augmenter légèrement les apports en fer, calcium et protéines.");
            etape.setConseilsHydratation("Boire régulièrement, environ 2 litres d'eau par jour.");
            etape.setExercicesPhysiques("Marche, natation prénatale ou yoga adapté à la grossesse.");
            etape.setConseilsSommeil("Dormir sur le côté gauche de préférence pour améliorer la circulation.");
            etape.setPrecautions("Éviter le port de charges lourdes et les stations debout prolongées.");
            etape.setExamensRecommandes(semaine == 22
                    ? "Échographie morphologique du 2e trimestre."
                    : "Consultation prénatale de suivi et contrôle de la tension artérielle.");
            etape.setSignesAlerte("Diminution des mouvements du bébé, maux de tête intenses, œdèmes soudains.");
        } else {
            etape.setDeveloppementBebe("Semaine " + semaine + " : prise de poids rapide, maturation pulmonaire et préparation à la naissance.");
            etape.setChangementsMere("Ventre volumineux, essoufflement possible, contractions de Braxton-Hicks fréquentes.");
            etape.setObjectifsSante("Préparer l'accouchement, surveiller la position du bébé et les signes de travail.");
            etape.setConseilsAlimentation("Alimentation légère et fractionnée, riche en fibres pour limiter les inconforts digestifs.");
            etape.setConseilsHydratation("Bien s'hydrater pour limiter les contractions liées à la déshydratation.");
            etape.setExercicesPhysiques("Marche douce et exercices de respiration en préparation à l'accouchement.");
            etape.setConseilsSommeil("Utiliser des coussins de soutien pour dormir plus confortablement sur le côté.");
            etape.setPrecautions("Préparer le sac de maternité et identifier la structure de santé pour l'accouchement.");
            etape.setExamensRecommandes(semaine == 32
                    ? "Échographie du 3e trimestre et contrôle de la croissance fœtale."
                    : "Consultation prénatale rapprochée et monitoring si nécessaire.");
            etape.setSignesAlerte("Perte de liquide, contractions régulières et rapprochées, diminution des mouvements du bébé, saignements.");
        }

        return etape;
    }

    // ==================== BIBLIOTHEQUE DE CONSEILS (DEMO) ====================

    private void seedConseils(Utilisateur auteur) {
        if (conseilGrossesseRepository.count() > 0) {
            return;
        }

        creerConseil(auteur, 1, 13, CategorieConseil.NUTRITION,
                "Acide folique et prévention des malformations",
                "La prise quotidienne d'acide folique dès le début de la grossesse aide à réduire le risque d'anomalies du tube neural chez le bébé.");

        creerConseil(auteur, 1, 13, CategorieConseil.SANTE_MENTALE,
                "Gérer l'anxiété du premier trimestre",
                "Il est normal de ressentir de l'anxiété en début de grossesse. En parler à un professionnel de santé ou à un proche de confiance peut aider.");

        creerConseil(auteur, 14, 27, CategorieConseil.ACTIVITE_PHYSIQUE,
                "Bouger en toute sécurité au 2e trimestre",
                "La marche, la natation et le yoga prénatal sont recommandés pour maintenir une bonne forme physique sans risque pour la grossesse.");

        creerConseil(auteur, 14, 27, CategorieConseil.NUTRITION,
                "Prévenir l'anémie pendant la grossesse",
                "Les aliments riches en fer (viande rouge, légumineuses, légumes verts) aident à prévenir l'anémie fréquente au deuxième trimestre.");

        creerConseil(auteur, 28, 40, CategorieConseil.PREPARATION_ACCOUCHEMENT,
                "Préparer son sac de maternité",
                "Il est conseillé de préparer le sac de maternité dès la 34e semaine : documents administratifs, vêtements pour le bébé et pour la maman.");

        creerConseil(auteur, 28, 40, CategorieConseil.ALLAITEMENT,
                "Se préparer à l'allaitement",
                "Se renseigner tôt sur les bonnes pratiques d'allaitement facilite la mise en route après l'accouchement.");

        creerConseil(auteur, 1, 40, CategorieConseil.SOMMEIL,
                "Bien dormir pendant la grossesse",
                "Dormir sur le côté gauche, utiliser des coussins de soutien et éviter les écrans avant le coucher améliore la qualité du sommeil.");

        creerConseil(auteur, 1, 40, CategorieConseil.PREVENTION,
                "Ne jamais remplacer un avis médical",
                "Les conseils de la plateforme accompagnent le suivi mais ne remplacent jamais une consultation avec un professionnel de santé.");

        log.info("=== [YAY360] Bibliothèque de conseils initialisée (8 conseils de démonstration) ===");
    }

    private void creerConseil(Utilisateur auteur, int semaineDebut, int semaineFin, String categorie,
                               String titre, String contenu) {
        ConseilGrossesse conseil = new ConseilGrossesse();
        conseil.setSemaineDebut(semaineDebut);
        conseil.setSemaineFin(semaineFin);
        conseil.setTrimestre(calcul.calculerTrimestre(semaineDebut));
        conseil.setCategorie(categorie);
        conseil.setTitre(titre);
        conseil.setContenu(contenu);
        conseil.setActif(true);
        conseil.setAuteur(auteur);
        conseilGrossesseRepository.save(conseil);
    }

    // ==================== ETABLISSEMENTS DE SANTE (DEMO) ====================

    private void seedEtablissements() {
        if (etablissementSanteRepository.count() > 0) {
            return;
        }

        creerEtablissement("Hôpital Principal de Dakar", "HOPITAL", "Avenue Nelson Mandela, Dakar",
                "Dakar", "+221338391212", 14.6708, -17.4335);

        creerEtablissement("Centre de Santé Roi Baudouin", "CENTRE_DE_SANTE", "Guédiawaye",
                "Dakar", "+221338341414", 14.7692, -17.4034);

        creerEtablissement("Hôpital Régional de Thiès", "HOPITAL", "Route de Dakar, Thiès",
                "Thiès", "+221339512727", 14.7910, -16.9256);

        log.info("=== [YAY360] 3 établissements de santé initialisés (démonstration) ===");
    }

    private void creerEtablissement(String nom, String type, String adresse, String ville,
                                     String telephone, double latitude, double longitude) {
        EtablissementSante etablissement = new EtablissementSante();
        etablissement.setNom(nom);
        etablissement.setType(type);
        etablissement.setAdresse(adresse);
        etablissement.setVille(ville);
        etablissement.setTelephone(telephone);
        etablissement.setLatitude(latitude);
        etablissement.setLongitude(longitude);
        etablissement.setActif(true);
        etablissementSanteRepository.save(etablissement);
    }

    // ==================== DOSSIERS DE DEMONSTRATION ====================

    void seedDonneesDemo() {
        if (motDePasseDemo == null || motDePasseDemo.isBlank()) {
            motDePasseDemo = genererMotDePasse();
            log.warn("[YAY360] Mot de passe des comptes de démonstration généré : {} "
                    + "(définissez YAY360_DEMO_PASSWORD pour le fixer)", motDePasseDemo);
        }
        Utilisateur medecin = creerUtilisateurDemo(
                "medecin.demo@yay360.sn", "DEMO", "Medecin", RoleUtilisateur.MEDECIN, null,
                LocalDate.of(1980, 4, 12), "+221770000101");
        Utilisateur sageFemme = creerUtilisateurDemo(
                "sagefemme.demo@yay360.sn", "DEMO", "Sage Femme", RoleUtilisateur.SAGE_FEMME, null,
                LocalDate.of(1985, 8, 20), "+221770000102");
        Utilisateur patiente = creerUtilisateurDemo(
                "patiente.demo@yay360.sn", "DEMO", "Patiente", RoleUtilisateur.FEMME_ENCEINTE, null,
                LocalDate.of(1998, 3, 15), "+221770000103");
        Utilisateur secondePatiente = creerUtilisateurDemo(
                "patiente2.demo@yay360.sn", "DEMO", "Patiente Deux", RoleUtilisateur.FEMME_ENCEINTE, null,
                LocalDate.of(1995, 11, 3), "+221770000104");
        creerUtilisateurDemo(
                "conjoint.demo@yay360.sn", "DEMO", "Conjoint", RoleUtilisateur.CONJOINT, patiente,
                LocalDate.of(1994, 1, 7), "+221770000105");

        associerProfessionnels(medecin, sageFemme);

        Grossesse grossesse = creerGrossesseDemo(patiente, 24, true);
        creerGrossesseDemo(secondePatiente, 10, false);

        List<RendezVous> rendezVous = creerRendezVousDemo(grossesse, patiente, medecin, sageFemme);
        creerConsultationDemo(grossesse, patiente, medecin, rendezVous);
        creerAlertesDemo(grossesse, patiente, medecin);
        creerNotificationsDemo(grossesse, patiente);

        log.info("[YAY360] Dossiers de démonstration fictifs initialisés (comptes *.demo@yay360.sn, "
                + "mot de passe défini par YAY360_DEMO_PASSWORD)");
    }

    private Utilisateur creerUtilisateurDemo(String email, String nom, String prenom, String role,
                                             Utilisateur femmeAssociee, LocalDate dateNaissance, String telephone) {
        return utilisateurRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Utilisateur utilisateur = new Utilisateur();
            utilisateur.setNom(nom);
            utilisateur.setPrenom(prenom);
            utilisateur.setEmail(email);
            utilisateur.setMotDePasse(passwordEncoder.encode(motDePasseDemo));
            utilisateur.setTelephone(telephone);
            utilisateur.setDateNaissance(dateNaissance);
            utilisateur.setRole(role);
            utilisateur.setFemmeAssociee(femmeAssociee);
            utilisateur.setActif(true);
            return utilisateurRepository.save(utilisateur);
        });
    }

    private void associerProfessionnels(Utilisateur medecin, Utilisateur sageFemme) {
        EtablissementSante etablissement = etablissementSanteRepository.findAll().stream().findFirst().orElse(null);
        if (etablissement == null) {
            return;
        }

        List<Utilisateur> professionnels = etablissement.getProfessionnels();
        if (professionnels == null) {
            professionnels = new ArrayList<>();
            etablissement.setProfessionnels(professionnels);
        }
        ajouterProfessionnelSiAbsent(professionnels, medecin);
        ajouterProfessionnelSiAbsent(professionnels, sageFemme);
        etablissementSanteRepository.save(etablissement);
    }

    private void ajouterProfessionnelSiAbsent(List<Utilisateur> professionnels, Utilisateur utilisateur) {
        boolean dejaAssocie = professionnels.stream()
                .anyMatch(professionnel -> professionnel.getId() != null
                        && professionnel.getId().equals(utilisateur.getId()));
        if (!dejaAssocie) {
            professionnels.add(utilisateur);
        }
    }

    private Grossesse creerGrossesseDemo(Utilisateur patiente, int semaines, boolean aRisque) {
        return grossesseRepository.findByFemmeEnceinteIdAndStatut(patiente.getId(), StatutGrossesse.EN_COURS)
                .orElseGet(() -> {
                    LocalDate dateDernieresRegles = LocalDate.now().minusWeeks(semaines);
                    Grossesse grossesse = new Grossesse();
                    grossesse.setFemmeEnceinte(patiente);
                    grossesse.setDateDernieresRegles(dateDernieresRegles);
                    grossesse.setDatePrevueAccouchement(dateDernieresRegles.plusDays(280));
                    grossesse.setStatut(StatutGrossesse.EN_COURS);
                    grossesse.setGrossesseARisque(aRisque);
                    grossesse.setMotifRisque(aRisque ? "DONNÉE DE TEST — suivi renforcé fictif." : null);
                    grossesse.setNombreGrossessesPrecedentes(1);
                    grossesse.setAntecedents("Dossier de démonstration : antécédents fictifs.");
                    return grossesseRepository.save(grossesse);
                });
    }

    private List<RendezVous> creerRendezVousDemo(Grossesse grossesse, Utilisateur patiente,
                                                  Utilisateur medecin, Utilisateur sageFemme) {
        List<RendezVous> existants = rendezVousRepository.findByGrossesseIdOrderByDateHeureDesc(grossesse.getId());
        if (!existants.isEmpty()) {
            return existants;
        }

        List<RendezVous> rendezVous = new ArrayList<>();
        rendezVous.add(creerRendezVous(grossesse, patiente, medecin, TypeRendezVous.CONSULTATION_PRENATALE,
                StatutRendezVous.TERMINE, LocalDateTime.now().minusDays(14),
                "DONNÉE DE TEST — consultation prénatale de démonstration."));
        rendezVous.add(creerRendezVous(grossesse, patiente, sageFemme, TypeRendezVous.ECHOGRAPHIE,
                StatutRendezVous.CONFIRME, LocalDateTime.now().plusDays(2),
                "DONNÉE DE TEST — échographie de démonstration."));
        rendezVous.add(creerRendezVous(grossesse, patiente, sageFemme, TypeRendezVous.CONSULTATION_PRENATALE,
                StatutRendezVous.PLANIFIE, LocalDateTime.now().plusDays(9),
                "DONNÉE DE TEST — prochain suivi prénatal."));
        return rendezVous;
    }

    private RendezVous creerRendezVous(Grossesse grossesse, Utilisateur patiente, Utilisateur professionnel,
                                        String type, String statut, LocalDateTime dateHeure, String motif) {
        RendezVous rendezVous = new RendezVous();
        rendezVous.setGrossesse(grossesse);
        rendezVous.setFemmeEnceinte(patiente);
        rendezVous.setProfessionnel(professionnel);
        rendezVous.setEtablissement(etablissementSanteRepository.findAll().stream().findFirst().orElse(null));
        rendezVous.setDateHeure(dateHeure);
        rendezVous.setType(type);
        rendezVous.setStatut(statut);
        rendezVous.setMotif(motif);
        return rendezVousRepository.save(rendezVous);
    }

    private void creerConsultationDemo(Grossesse grossesse, Utilisateur patiente, Utilisateur medecin,
                                        List<RendezVous> rendezVous) {
        if (!consultationRepository.findByGrossesseIdOrderByDateConsultationDesc(grossesse.getId()).isEmpty()) {
            return;
        }

        RendezVous rendezVousTermine = rendezVous.stream()
                .filter(rdv -> StatutRendezVous.TERMINE.equals(rdv.getStatut()))
                .findFirst()
                .orElse(null);
        Consultation consultation = new Consultation();
        consultation.setGrossesse(grossesse);
        consultation.setFemmeEnceinte(patiente);
        consultation.setProfessionnel(medecin);
        consultation.setRendezVous(rendezVousTermine);
        consultation.setEtablissement(etablissementSanteRepository.findAll().stream().findFirst().orElse(null));
        consultation.setDateConsultation(rendezVousTermine != null
                ? rendezVousTermine.getDateHeure()
                : LocalDateTime.now().minusDays(14));
        consultation.setPoidsKg(64.5);
        consultation.setTensionArterielle("11/7");
        consultation.setTemperatureC(36.8);
        consultation.setHauteurUterineCm(22.0);
        consultation.setBattementsCoeurFoetal(145);
        consultation.setObservations("DONNÉE DE TEST — constantes fictives, sans valeur médicale.");
        consultation.setDiagnostic("DONNÉE DE TEST — aucune interprétation clinique.");
        consultation.setTraitement("Aucun traitement enregistré dans cette donnée fictive.");
        consultation.setPrescriptions("Aucune prescription réelle.");
        consultationRepository.save(consultation);
    }

    private void creerAlertesDemo(Grossesse grossesse, Utilisateur patiente, Utilisateur medecin) {
        if (!alerteMedicaleRepository.findByGrossesseIdOrderByDateDeclarationDesc(grossesse.getId()).isEmpty()) {
            return;
        }

        AlerteMedicale alerteNouvelle = new AlerteMedicale();
        alerteNouvelle.setGrossesse(grossesse);
        alerteNouvelle.setFemmeEnceinte(patiente);
        alerteNouvelle.setTypeSymptome(TypeSymptome.MAUX_DE_TETE_INTENSES);
        alerteNouvelle.setDescription("DONNÉE DE TEST — exemple fictif d'alerte à examiner.");
        alerteNouvelle.setNiveauGravite(NiveauGravite.ELEVE);
        alerteNouvelle.setStatut(StatutAlerte.NOUVELLE);
        alerteMedicaleRepository.save(alerteNouvelle);

        AlerteMedicale alertePriseEnCharge = new AlerteMedicale();
        alertePriseEnCharge.setGrossesse(grossesse);
        alertePriseEnCharge.setFemmeEnceinte(patiente);
        alertePriseEnCharge.setTypeSymptome(TypeSymptome.DOULEURS_ABDOMINALES);
        alertePriseEnCharge.setDescription("DONNÉE DE TEST — alerte fictive déjà prise en charge.");
        alertePriseEnCharge.setNiveauGravite(NiveauGravite.MODERE);
        alertePriseEnCharge.setStatut(StatutAlerte.PRISE_EN_CHARGE);
        alertePriseEnCharge.setProfessionnelPriseEnCharge(medecin);
        alertePriseEnCharge.setNotesPriseEnCharge("DONNÉE DE TEST — suivi de démonstration uniquement.");
        alerteMedicaleRepository.save(alertePriseEnCharge);
    }

    private void creerNotificationsDemo(Grossesse grossesse, Utilisateur patiente) {
        if (!notificationRepository.findByDestinataireIdOrderByDateEnvoiDesc(patiente.getId()).isEmpty()) {
            return;
        }

        Notification notificationRendezVous = new Notification();
        notificationRendezVous.setDestinataire(patiente);
        notificationRendezVous.setGrossesse(grossesse);
        notificationRendezVous.setType(TypeNotification.RAPPEL_CONSULTATION);
        notificationRendezVous.setTitre("Rendez-vous de démonstration");
        notificationRendezVous.setMessage("DONNÉE DE TEST — rendez-vous prénatal prévu prochainement.");
        notificationRendezVous.setLue(false);
        notificationRepository.save(notificationRendezVous);

        Notification notificationConseil = new Notification();
        notificationConseil.setDestinataire(patiente);
        notificationConseil.setGrossesse(grossesse);
        notificationConseil.setType(TypeNotification.INFORMATION);
        notificationConseil.setTitre("Bienvenue dans le dossier de démonstration");
        notificationConseil.setMessage("Toutes les informations de ce dossier sont fictives et destinées aux tests.");
        notificationConseil.setLue(true);
        notificationRepository.save(notificationConseil);
    }
}
