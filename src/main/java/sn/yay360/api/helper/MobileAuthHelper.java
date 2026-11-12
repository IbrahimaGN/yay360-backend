package sn.yay360.api.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.yay360.api.dto.*;
import sn.yay360.api.exception.AuthentificationException;
import sn.yay360.api.exception.EmailDejaUtiliseException;
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

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Connexion mobile (patiente, conjoint) : téléphone → code à usage unique envoyé par e-mail →
 * PIN à 4 chiffres ; puis téléphone + PIN, avec session longue facultative (« rester connecté »).
 *
 * <p>Mesures reprises des recommandations OWASP (fiches Authentication, Multifactor Authentication et
 * OAuth 2.0) : code court, à usage unique, à durée de vie et nombre d'essais limités, renvois limités ;
 * codes et PIN hachés ; verrouillage progressif du PIN compté par compte ; jetons de session à rotation,
 * la réutilisation d'un jeton révoqué révoquant toute la session.</p>
 *
 * <p>Endpoints publics par nature (l'utilisateur n'est pas encore authentifié) : aucun {@code @PreAuthorize}.</p>
 */
@Component
@RequiredArgsConstructor
public class MobileAuthHelper {

    static final Set<String> ROLES_MOBILES = Set.of(RoleUtilisateur.FEMME_ENCEINTE, RoleUtilisateur.CONJOINT);
    static final String IDENTIFIANTS_INCORRECTS =
            "Numéro ou PIN incorrect. Après plusieurs échecs, réessayez dans quelques minutes "
                    + "ou utilisez « PIN oublié ».";

    private static final SecureRandom ALEATOIRE = new SecureRandom();

    private final UtilisateurRepository utilisateurRepository;
    private final CodeVerificationRepository codeRepository;
    private final JetonRafraichissementRepository jetonRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UtilisateurMapper utilisateurMapper;
    private final EmailService emailService;

    @Value("${yay360.mobile.code-duree-minutes:10}")
    private long codeDureeMinutes = 10;
    @Value("${yay360.mobile.code-essais-max:5}")
    private int codeEssaisMax = 5;
    @Value("${yay360.mobile.code-delai-renvoi-secondes:60}")
    private long delaiRenvoiSecondes = 60;
    @Value("${yay360.mobile.codes-max-par-heure:5}")
    private int codesMaxParHeure = 5;
    @Value("${yay360.mobile.jeton-pin-minutes:10}")
    private long jetonPinMinutes = 10;
    @Value("${yay360.mobile.pin-echecs-max:5}")
    private int pinEchecsMax = 5;
    @Value("${yay360.mobile.pin-verrouillage-minutes:15}")
    private long pinVerrouillageMinutes = 15;
    @Value("${yay360.mobile.pin-verrouillages-avant-code:3}")
    private int pinVerrouillagesAvantCode = 3;
    @Value("${yay360.mobile.session-jours:30}")
    private long sessionJours = 30;

    private volatile String empreinteFactice;

    // ==================== 1. TÉLÉPHONE ====================

    /**
     * Numéro avec PIN : demande le PIN (aucun envoi). Numéro sans PIN : envoie un code à l'e-mail du compte.
     * Numéro inconnu : demande l'inscription.
     */
    @Transactional(noRollbackFor = TropDeDemandesException.class)
    public MobileDemarrerResponse demarrer(MobileDemarrerRequest request) {
        return demarrer(request, false);
    }

    /** « PIN oublié » : envoie toujours un code, même si un PIN existe. */
    @Transactional(noRollbackFor = TropDeDemandesException.class)
    public MobileDemarrerResponse pinOublie(MobileDemarrerRequest request) {
        return demarrer(request, true);
    }

    private MobileDemarrerResponse demarrer(MobileDemarrerRequest request, boolean forcerCode) {
        String telephone = request.getTelephone().trim();
        Utilisateur utilisateur = compteMobile(telephone);
        if (utilisateur == null) {
            return new MobileDemarrerResponse(MobileDemarrerResponse.INSCRIPTION, null,
                    "Ce numéro n'a pas encore de compte : complétez l'inscription.");
        }
        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new AuthentificationException("Ce compte a été désactivé. Contactez votre structure de santé.");
        }
        if (!forcerCode && utilisateur.getPin() != null) {
            return new MobileDemarrerResponse(MobileDemarrerResponse.PIN, null, "Saisissez votre code PIN.");
        }
        CodeVerification code = nouveauCode(telephone);
        code.setUtilisateur(utilisateur);
        envoyerCode(code, utilisateur.getEmail(), utilisateur.getPrenom());
        return new MobileDemarrerResponse(MobileDemarrerResponse.CODE_ENVOYE, masquerEmail(utilisateur.getEmail()),
                "Un code à 6 chiffres vient d'être envoyé à votre adresse e-mail.");
    }

    // ==================== 2. INSCRIPTION ====================

    /** Mémorise l'inscription et envoie le code ; le compte n'est créé qu'une fois le code vérifié. */
    @Transactional(noRollbackFor = TropDeDemandesException.class)
    public MobileDemarrerResponse inscrire(MobileInscriptionRequest request) {
        String telephone = request.getTelephone().trim();
        String email = AuthHelper.normaliserEmail(request.getEmail());
        if (compteMobile(telephone) != null) {
            throw new EmailDejaUtiliseException("Un compte existe déjà avec ce numéro : connectez-vous avec votre PIN.");
        }
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailDejaUtiliseException("Un compte existe déjà avec cet email");
        }
        RegleAgeHelper.valider(RoleUtilisateur.FEMME_ENCEINTE, request.getDateNaissance());

        CodeVerification code = nouveauCode(telephone);
        code.setInscNom(request.getNom().trim());
        code.setInscPrenom(request.getPrenom().trim());
        code.setInscEmail(email);
        code.setInscDateNaissance(request.getDateNaissance());
        envoyerCode(code, email, request.getPrenom().trim());
        return new MobileDemarrerResponse(MobileDemarrerResponse.CODE_ENVOYE, masquerEmail(email),
                "Un code à 6 chiffres vient d'être envoyé à votre adresse e-mail.");
    }

    // ==================== 3. VÉRIFICATION DU CODE ====================

    @Transactional(noRollbackFor = AuthentificationException.class)
    public MobileVerifierCodeResponse verifierCode(MobileVerifierCodeRequest request) {
        String telephone = request.getTelephone().trim();
        CodeVerification code = codeRepository.findFirstByTelephoneAndUtiliseFalseOrderByDateEnvoiDesc(telephone)
                .orElseThrow(() -> new AuthentificationException("Aucun code en cours pour ce numéro : demandez-en un nouveau."));
        LocalDateTime maintenant = LocalDateTime.now();
        if (code.getExpiration().isBefore(maintenant) || code.getEssais() >= codeEssaisMax) {
            code.setUtilise(true);
            codeRepository.save(code);
            throw new AuthentificationException("Ce code a expiré ou a été trop souvent mal saisi : demandez-en un nouveau.");
        }
        if (!passwordEncoder.matches(request.getCode(), code.getCodeHache())) {
            code.setEssais(code.getEssais() + 1);
            int restants = codeEssaisMax - code.getEssais();
            if (restants <= 0) {
                code.setUtilise(true);
            }
            codeRepository.save(code);
            throw new AuthentificationException(restants > 0
                    ? "Code incorrect. Essais restants : " + restants + "."
                    : "Code incorrect. Demandez un nouveau code.");
        }

        if (code.getUtilisateur() == null) {
            code.setUtilisateur(creerCompte(code));
        }
        String jetonPin = JetonSecurise.generer();
        code.setUtilise(true);
        code.setJetonPinHache(JetonSecurise.hacher(jetonPin));
        code.setJetonPinExpiration(maintenant.plusMinutes(jetonPinMinutes));
        codeRepository.save(code);
        return new MobileVerifierCodeResponse(jetonPin, "Numéro vérifié. Choisissez votre code PIN à 4 chiffres.");
    }

    // ==================== 4. DÉFINITION DU PIN ====================

    @Transactional
    public LoginResponse definirPin(MobileDefinirPinRequest request) {
        CodeVerification code = codeRepository.findByJetonPinHache(JetonSecurise.hacher(request.getJetonPin()))
                .orElseThrow(() -> new AuthentificationException("Vérification expirée : recommencez depuis votre numéro."));
        if (code.getJetonPinExpiration() == null || code.getJetonPinExpiration().isBefore(LocalDateTime.now())) {
            throw new AuthentificationException("Vérification expirée : recommencez depuis votre numéro.");
        }
        String faiblesse = faiblessePin(request.getPin());
        if (faiblesse != null) {
            throw new IllegalArgumentException(faiblesse);
        }
        // Jeton à usage unique
        code.setJetonPinHache(null);
        code.setJetonPinExpiration(null);
        codeRepository.save(code);

        Utilisateur utilisateur = code.getUtilisateur();
        utilisateur.setPin(passwordEncoder.encode(request.getPin()));
        utilisateur.setPinEchecs(0);
        utilisateur.setPinVerrouillages(0);
        utilisateur.setPinVerrouilleJusqua(null);
        utilisateurRepository.save(utilisateur);
        return session(utilisateur, request.isResterConnecte(), null);
    }

    // ==================== 5. CONNEXION TÉLÉPHONE + PIN ====================

    @Transactional(noRollbackFor = AuthentificationException.class)
    public LoginResponse connecter(MobileConnexionPinRequest request) {
        Utilisateur utilisateur = compteMobile(request.getTelephone().trim());
        LocalDateTime maintenant = LocalDateTime.now();
        if (utilisateur == null || utilisateur.getPin() == null) {
            passwordEncoder.matches(request.getPin(), empreinteFactice()); // temps de réponse comparable
            throw new AuthentificationException(IDENTIFIANTS_INCORRECTS);
        }
        if (utilisateur.getPinVerrouilleJusqua() != null && utilisateur.getPinVerrouilleJusqua().isAfter(maintenant)) {
            passwordEncoder.matches(request.getPin(), empreinteFactice());
            throw new AuthentificationException(IDENTIFIANTS_INCORRECTS);
        }
        if (!passwordEncoder.matches(request.getPin(), utilisateur.getPin())) {
            enregistrerEchecPin(utilisateur, maintenant);
            throw new AuthentificationException(IDENTIFIANTS_INCORRECTS);
        }
        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new AuthentificationException("Ce compte a été désactivé. Contactez votre structure de santé.");
        }
        if (valeur(utilisateur.getPinEchecs()) > 0 || valeur(utilisateur.getPinVerrouillages()) > 0
                || utilisateur.getPinVerrouilleJusqua() != null) {
            utilisateur.setPinEchecs(0);
            utilisateur.setPinVerrouillages(0);
            utilisateur.setPinVerrouilleJusqua(null);
            utilisateurRepository.save(utilisateur);
        }
        return session(utilisateur, request.isResterConnecte(), null);
    }

    // ==================== 6. RESTER CONNECTÉ ====================

    /** Rotation : l'ancien jeton est révoqué, un nouveau (30 jours) est émis dans la même famille. */
    @Transactional(noRollbackFor = AuthentificationException.class)
    public LoginResponse rafraichir(MobileJetonRequest request) {
        JetonRafraichissement jeton = jetonRepository.findByJetonHache(JetonSecurise.hacher(request.getJetonRafraichissement()))
                .orElseThrow(() -> new AuthentificationException("Session expirée : reconnectez-vous avec votre PIN."));
        if (Boolean.TRUE.equals(jeton.getRevoque())) {
            // Réutilisation d'un jeton déjà échangé : vol présumé, toute la session est fermée
            jetonRepository.revoquerFamille(jeton.getFamille());
            throw new AuthentificationException("Session expirée : reconnectez-vous avec votre PIN.");
        }
        Utilisateur utilisateur = jeton.getUtilisateur();
        if (jeton.getExpiration().isBefore(LocalDateTime.now()) || !Boolean.TRUE.equals(utilisateur.getActif())) {
            jetonRepository.revoquerFamille(jeton.getFamille());
            throw new AuthentificationException("Session expirée : reconnectez-vous avec votre PIN.");
        }
        jeton.setRevoque(true);
        jetonRepository.save(jeton);
        return session(utilisateur, true, jeton.getFamille());
    }

    @Transactional
    public void deconnecter(MobileJetonRequest request) {
        jetonRepository.findByJetonHache(JetonSecurise.hacher(request.getJetonRafraichissement()))
                .ifPresent(j -> jetonRepository.revoquerFamille(j.getFamille()));
    }

    // ==================== OUTILS ====================

    /** PIN trop facile à deviner : 4 chiffres identiques, suites croissantes ou décroissantes. */
    static String faiblessePin(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            return "Le PIN contient exactement 4 chiffres.";
        }
        boolean identiques = pin.chars().distinct().count() == 1;
        boolean croissant = true;
        boolean decroissant = true;
        for (int i = 1; i < 4; i++) {
            croissant &= pin.charAt(i) - pin.charAt(i - 1) == 1;
            decroissant &= pin.charAt(i - 1) - pin.charAt(i) == 1;
        }
        return identiques || croissant || decroissant
                ? "Ce PIN est trop facile à deviner (chiffres identiques ou qui se suivent). Choisissez-en un autre."
                : null;
    }

    static String masquerEmail(String email) {
        int arobase = email.indexOf('@');
        if (arobase <= 1) {
            return "*" + email.substring(Math.max(arobase, 0));
        }
        return email.charAt(0) + "*".repeat(Math.min(arobase - 1, 6)) + email.substring(arobase);
    }

    private Utilisateur compteMobile(String telephone) {
        List<Utilisateur> comptes = utilisateurRepository.findByTelephoneAndRoleIn(telephone, ROLES_MOBILES);
        if (comptes.size() > 1) {
            throw new IllegalArgumentException("Plusieurs comptes utilisent ce numéro : connectez-vous avec votre e-mail "
                    + "et contactez votre structure de santé.");
        }
        return comptes.isEmpty() ? null : comptes.get(0);
    }

    /** Limite les renvois, invalide les codes précédents et prépare un nouveau code (non encore enregistré). */
    private CodeVerification nouveauCode(String telephone) {
        LocalDateTime maintenant = LocalDateTime.now();
        codeRepository.findFirstByTelephoneOrderByDateEnvoiDesc(telephone).ifPresent(dernier -> {
            long ecoule = Duration.between(dernier.getDateEnvoi().atZone(ZoneId.systemDefault()),
                    maintenant.atZone(ZoneId.systemDefault())).getSeconds();
            if (ecoule < delaiRenvoiSecondes) {
                throw new TropDeDemandesException("Patientez " + (delaiRenvoiSecondes - ecoule)
                        + " secondes avant de demander un nouveau code.");
            }
        });
        if (codeRepository.countByTelephoneAndDateEnvoiAfter(telephone, maintenant.minusHours(1)) >= codesMaxParHeure) {
            throw new TropDeDemandesException("Trop de codes demandés pour ce numéro. Réessayez dans une heure.");
        }
        codeRepository.findByTelephoneAndUtiliseFalse(telephone).forEach(ancien -> {
            ancien.setUtilise(true);
            codeRepository.save(ancien);
        });
        CodeVerification code = new CodeVerification();
        code.setTelephone(telephone);
        code.setDateEnvoi(maintenant);
        code.setExpiration(maintenant.plusMinutes(codeDureeMinutes));
        return code;
    }

    private void envoyerCode(CodeVerification code, String email, String prenom) {
        String clair = String.format("%06d", ALEATOIRE.nextInt(1_000_000));
        code.setCodeHache(passwordEncoder.encode(clair));
        codeRepository.save(code);
        emailService.envoyer(email, "YAY360 — Votre code de vérification : " + clair,
                "Bonjour " + prenom + ",\n\n"
                        + "Votre code de vérification YAY360 est : " + clair + "\n\n"
                        + "Il est valable " + codeDureeMinutes + " minutes et ne peut servir qu'une fois. "
                        + "Ne le communiquez à personne : l'équipe YAY360 ne vous le demandera jamais.\n\n"
                        + "Si vous n'avez rien demandé, ignorez ce message.",
                clair);
    }

    private Utilisateur creerCompte(CodeVerification code) {
        if (utilisateurRepository.existsByEmailIgnoreCase(code.getInscEmail()) || compteMobile(code.getTelephone()) != null) {
            throw new EmailDejaUtiliseException("Un compte existe déjà avec ce numéro ou cet email");
        }
        Utilisateur femme = new Utilisateur();
        femme.setNom(code.getInscNom());
        femme.setPrenom(code.getInscPrenom());
        femme.setEmail(code.getInscEmail());
        femme.setTelephone(code.getTelephone());
        femme.setDateNaissance(code.getInscDateNaissance());
        femme.setRole(RoleUtilisateur.FEMME_ENCEINTE);
        femme.setActif(true);
        // Mot de passe aléatoire inconnu : la connexion se fait par PIN ; « mot de passe oublié » reste possible.
        femme.setMotDePasse(passwordEncoder.encode(JetonSecurise.generer()));
        return utilisateurRepository.save(femme);
    }

    private void enregistrerEchecPin(Utilisateur utilisateur, LocalDateTime maintenant) {
        int echecs = valeur(utilisateur.getPinEchecs()) + 1;
        utilisateur.setPinEchecs(echecs);
        if (echecs >= pinEchecsMax) {
            int verrouillages = valeur(utilisateur.getPinVerrouillages()) + 1;
            utilisateur.setPinEchecs(0);
            utilisateur.setPinVerrouillages(verrouillages);
            if (verrouillages >= pinVerrouillagesAvantCode) {
                // Trop de verrouillages : le PIN est effacé, un nouveau code par e-mail est exigé
                utilisateur.setPin(null);
                utilisateur.setPinVerrouilleJusqua(null);
                jetonRepository.revoquerTousPourUtilisateur(utilisateur.getId());
            } else {
                // Verrouillage progressif : 15, puis 30 minutes…
                long minutes = pinVerrouillageMinutes * (1L << (verrouillages - 1));
                utilisateur.setPinVerrouilleJusqua(maintenant.plusMinutes(minutes));
            }
        }
        utilisateurRepository.save(utilisateur);
    }

    private LoginResponse session(Utilisateur utilisateur, boolean resterConnecte, String famille) {
        String acces = jwtUtil.genererToken(utilisateur.getId(), utilisateur.getEmail(), utilisateur.getRole(),
                utilisateur.getVersionJeton());
        LoginResponse reponse = utilisateurMapper.versLoginResponse(utilisateur, acces);
        if (resterConnecte) {
            String clair = JetonSecurise.generer();
            JetonRafraichissement jeton = new JetonRafraichissement();
            jeton.setUtilisateur(utilisateur);
            jeton.setJetonHache(JetonSecurise.hacher(clair));
            jeton.setFamille(famille != null ? famille : JetonSecurise.generer());
            jeton.setDateCreation(LocalDateTime.now());
            jeton.setExpiration(LocalDateTime.now().plusDays(sessionJours));
            jetonRepository.save(jeton);
            reponse.setJetonRafraichissement(clair);
        }
        return reponse;
    }

    private String empreinteFactice() {
        if (empreinteFactice == null) {
            empreinteFactice = passwordEncoder.encode("0000-factice");
        }
        return empreinteFactice;
    }

    private static int valeur(Integer entier) {
        return entier == null ? 0 : entier;
    }
}
