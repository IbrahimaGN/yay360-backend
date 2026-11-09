package sn.yay360.api.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import sn.yay360.api.dto.*;
import sn.yay360.api.exception.AuthentificationException;
import sn.yay360.api.exception.TropDeDemandesException;
import sn.yay360.api.mapper.UtilisateurMapper;
import sn.yay360.api.model.CodeVerification;
import sn.yay360.api.model.JetonRafraichissement;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.CodeVerificationRepository;
import sn.yay360.api.repo.JetonRafraichissementRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import sn.yay360.api.security.JetonSecurise;
import sn.yay360.api.security.JwtUtil;
import sn.yay360.api.service.EmailService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MobileAuthHelperTest {

    private static final String TEL = "+221770000001";

    private UtilisateurRepository utilisateurs;
    private CodeVerificationRepository codes;
    private JetonRafraichissementRepository jetons;
    private EmailService email;
    private final BCryptPasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private MobileAuthHelper helper;
    private Utilisateur awa;

    @BeforeEach
    void setUp() {
        utilisateurs = mock(UtilisateurRepository.class);
        codes = mock(CodeVerificationRepository.class);
        jetons = mock(JetonRafraichissementRepository.class);
        email = mock(EmailService.class);
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.genererToken(any(), any(), any(), any())).thenReturn("jwt-acces");
        helper = new MobileAuthHelper(utilisateurs, codes, jetons, encodeur, jwt, new UtilisateurMapper(), email);

        awa = new Utilisateur();
        awa.setId(1L);
        awa.setPrenom("Awa");
        awa.setNom("Ndiaye");
        awa.setEmail("awa.ndiaye@exemple.sn");
        awa.setTelephone(TEL);
        awa.setRole(RoleUtilisateur.FEMME_ENCEINTE);
        awa.setActif(true);
        when(codes.save(any(CodeVerification.class))).thenAnswer(i -> i.getArgument(0));
        when(utilisateurs.save(any(Utilisateur.class))).thenAnswer(i -> i.getArgument(0));
        when(jetons.save(any(JetonRafraichissement.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void compteExistant() {
        when(utilisateurs.findByTelephoneAndRoleIn(eq(TEL), any())).thenReturn(List.of(awa));
    }

    private MobileDemarrerRequest demarrer() {
        MobileDemarrerRequest r = new MobileDemarrerRequest();
        r.setTelephone(TEL);
        return r;
    }

    /** Envoie un code et renvoie sa valeur en clair, lue dans l'email envoyé. */
    private String envoyerCodeEtLire(CodeVerification[] stocke) {
        compteExistant();
        ArgumentCaptor<CodeVerification> captureCode = ArgumentCaptor.forClass(CodeVerification.class);
        helper.demarrer(demarrer());
        verify(codes, atLeastOnce()).save(captureCode.capture());
        stocke[0] = captureCode.getValue();
        ArgumentCaptor<String> lien = ArgumentCaptor.forClass(String.class);
        verify(email).envoyer(eq(awa.getEmail()), anyString(), anyString(), lien.capture());
        return lien.getValue();
    }

    @Nested
    class EnvoiDuCode {

        @Test
        void numeroInconnu_demandeLInscriptionSansEnvoyerDeCode() {
            when(utilisateurs.findByTelephoneAndRoleIn(eq(TEL), any())).thenReturn(List.of());

            MobileDemarrerResponse r = helper.demarrer(demarrer());

            assertThat(r.getEtape()).isEqualTo(MobileDemarrerResponse.INSCRIPTION);
            verifyNoInteractions(email);
        }

        @Test
        void numeroConnu_envoieUnCodeASixChiffresALEmailDuCompte_stockeHache() {
            CodeVerification[] stocke = new CodeVerification[1];
            String clair = envoyerCodeEtLire(stocke);

            assertThat(clair).matches("\\d{6}");
            assertThat(stocke[0].getCodeHache()).isNotEqualTo(clair).startsWith("$2");
            assertThat(encodeur.matches(clair, stocke[0].getCodeHache())).isTrue();
            assertThat(stocke[0].getExpiration()).isAfter(LocalDateTime.now().plusMinutes(9));
        }

        @Test
        void compteAvecPin_demandeLePinSansEnvoyerDEmail() {
            compteExistant();
            awa.setPin(encodeur.encode("2580"));

            assertThat(helper.demarrer(demarrer()).getEtape()).isEqualTo(MobileDemarrerResponse.PIN);
            verifyNoInteractions(email);
        }

        @Test
        void pinOublie_envoieUnCodeMemeSiUnPinExiste() {
            compteExistant();
            awa.setPin(encodeur.encode("2580"));

            assertThat(helper.pinOublie(demarrer()).getEtape()).isEqualTo(MobileDemarrerResponse.CODE_ENVOYE);
            verify(email).envoyer(eq(awa.getEmail()), anyString(), anyString(), anyString());
        }

        @Test
        void reponseAvecEmailMasque() {
            compteExistant();

            assertThat(helper.demarrer(demarrer()).getEmailMasque()).isEqualTo("a******@exemple.sn");
        }

        @Test
        void renvoiTropRapproche_refuse429() {
            compteExistant();
            CodeVerification recent = new CodeVerification();
            recent.setDateEnvoi(LocalDateTime.now().minusSeconds(20));
            when(codes.findFirstByTelephoneOrderByDateEnvoiDesc(TEL)).thenReturn(Optional.of(recent));

            assertThatThrownBy(() -> helper.demarrer(demarrer()))
                    .isInstanceOf(TropDeDemandesException.class).hasMessageContaining("Patientez");
            verifyNoInteractions(email);
        }

        @Test
        void tropDeCodesDansLHeure_refuse429() {
            compteExistant();
            when(codes.countByTelephoneAndDateEnvoiAfter(eq(TEL), any())).thenReturn(5L);

            assertThatThrownBy(() -> helper.demarrer(demarrer())).isInstanceOf(TropDeDemandesException.class);
        }
    }

    @Nested
    class VerificationDuCode {

        private MobileVerifierCodeRequest saisie(String code) {
            MobileVerifierCodeRequest r = new MobileVerifierCodeRequest();
            r.setTelephone(TEL);
            r.setCode(code);
            return r;
        }

        private CodeVerification codeEnCours(String clair) {
            CodeVerification c = new CodeVerification();
            c.setTelephone(TEL);
            c.setUtilisateur(awa);
            c.setCodeHache(encodeur.encode(clair));
            c.setDateEnvoi(LocalDateTime.now());
            c.setExpiration(LocalDateTime.now().plusMinutes(10));
            when(codes.findFirstByTelephoneAndUtiliseFalseOrderByDateEnvoiDesc(TEL)).thenReturn(Optional.of(c));
            return c;
        }

        @Test
        void bonCode_donneUnJetonPinAUsageUnique() {
            CodeVerification c = codeEnCours("123456");

            MobileVerifierCodeResponse r = helper.verifierCode(saisie("123456"));

            assertThat(r.getJetonPin()).isNotBlank();
            assertThat(c.getUtilise()).isTrue();
            assertThat(c.getJetonPinHache()).isEqualTo(JetonSecurise.hacher(r.getJetonPin()));
        }

        @Test
        void mauvaisCode_compteLesEssais_puisInvalideLeCode() {
            CodeVerification c = codeEnCours("123456");

            assertThatThrownBy(() -> helper.verifierCode(saisie("000000")))
                    .isInstanceOf(AuthentificationException.class).hasMessageContaining("Essais restants : 4");
            c.setEssais(4);
            assertThatThrownBy(() -> helper.verifierCode(saisie("000000")))
                    .hasMessageContaining("Demandez un nouveau code");
            assertThat(c.getUtilise()).isTrue();
        }

        @Test
        void codeExpire_refuse() {
            CodeVerification c = codeEnCours("123456");
            c.setExpiration(LocalDateTime.now().minusSeconds(1));

            assertThatThrownBy(() -> helper.verifierCode(saisie("123456")))
                    .isInstanceOf(AuthentificationException.class).hasMessageContaining("expiré");
        }

        @Test
        void inscription_leCompteNEstCreeQuApresLeBonCode() {
            CodeVerification c = codeEnCours("654321");
            c.setUtilisateur(null);
            c.setInscNom("Diop");
            c.setInscPrenom("Fatou");
            c.setInscEmail("fatou.diop@exemple.sn");
            c.setInscDateNaissance(LocalDate.of(1998, 3, 1));

            helper.verifierCode(saisie("654321"));

            ArgumentCaptor<Utilisateur> cree = ArgumentCaptor.forClass(Utilisateur.class);
            verify(utilisateurs).save(cree.capture());
            assertThat(cree.getValue().getRole()).isEqualTo(RoleUtilisateur.FEMME_ENCEINTE);
            assertThat(cree.getValue().getTelephone()).isEqualTo(TEL);
            assertThat(cree.getValue().getPin()).isNull();
        }
    }

    @Nested
    class DefinitionDuPin {

        private CodeVerification jetonValide(String jeton) {
            CodeVerification c = new CodeVerification();
            c.setUtilisateur(awa);
            c.setJetonPinHache(JetonSecurise.hacher(jeton));
            c.setJetonPinExpiration(LocalDateTime.now().plusMinutes(10));
            when(codes.findByJetonPinHache(JetonSecurise.hacher(jeton))).thenReturn(Optional.of(c));
            return c;
        }

        private MobileDefinirPinRequest pin(String jeton, String pin, boolean rester) {
            MobileDefinirPinRequest r = new MobileDefinirPinRequest();
            r.setJetonPin(jeton);
            r.setPin(pin);
            r.setResterConnecte(rester);
            return r;
        }

        @Test
        void pinTropFacile_refuse() {
            jetonValide("j1");

            for (String faible : List.of("0000", "1234", "4321", "7777", "6789")) {
                assertThatThrownBy(() -> helper.definirPin(pin("j1", faible, false)))
                        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("trop facile");
            }
            assertThat(MobileAuthHelper.faiblessePin("2580")).isNull();
        }

        @Test
        void pinValide_hacheEtSessionLongueSiResterConnecte() {
            CodeVerification c = jetonValide("j2");

            LoginResponse r = helper.definirPin(pin("j2", "2580", true));

            assertThat(awa.getPin()).isNotEqualTo("2580");
            assertThat(encodeur.matches("2580", awa.getPin())).isTrue();
            assertThat(r.getToken()).isEqualTo("jwt-acces");
            assertThat(r.getJetonRafraichissement()).isNotBlank();
            assertThat(c.getJetonPinHache()).isNull(); // jeton consommé
        }

        @Test
        void sansResterConnecte_pasDeJetonDeSession() {
            jetonValide("j3");

            assertThat(helper.definirPin(pin("j3", "2580", false)).getJetonRafraichissement()).isNull();
            verifyNoInteractions(jetons);
        }

        @Test
        void jetonExpire_refuse() {
            CodeVerification c = jetonValide("j4");
            c.setJetonPinExpiration(LocalDateTime.now().minusSeconds(1));

            assertThatThrownBy(() -> helper.definirPin(pin("j4", "2580", false)))
                    .isInstanceOf(AuthentificationException.class);
        }
    }

    @Nested
    class ConnexionParPin {

        private MobileConnexionPinRequest saisie(String pin) {
            MobileConnexionPinRequest r = new MobileConnexionPinRequest();
            r.setTelephone(TEL);
            r.setPin(pin);
            return r;
        }

        @BeforeEach
        void pinDefini() {
            compteExistant();
            awa.setPin(encodeur.encode("2580"));
        }

        @Test
        void bonPin_connecte() {
            assertThat(helper.connecter(saisie("2580")).getToken()).isEqualTo("jwt-acces");
        }

        @Test
        void cinqEchecs_verrouille15Minutes_memeAvecLeBonPin() {
            for (int i = 0; i < 5; i++) {
                assertThatThrownBy(() -> helper.connecter(saisie("1111"))).isInstanceOf(AuthentificationException.class);
            }
            assertThat(awa.getPinVerrouilleJusqua()).isAfter(LocalDateTime.now().plusMinutes(14));
            assertThatThrownBy(() -> helper.connecter(saisie("2580"))).isInstanceOf(AuthentificationException.class);
        }

        @Test
        void verrouillageProgressif_puisPinEfface() {
            awa.setPinVerrouillages(1);
            awa.setPinEchecs(4);
            assertThatThrownBy(() -> helper.connecter(saisie("1111"))).isInstanceOf(AuthentificationException.class);
            assertThat(awa.getPinVerrouilleJusqua()).isAfter(LocalDateTime.now().plusMinutes(29)); // 2e : 30 min

            awa.setPinVerrouilleJusqua(null);
            awa.setPinEchecs(4);
            assertThatThrownBy(() -> helper.connecter(saisie("1111"))).isInstanceOf(AuthentificationException.class);
            assertThat(awa.getPin()).isNull(); // 3e : nouveau code exigé
            verify(jetons).revoquerTousPourUtilisateur(1L);
        }

        @Test
        void numeroInconnu_memeMessageQuUnMauvaisPin() {
            when(utilisateurs.findByTelephoneAndRoleIn(eq(TEL), any())).thenReturn(List.of());

            assertThatThrownBy(() -> helper.connecter(saisie("2580")))
                    .hasMessage(MobileAuthHelper.IDENTIFIANTS_INCORRECTS);
        }
    }

    @Nested
    class ResterConnecte {

        private JetonRafraichissement jeton(String clair, boolean revoque, LocalDateTime expiration) {
            JetonRafraichissement j = new JetonRafraichissement();
            j.setUtilisateur(awa);
            j.setJetonHache(JetonSecurise.hacher(clair));
            j.setFamille("famille-1");
            j.setRevoque(revoque);
            j.setExpiration(expiration);
            when(jetons.findByJetonHache(JetonSecurise.hacher(clair))).thenReturn(Optional.of(j));
            return j;
        }

        private MobileJetonRequest requete(String clair) {
            MobileJetonRequest r = new MobileJetonRequest();
            r.setJetonRafraichissement(clair);
            return r;
        }

        @Test
        void rotation_lAncienEstRevoque_unNouveauEstEmisDansLaMemeFamille() {
            JetonRafraichissement ancien = jeton("ancien", false, LocalDateTime.now().plusDays(10));

            LoginResponse r = helper.rafraichir(requete("ancien"));

            assertThat(ancien.getRevoque()).isTrue();
            assertThat(r.getJetonRafraichissement()).isNotBlank().isNotEqualTo("ancien");
            ArgumentCaptor<JetonRafraichissement> nouveau = ArgumentCaptor.forClass(JetonRafraichissement.class);
            verify(jetons, times(2)).save(nouveau.capture());
            assertThat(nouveau.getAllValues().get(1).getFamille()).isEqualTo("famille-1");
            assertThat(nouveau.getAllValues().get(1).getExpiration()).isAfter(LocalDateTime.now().plusDays(29));
        }

        @Test
        void reutilisationDUnJetonRevoque_fermeToutesLesSessionsDeLaFamille() {
            jeton("vole", true, LocalDateTime.now().plusDays(10));

            assertThatThrownBy(() -> helper.rafraichir(requete("vole"))).isInstanceOf(AuthentificationException.class);
            verify(jetons).revoquerFamille("famille-1");
        }

        @Test
        void jetonExpire_refuse() {
            jeton("vieux", false, LocalDateTime.now().minusMinutes(1));

            assertThatThrownBy(() -> helper.rafraichir(requete("vieux"))).isInstanceOf(AuthentificationException.class);
        }

        @Test
        void deconnexion_revoqueLaFamille() {
            jeton("courant", false, LocalDateTime.now().plusDays(10));

            helper.deconnecter(requete("courant"));

            verify(jetons).revoquerFamille("famille-1");
        }
    }

    @Test
    void masquageDeLEmail() {
        assertThat(MobileAuthHelper.masquerEmail("fatou@gmail.com")).isEqualTo("f****@gmail.com");
        assertThat(MobileAuthHelper.masquerEmail("a@b.sn")).isEqualTo("*@b.sn");
    }

    @Test
    void valeursParDefautAlignéesSurLaConfiguration() {
        assertThat(ReflectionTestUtils.getField(helper, "codeDureeMinutes")).isEqualTo(10L);
        assertThat(ReflectionTestUtils.getField(helper, "sessionJours")).isEqualTo(30L);
    }
}
