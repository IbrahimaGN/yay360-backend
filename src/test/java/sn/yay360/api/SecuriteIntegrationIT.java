package sn.yay360.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration complet : contexte Spring, Flyway, filtre JWT et sécurité des méthodes
 * (@PreAuthorize) sur une vraie base PostgreSQL. Les tests unitaires ne peuvent pas vérifier
 * ces annotations : ils appellent les helpers sans passer par les proxys Spring.
 *
 * <p>Exécution : définir YAY360_IT_DATASOURCE_URL (base vide dédiée), par exemple
 * {@code jdbc:postgresql://localhost:5433/yay360_it}, et YAY360_IT_DATASOURCE_PASSWORD.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "YAY360_IT_DATASOURCE_URL", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecuriteIntegrationIT {

    private static final String ADMIN_MDP = "Admin-IT-2026";
    private static final String SUFFIXE = String.valueOf(System.nanoTime());

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    @DynamicPropertySource
    static void configuration(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("YAY360_IT_DATASOURCE_URL"));
        registry.add("spring.datasource.password", () -> System.getenv("YAY360_IT_DATASOURCE_PASSWORD"));
        registry.add("jwt.secret", () -> "secret-des-tests-d-integration-yay360-0123456789");
        registry.add("yay360.admin.password", () -> ADMIN_MDP);
        registry.add("yay360.demo-data.enabled", () -> "false");
    }

    // ==================== OUTILS ====================

    private String connexion(String email, String motDePasse) throws Exception {
        MvcResult r = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + motDePasse + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return lire(r).get("token").asText();
    }

    private JsonNode lire(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private String bearer(String jeton) {
        return "Bearer " + jeton;
    }

    private String inscrirePatiente(String prefixe) throws Exception {
        String email = prefixe + SUFFIXE + "@it.sn";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nom":"Diop","prenom":"Awa","email":"%s","password":"Passw0rd1",
                         "telephone":"+221771234567","dateNaissance":"1996-04-02"}""".formatted(email)))
                .andExpect(status().isCreated());
        return connexion(email, "Passw0rd1");
    }

    private long declarerGrossesse(String jeton) throws Exception {
        MvcResult r = mvc.perform(post("/grossesses").header("Authorization", bearer(jeton))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateDernieresRegles\":\"" + LocalDate.now().minusWeeks(22) + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return lire(r).get("id").asLong();
    }

    /** Administrateur d'un établissement dédié aux tests, créé une fois : il crée les soignants. */
    private static String jetonAdminSoignants;

    private String creerProfessionnel(String role, String prefixe) throws Exception {
        if (jetonAdminSoignants == null) {
            long etablissement = creerEtablissement("Centre des soignants IT");
            jetonAdminSoignants = creerCompte(connexion("admin@yay360.sn", ADMIN_MDP), "ADMIN_ETABLISSEMENT",
                    "admsoins", etablissement);
        }
        return creerCompte(jetonAdminSoignants, role, prefixe, null);
    }

    /** Crée un compte par l'administrateur donné, change le mot de passe temporaire et renvoie le jeton. */
    private String creerCompte(String jetonAuteur, String role, String prefixe, Long etablissementId) throws Exception {
        String email = prefixe + SUFFIXE + "@hopital.sn";
        MvcResult r = mvc.perform(post("/utilisateurs/professionnels").header("Authorization", bearer(jetonAuteur))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nom":"Sarr","prenom":"Fatou","email":"%s","telephone":"+221781234567",
                                 "dateNaissance":"1984-03-01","role":"%s","etablissementId":%s}"""
                                .formatted(email, role, etablissementId)))
                .andExpect(status().isCreated()).andReturn();
        String temporaire = connexion(email, lire(r).get("motDePasseTemporaire").asText());
        // Première connexion : le mot de passe temporaire doit être changé avant tout usage
        MvcResult change = mvc.perform(put("/auth/change-password").header("Authorization", bearer(temporaire))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ancienMotDePasse\":\"" + lire(r).get("motDePasseTemporaire").asText()
                                + "\",\"nouveauMotDePasse\":\"Soignant2026\"}"))
                .andExpect(status().isOk()).andReturn();
        return lire(change).get("token").asText();
    }

    private long creerEtablissement(String nom) throws Exception {
        String admin = connexion("admin@yay360.sn", ADMIN_MDP);
        MvcResult e = mvc.perform(post("/etablissements").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nom":"%s %s","type":"CENTRE_DE_SANTE","adresse":"Dakar",
                                 "telephone":"+221338000000"}""".formatted(nom, SUFFIXE)))
                .andExpect(status().isCreated()).andReturn();
        return lire(e).get("id").asLong();
    }

    private long idDe(String jeton) throws Exception {
        MvcResult r = mvc.perform(get("/utilisateurs/profil").header("Authorization", bearer(jeton)))
                .andExpect(status().isOk()).andReturn();
        return lire(r).get("id").asLong();
    }

    // ==================== AUTHENTIFICATION ====================

    @Test
    @Order(1)
    void sansJetonUneRessourceProtegeeRenvoie401EnJson() throws Exception {
        mvc.perform(get("/grossesses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void lInscriptionPubliqueRefuseLeRoleAdmin() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nom":"Pirate","prenom":"Admin","email":"pirate%s@it.sn","password":"Passw0rd1",
                         "telephone":"+221771234567","dateNaissance":"1990-01-01","role":"ADMIN"}""".formatted(SUFFIXE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changerSonMotDePasseExigeUnJeton() throws Exception {
        mvc.perform(put("/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ancienMotDePasse\":\"a\",\"nouveauMotDePasse\":\"Passw0rd2\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== SÉCURITÉ DES MÉTHODES (@PreAuthorize) ====================

    @Test
    void unePatienteNAccedePasAuTableauDeBordNiALaListeDesUtilisateurs() throws Exception {
        String patiente = inscrirePatiente("dash");
        mvc.perform(get("/dashboard").header("Authorization", bearer(patiente))).andExpect(status().isForbidden());
        mvc.perform(get("/utilisateurs").header("Authorization", bearer(patiente))).andExpect(status().isForbidden());
    }

    @Test
    void seulUnMedecinPeutDeclarerUneGrossesseARisque() throws Exception {
        long grossesse = declarerGrossesse(inscrirePatiente("risque"));
        String sageFemme = creerProfessionnel("SAGE_FEMME", "sf");

        mvc.perform(put("/grossesses/" + grossesse + "/risque").header("Authorization", bearer(sageFemme))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"HTA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void leTableauDeBordAdministrateurFonctionne() throws Exception {
        String admin = connexion("admin@yay360.sn", ADMIN_MDP);
        MvcResult r = mvc.perform(get("/dashboard").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn();
        assertThat(lire(r).has("tauxResolutionAlertes")).isTrue();
    }

    // ==================== CLOISONNEMENT DES DOSSIERS ====================

    @Test
    void unMedecinNeLitPasLeDossierDUnePatienteQuIlNeSuitPas() throws Exception {
        long grossesse = declarerGrossesse(inscrirePatiente("cloison"));
        String medecin = creerProfessionnel("MEDECIN", "doc");

        mvc.perform(get("/grossesses/" + grossesse).header("Authorization", bearer(medecin)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/consultations/grossesse/" + grossesse).header("Authorization", bearer(medecin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unePatienteNeLitPasLaGrossesseDUneAutre() throws Exception {
        long grossesseA = declarerGrossesse(inscrirePatiente("a"));
        String patienteB = inscrirePatiente("b");

        mvc.perform(get("/grossesses/" + grossesseA).header("Authorization", bearer(patienteB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void uneSecondeGrossesseEnCoursEstRefusee() throws Exception {
        String patiente = inscrirePatiente("double");
        declarerGrossesse(patiente);

        mvc.perform(post("/grossesses").header("Authorization", bearer(patiente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateDernieresRegles\":\"" + LocalDate.now().minusWeeks(10) + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void lAlerteDUnePatienteSansSuiviPeutEtrePriseEnChargeEtCreeLeLienDeSoin() throws Exception {
        String patiente = inscrirePatiente("alerte");
        long grossesse = declarerGrossesse(patiente);
        String medecin = creerProfessionnel("MEDECIN", "urg");

        MvcResult r = mvc.perform(post("/alertes").header("Authorization", bearer(patiente))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"typeSymptome\":\"SAIGNEMENTS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.niveauGravite").value("URGENT"))
                .andReturn();
        long alerte = lire(r).get("id").asLong();

        mvc.perform(put("/alertes/" + alerte + "/prendre-en-charge").header("Authorization", bearer(medecin)))
                .andExpect(status().isOk());
        mvc.perform(get("/grossesses/" + grossesse).header("Authorization", bearer(medecin)))
                .andExpect(status().isOk());
    }

    @Test
    void unCompteAvecMotDePasseTemporaireNePeutQueLeChanger() throws Exception {
        long etablissement = creerEtablissement("Centre temporaire");
        String admin = creerCompte(connexion("admin@yay360.sn", ADMIN_MDP), "ADMIN_ETABLISSEMENT", "admtemp", etablissement);
        String email = "temp" + SUFFIXE + "@hopital.sn";
        MvcResult r = mvc.perform(post("/utilisateurs/professionnels").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nom":"Ba","prenom":"Aminata","email":"%s","telephone":"+221781234567",
                                 "dateNaissance":"1984-03-01","role":"SAGE_FEMME"}""".formatted(email)))
                .andExpect(status().isCreated()).andReturn();
        String jeton = connexion(email, lire(r).get("motDePasseTemporaire").asText());

        mvc.perform(get("/grossesses").header("Authorization", bearer(jeton))).andExpect(status().isForbidden());
        mvc.perform(get("/utilisateurs/profil").header("Authorization", bearer(jeton))).andExpect(status().isOk());
    }

    @Test
    void lAlerteDUnePatienteRattacheeAUnEtablissementSansSoignantResteprenable() throws Exception {
        String admin = connexion("admin@yay360.sn", ADMIN_MDP);
        MvcResult e = mvc.perform(post("/etablissements").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nom":"Poste de santé vide %s","type":"POSTE_DE_SANTE","adresse":"Kaolack",
                                 "telephone":"+221339000000"}""".formatted(SUFFIXE)))
                .andExpect(status().isCreated()).andReturn();
        long etablissement = lire(e).get("id").asLong();

        String patiente = inscrirePatiente("vide");
        declarerGrossesse(patiente);
        mvc.perform(put("/grossesses/ma-grossesse/etablissement").header("Authorization", bearer(patiente))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"etablissementId\":" + etablissement + "}"))
                .andExpect(status().isOk());
        MvcResult a = mvc.perform(post("/alertes").header("Authorization", bearer(patiente))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"typeSymptome\":\"CONVULSIONS\"}"))
                .andExpect(status().isCreated()).andReturn();
        long alerte = lire(a).get("id").asLong();

        String medecin = creerProfessionnel("MEDECIN", "garde");
        mvc.perform(get("/alertes/" + alerte).header("Authorization", bearer(medecin))).andExpect(status().isOk());
        mvc.perform(put("/alertes/" + alerte + "/prendre-en-charge").header("Authorization", bearer(medecin)))
                .andExpect(status().isOk());
    }

    @Test
    void connexionMobile_routesPubliquesEtCodeEnregistreEnBase() throws Exception {
        String telephone = "+22178" + SUFFIXE.substring(SUFFIXE.length() - 7);
        // Numéro inconnu : inscription demandée, sans jeton
        mvc.perform(post("/auth/mobile/demarrer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":\"" + telephone + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etape").value("INSCRIPTION"));
        // Inscription : code créé en base (table codes_verification, migration V7), email masqué
        mvc.perform(post("/auth/mobile/inscription").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nom":"Sarr","prenom":"Khady","email":"khady.mobile.%s@exemple.sn",
                         "telephone":"%s","dateNaissance":"1997-04-12"}""".formatted(SUFFIXE, telephone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etape").value("CODE_ENVOYE"))
                .andExpect(jsonPath("$.emailMasque").value(org.hamcrest.Matchers.startsWith("k")));
        // Renvoi immédiat refusé (429)
        mvc.perform(post("/auth/mobile/inscription").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nom":"Sarr","prenom":"Khady","email":"khady.mobile.%s@exemple.sn",
                         "telephone":"%s","dateNaissance":"1997-04-12"}""".formatted(SUFFIXE, telephone)))
                .andExpect(status().isTooManyRequests());
        // Mauvais code : 401 avec le nombre d'essais restants
        mvc.perform(post("/auth/mobile/verifier-code").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":\"" + telephone + "\",\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Essais restants")));
        // PIN sans compte : refus générique ; jeton de session inconnu : 401
        mvc.perform(post("/auth/mobile/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":\"" + telephone + "\",\"pin\":\"2580\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/mobile/rafraichir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jetonRafraichissement\":\"inconnu\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportPdfDuDossier_reserveALaPatienteEtAuxSoignantsQuiLaSuivent() throws Exception {
        String patiente = inscrirePatiente("pdf");
        long grossesse = declarerGrossesse(patiente);
        MvcResult r = mvc.perform(get("/grossesses/ma-grossesse/export-pdf").header("Authorization", bearer(patiente)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        assertThat(new String(r.getResponse().getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");
        assertThat(r.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");

        mvc.perform(get("/grossesses/ma-grossesse/export-pdf")).andExpect(status().isUnauthorized());
        String admin = connexion("admin@yay360.sn", ADMIN_MDP);
        mvc.perform(get("/grossesses/" + grossesse + "/export-pdf").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        String medecin = creerProfessionnel("MEDECIN", "pdf");
        mvc.perform(get("/grossesses/" + grossesse + "/export-pdf").header("Authorization", bearer(medecin)))
                .andExpect(status().isForbidden());
    }

    // ==================== DEUX NIVEAUX D'ADMINISTRATION ====================

    @Test
    void lAdministrateurDeLaPlateformeNAccedeAAucuneDonneeDeSante() throws Exception {
        long grossesse = declarerGrossesse(inscrirePatiente("superadmin"));
        String admin = connexion("admin@yay360.sn", ADMIN_MDP);

        for (String route : new String[]{"/grossesses", "/grossesses/a-risque", "/grossesses/" + grossesse,
                "/grossesses/" + grossesse + "/calendrier", "/grossesses/" + grossesse + "/suivi",
                "/grossesses/" + grossesse + "/plan-cpn", "/consultations/grossesse/" + grossesse,
                "/rendez-vous?statut=PLANIFIE", "/alertes?statut=NOUVELLE", "/alertes/urgentes"}) {
            mvc.perform(get(route).header("Authorization", bearer(admin))).andExpect(status().isForbidden());
        }
        // Il garde l'administration : comptes et établissements
        mvc.perform(get("/utilisateurs").header("Authorization", bearer(admin))).andExpect(status().isOk());
        mvc.perform(get("/etablissements/tous").header("Authorization", bearer(admin))).andExpect(status().isOk());
    }

    @Test
    void lAdministrateurDEtablissementNeGereQueSonEtablissement() throws Exception {
        long sien = creerEtablissement("Centre A");
        long autre = creerEtablissement("Centre B");
        String adminA = creerCompte(connexion("admin@yay360.sn", ADMIN_MDP), "ADMIN_ETABLISSEMENT", "adma", sien);

        // Son établissement, et lui seul
        mvc.perform(get("/etablissements/mon-etablissement").header("Authorization", bearer(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sien));
        mvc.perform(put("/etablissements/" + autre).header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nom":"Pris","type":"CENTRE_DE_SANTE","adresse":"Dakar","telephone":"+221338000000"}"""))
                .andExpect(status().isForbidden());
        mvc.perform(post("/etablissements").header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nom":"Nouveau","type":"CENTRE_DE_SANTE","adresse":"Dakar","telephone":"+221338000000"}"""))
                .andExpect(status().isForbidden());

        // Il crée un soignant : rattaché d'office à son établissement, même s'il en demande un autre
        String sageFemme = creerCompte(adminA, "SAGE_FEMME", "sfa", autre);
        long idSageFemme = idDe(sageFemme);
        MvcResult soignants = mvc.perform(get("/utilisateurs/mon-etablissement").header("Authorization", bearer(adminA)))
                .andExpect(status().isOk()).andReturn();
        assertThat(lire(soignants).findValuesAsText("id")).contains(String.valueOf(idSageFemme));
        mvc.perform(get("/etablissements/mes-etablissements").header("Authorization", bearer(sageFemme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(sien));

        // Il ne crée pas d'administrateur et ne voit ni toute la plateforme ni le tableau de bord
        mvc.perform(post("/utilisateurs/professionnels").header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nom":"Faye","prenom":"Modou","email":"pirate%s@hopital.sn","telephone":"+221781234567",
                                 "dateNaissance":"1984-03-01","role":"ADMIN"}""".formatted(SUFFIXE)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/utilisateurs").header("Authorization", bearer(adminA))).andExpect(status().isForbidden());
        mvc.perform(get("/dashboard").header("Authorization", bearer(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreEtablissementsActifs").value(1))
                .andExpect(jsonPath("$.activiteEtablissements[0].etablissementId").value(sien))
                .andExpect(jsonPath("$.nombreSagesFemmes").value(1))
                .andExpect(jsonPath("$.nombreAdmins").value(1));

        // Il désactive un soignant de son établissement ; l'administrateur d'un autre ne peut pas le réactiver
        mvc.perform(put("/utilisateurs/" + idSageFemme + "/desactiver").header("Authorization", bearer(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false));
        String adminB = creerCompte(connexion("admin@yay360.sn", ADMIN_MDP), "ADMIN_ETABLISSEMENT", "admb", autre);
        mvc.perform(put("/utilisateurs/" + idSageFemme + "/activer").header("Authorization", bearer(adminB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void lAdministrateurDEtablissementNAccedeAAucuneDonneeDeSante() throws Exception {
        long etablissement = creerEtablissement("Centre C");
        String patiente = inscrirePatiente("admetab");
        long grossesse = declarerGrossesse(patiente);
        mvc.perform(put("/grossesses/ma-grossesse/etablissement").header("Authorization", bearer(patiente))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"etablissementId\":" + etablissement + "}"))
                .andExpect(status().isOk());
        String admin = creerCompte(connexion("admin@yay360.sn", ADMIN_MDP), "ADMIN_ETABLISSEMENT", "admc", etablissement);

        for (String route : new String[]{"/grossesses", "/grossesses/" + grossesse,
                "/grossesses/" + grossesse + "/export-pdf", "/consultations/grossesse/" + grossesse,
                "/rendez-vous?statut=PLANIFIE", "/alertes?statut=NOUVELLE"}) {
            mvc.perform(get(route).header("Authorization", bearer(admin))).andExpect(status().isForbidden());
        }
    }

    @Test
    void lAdministrateurDeLaPlateformeNeCreeNiNAffecteAucunSoignant() throws Exception {
        String admin = connexion("admin@yay360.sn", ADMIN_MDP);
        long etablissement = creerEtablissement("Centre D");

        for (String role : new String[]{"MEDECIN", "SAGE_FEMME"}) {
            mvc.perform(post("/utilisateurs/professionnels").header("Authorization", bearer(admin))
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"nom":"Faye","prenom":"Modou","email":"%s%s@hopital.sn","telephone":"+221781234567",
                                     "dateNaissance":"1984-03-01","role":"%s","etablissementId":%s}"""
                                    .formatted(role.toLowerCase(), SUFFIXE, role, etablissement)))
                    .andExpect(status().isForbidden());
        }
        long soignant = idDe(creerProfessionnel("MEDECIN", "nonaffecte"));
        mvc.perform(post("/etablissements/" + etablissement + "/professionnels").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"professionnelId\":" + soignant + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/etablissements/" + etablissement + "/professionnels/" + soignant)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        // Les conseils et étapes sont réservés aux administrateurs d'établissement et aux soignants
        mvc.perform(post("/conseils").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"semaineDebut":10,"semaineFin":12,"categorie":"NUTRITION","titre":"Fer","contenu":"Test"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void lAdministrateurDeLaPlateformeCreeUnAutreAdministrateurDeLaPlateforme() throws Exception {
        String nouveau = creerCompte(connexion("admin@yay360.sn", ADMIN_MDP), "ADMIN", "superadm", null);

        mvc.perform(get("/utilisateurs").header("Authorization", bearer(nouveau))).andExpect(status().isOk());
        mvc.perform(get("/grossesses").header("Authorization", bearer(nouveau))).andExpect(status().isForbidden());
    }

    @Test
    void unEtablissementNAQuUnSeulAdministrateur() throws Exception {
        long etablissement = creerEtablissement("Centre E");
        String admin = connexion("admin@yay360.sn", ADMIN_MDP);
        creerCompte(admin, "ADMIN_ETABLISSEMENT", "adme1", etablissement);

        mvc.perform(post("/utilisateurs/professionnels").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nom":"Ka","prenom":"Ndeye","email":"adme2%s@hopital.sn","telephone":"+221781234567",
                                 "dateNaissance":"1984-03-01","role":"ADMIN_ETABLISSEMENT","etablissementId":%s}"""
                                .formatted(SUFFIXE, etablissement)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cet établissement a déjà un administrateur"));
    }

    @Test
    void unAdministrateurDEtablissementExigeUnEtablissement() throws Exception {
        String admin = connexion("admin@yay360.sn", ADMIN_MDP);
        mvc.perform(post("/utilisateurs/professionnels").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nom":"Ka","prenom":"Ndeye","email":"sansetab%s@hopital.sn","telephone":"+221781234567",
                                 "dateNaissance":"1984-03-01","role":"ADMIN_ETABLISSEMENT"}""".formatted(SUFFIXE)))
                .andExpect(status().isBadRequest());
    }
}
