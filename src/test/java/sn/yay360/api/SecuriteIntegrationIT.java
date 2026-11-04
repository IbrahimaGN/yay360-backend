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

    private String creerProfessionnel(String role, String prefixe) throws Exception {
        String admin = connexion("admin@yay360.sn", ADMIN_MDP);
        String email = prefixe + SUFFIXE + "@hopital.sn";
        MvcResult r = mvc.perform(post("/utilisateurs/professionnels").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nom":"Sarr","prenom":"Fatou","email":"%s","telephone":"+221781234567",
                                 "dateNaissance":"1984-03-01","role":"%s"}""".formatted(email, role)))
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
        String admin = connexion("admin@yay360.sn", ADMIN_MDP);
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
}
