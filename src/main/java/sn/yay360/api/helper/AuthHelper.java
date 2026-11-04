package sn.yay360.api.helper;

import sn.yay360.api.dto.*;
import sn.yay360.api.exception.AuthentificationException;
import sn.yay360.api.exception.EmailDejaUtiliseException;
import sn.yay360.api.mapper.UtilisateurMapper;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.InvitationConjoint;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.InvitationConjointRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import sn.yay360.api.security.JetonSecurise;
import sn.yay360.api.security.JwtUtil;
import sn.yay360.api.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Contient toute la logique métier liée à l'authentification :
 * inscription, connexion, mot de passe oublié, réinitialisation, changement.
 */
@Component
@RequiredArgsConstructor
public class AuthHelper {

    private static final Logger log = LoggerFactory.getLogger(AuthHelper.class);

    /**
     * Message unique pour tout échec de connexion (email inconnu, mot de passe faux, compte verrouillé) :
     * il ne révèle ni l'existence d'un compte ni son verrouillage.
     */
    static final String IDENTIFIANTS_INCORRECTS =
            "Email ou mot de passe incorrect. Après plusieurs échecs, réessayez dans quelques minutes.";

    private final UtilisateurRepository utilisateurRepository;
    private final InvitationConjointRepository invitationConjointRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UtilisateurMapper utilisateurMapper;
    private final EmailService emailService;

    @Value("${jwt.reset-token-expiration:3600000}")
    private long resetTokenExpirationMs = 3_600_000L;

    @Value("${yay360.securite.tentatives-max:5}")
    private int tentativesMax = 5;

    @Value("${yay360.securite.duree-verrouillage-minutes:15}")
    private long dureeVerrouillageMinutes = 15;

    @Value("${yay360.frontend-url:http://localhost:4200}")
    private String frontendUrl = "http://localhost:4200";

    private volatile String empreinteFactice;

    private String empreinteFactice() {
        if (empreinteFactice == null) {
            empreinteFactice = passwordEncoder.encode(JetonSecurise.generer());
        }
        return empreinteFactice;
    }

    public static String normaliserEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Inscription publique : réservée aux femmes enceintes. Les comptes professionnels
     * (médecin, sage-femme) et administrateurs sont créés par un administrateur.
     */
    @Transactional
    public LoginResponse inscrire(RegisterRequest request) {
        if (request.getRole() != null && !RoleUtilisateur.FEMME_ENCEINTE.equals(request.getRole())) {
            throw new IllegalArgumentException(
                    "L'inscription en ligne est réservée aux femmes enceintes. "
                            + "Les comptes professionnels sont créés par un administrateur.");
        }
        String email = normaliserEmail(request.getEmail());
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailDejaUtiliseException("Un compte existe déjà avec cet email");
        }

        RegleAgeHelper.valider(RoleUtilisateur.FEMME_ENCEINTE, request.getDateNaissance());

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.getNom().trim());
        utilisateur.setPrenom(request.getPrenom().trim());
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getPassword()));
        utilisateur.setTelephone(request.getTelephone());
        utilisateur.setDateNaissance(request.getDateNaissance());
        utilisateur.setRole(RoleUtilisateur.FEMME_ENCEINTE);
        utilisateur.setActif(true);

        Utilisateur enregistre = utilisateurRepository.save(utilisateur);
        return reponseConnexion(enregistre);
    }

    @Transactional
    public LoginResponse accepterInvitationConjoint(AccepterInvitationConjointRequest request) {
        String email = normaliserEmail(request.getEmail());
        InvitationConjoint invitation = invitationConjointRepository
                .findByTokenAndAccepteeFalse(JetonSecurise.hacher(request.getToken().trim()))
                .orElseThrow(() -> new AuthentificationException("Invitation invalide ou déjà utilisée"));
        if (invitation.getDateExpiration().isBefore(LocalDateTime.now())
                || !invitation.getEmail().equalsIgnoreCase(email)) {
            throw new AuthentificationException("Invitation expirée ou adresse email incorrecte");
        }
        if (utilisateurRepository.existsByEmailIgnoreCase(email)
                || utilisateurRepository.findByFemmeAssocieeId(invitation.getFemme().getId()).isPresent()) {
            throw new EmailDejaUtiliseException("Un compte ou un conjoint est déjà associé à cette invitation");
        }
        RegleAgeHelper.valider(RoleUtilisateur.CONJOINT, request.getDateNaissance());

        Utilisateur conjoint = new Utilisateur();
        conjoint.setNom(request.getNom().trim());
        conjoint.setPrenom(request.getPrenom().trim());
        conjoint.setEmail(email);
        conjoint.setMotDePasse(passwordEncoder.encode(request.getPassword()));
        conjoint.setTelephone(request.getTelephone());
        conjoint.setDateNaissance(request.getDateNaissance());
        conjoint.setRole(RoleUtilisateur.CONJOINT);
        conjoint.setFemmeAssociee(invitation.getFemme());
        conjoint.setActif(true);
        Utilisateur enregistre = utilisateurRepository.save(conjoint);

        invitation.setAcceptee(true);
        invitationConjointRepository.save(invitation);
        return reponseConnexion(enregistre);
    }

    /**
     * Connexion. Le mot de passe est vérifié AVANT l'état du compte pour ne pas révéler
     * l'existence d'un compte. Après {@code tentativesMax} échecs, le compte est verrouillé
     * temporairement (protection contre la force brute).
     */
    @Transactional(noRollbackFor = AuthentificationException.class)
    public LoginResponse connecter(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findPourConnexion(normaliserEmail(request.getEmail()))
                .orElse(null);

        if (utilisateur == null) {
            // Vérification factice : temps de réponse comparable à celui d'un compte existant
            passwordEncoder.matches(request.getPassword(), empreinteFactice());
            throw new AuthentificationException(IDENTIFIANTS_INCORRECTS);
        }

        LocalDateTime maintenant = LocalDateTime.now();
        if (utilisateur.getVerrouilleJusqua() != null && utilisateur.getVerrouilleJusqua().isAfter(maintenant)) {
            passwordEncoder.matches(request.getPassword(), empreinteFactice()); // temps de réponse constant
            throw new AuthentificationException(IDENTIFIANTS_INCORRECTS);
        }

        if (!passwordEncoder.matches(request.getPassword(), utilisateur.getMotDePasse())) {
            enregistrerEchec(utilisateur, maintenant);
            throw new AuthentificationException(IDENTIFIANTS_INCORRECTS);
        }

        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new AuthentificationException("Ce compte a été désactivé. Contactez un administrateur.");
        }

        if (valeur(utilisateur.getTentativesConnexionEchouees()) > 0 || utilisateur.getVerrouilleJusqua() != null) {
            utilisateur.setTentativesConnexionEchouees(0);
            utilisateur.setVerrouilleJusqua(null);
            utilisateurRepository.save(utilisateur);
        }
        return reponseConnexion(utilisateur);
    }

    /**
     * Mot de passe oublié : la réponse est identique que l'email existe ou non
     * (pas d'énumération des comptes). Le lien est envoyé par email.
     */
    @Transactional
    public void motDePasseOublie(ForgotPasswordRequest request) {
        utilisateurRepository.findByEmailIgnoreCase(normaliserEmail(request.getEmail()))
                .filter(u -> Boolean.TRUE.equals(u.getActif()))
                .ifPresent(utilisateur -> {
                    String jeton = JetonSecurise.generer();
                    utilisateur.setResetToken(JetonSecurise.hacher(jeton));
                    utilisateur.setResetTokenExpiration(
                            LocalDateTime.now().plus(resetTokenExpirationMs, ChronoUnit.MILLIS));
                    utilisateurRepository.save(utilisateur);

                    String lien = frontendUrl + "/reinitialiser-mot-de-passe?token=" + jeton;
                    long minutes = resetTokenExpirationMs / 60_000;
                    emailService.envoyer(utilisateur.getEmail(), "YAY360 — Réinitialisation de votre mot de passe",
                            "Bonjour " + utilisateur.getPrenom() + ",\n\n"
                                    + "Pour choisir un nouveau mot de passe, ouvrez ce lien (valable " + minutes
                                    + " minutes, utilisable une seule fois) :\n" + lien + "\n\n"
                                    + "Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.",
                            lien);
                    log.info("[YAY360] Demande de réinitialisation de mot de passe (utilisateur id={})",
                            utilisateur.getId());
                });
    }

    @Transactional
    public void reinitialiserMotDePasse(ResetPasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByResetToken(JetonSecurise.hacher(request.getToken().trim()))
                .orElseThrow(() -> new AuthentificationException("Lien de réinitialisation invalide ou déjà utilisé"));

        if (utilisateur.getResetTokenExpiration() == null
                || utilisateur.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            utilisateur.setResetToken(null);
            utilisateur.setResetTokenExpiration(null);
            utilisateurRepository.save(utilisateur);
            throw new AuthentificationException("Le lien de réinitialisation a expiré");
        }

        definirNouveauMotDePasse(utilisateur, request.getNouveauMotDePasse());
        utilisateur.setResetToken(null);
        utilisateur.setResetTokenExpiration(null);
        utilisateur.setTentativesConnexionEchouees(0);
        utilisateur.setVerrouilleJusqua(null);
        utilisateurRepository.save(utilisateur);
    }

    /**
     * Changement du mot de passe. Renvoie un nouveau jeton : les jetons émis avant
     * le changement sont invalidés (déconnexion des autres appareils).
     */
    @Transactional
    public LoginResponse changerMotDePasse(String email, ChangePasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthentificationException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(request.getAncienMotDePasse(), utilisateur.getMotDePasse())) {
            throw new AuthentificationException("L'ancien mot de passe est incorrect");
        }
        if (request.getAncienMotDePasse().equals(request.getNouveauMotDePasse())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        definirNouveauMotDePasse(utilisateur, request.getNouveauMotDePasse());
        Utilisateur enregistre = utilisateurRepository.save(utilisateur);
        return reponseConnexion(enregistre);
    }

    private void definirNouveauMotDePasse(Utilisateur utilisateur, String motDePasse) {
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateur.setMotDePasseTemporaire(false);
        utilisateur.setDateChangementMotDePasse(LocalDateTime.now());
        // Révoque immédiatement tous les jetons émis avant ce changement
        utilisateur.setVersionJeton((utilisateur.getVersionJeton() == null ? 0 : utilisateur.getVersionJeton()) + 1);
    }

    private void enregistrerEchec(Utilisateur utilisateur, LocalDateTime maintenant) {
        int echecs = valeur(utilisateur.getTentativesConnexionEchouees()) + 1;
        if (echecs >= tentativesMax) {
            utilisateur.setVerrouilleJusqua(maintenant.plusMinutes(dureeVerrouillageMinutes));
            utilisateur.setTentativesConnexionEchouees(0);
            log.warn("[YAY360] Compte id={} verrouillé {} min après {} échecs de connexion",
                    utilisateur.getId(), dureeVerrouillageMinutes, echecs);
        } else {
            utilisateur.setTentativesConnexionEchouees(echecs);
        }
        utilisateurRepository.save(utilisateur);
    }

    private LoginResponse reponseConnexion(Utilisateur utilisateur) {
        String token = jwtUtil.genererToken(utilisateur.getId(), utilisateur.getEmail(), utilisateur.getRole(),
                utilisateur.getVersionJeton());
        return utilisateurMapper.versLoginResponse(utilisateur, token);
    }

    private static int valeur(Integer entier) {
        return entier == null ? 0 : entier;
    }
}
