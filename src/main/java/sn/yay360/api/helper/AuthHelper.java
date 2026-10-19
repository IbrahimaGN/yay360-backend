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
import sn.yay360.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Contient toute la logique métier liée à l'authentification :
 * inscription, connexion, mot de passe oublié, réinitialisation, changement.
 */
@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final UtilisateurRepository utilisateurRepository;
    private final InvitationConjointRepository invitationConjointRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UtilisateurMapper utilisateurMapper;

    @Value("${jwt.reset-token-expiration}")
    private long resetTokenExpirationMs;

    private static final List<String> ROLES_VALIDES = List.of(
            RoleUtilisateur.ADMIN, RoleUtilisateur.FEMME_ENCEINTE,
            RoleUtilisateur.SAGE_FEMME, RoleUtilisateur.MEDECIN
    );

    // Âge minimum plausible par rôle ; le compte conjoint est créé uniquement sur invitation.
    private static final Map<String, Integer> AGE_MINIMUM_PAR_ROLE = Map.of(
            RoleUtilisateur.FEMME_ENCEINTE, 13,
            RoleUtilisateur.CONJOINT, 18,
            RoleUtilisateur.MEDECIN, 21,
            RoleUtilisateur.SAGE_FEMME, 21,
            RoleUtilisateur.ADMIN, 18
    );

    private static final int AGE_MAXIMUM_PLAUSIBLE = 120;

    public LoginResponse inscrire(RegisterRequest request) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new EmailDejaUtiliseException("Un compte existe déjà avec cet email : " + request.getEmail());
        }

        if (!ROLES_VALIDES.contains(request.getRole())) {
            throw new IllegalArgumentException("Rôle invalide : " + request.getRole());
        }

        validerAge(request.getRole(), request.getDateNaissance());

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getPassword()));
        utilisateur.setTelephone(request.getTelephone());
        utilisateur.setDateNaissance(request.getDateNaissance());
        utilisateur.setRole(request.getRole());
        utilisateur.setActif(true);

        Utilisateur enregistre = utilisateurRepository.save(utilisateur);

        String token = jwtUtil.genererToken(enregistre.getId(), enregistre.getEmail(), enregistre.getRole());
        return utilisateurMapper.versLoginResponse(enregistre, token);
    }

    @Transactional
    public LoginResponse accepterInvitationConjoint(AccepterInvitationConjointRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        InvitationConjoint invitation = invitationConjointRepository
                .findByTokenAndAccepteeFalse(request.getToken())
                .orElseThrow(() -> new AuthentificationException("Invitation invalide ou déjà utilisée"));
        if (invitation.getDateExpiration().isBefore(LocalDateTime.now())
                || !invitation.getEmail().equalsIgnoreCase(email)) {
            throw new AuthentificationException("Invitation expirée ou adresse email incorrecte");
        }
        if (utilisateurRepository.existsByEmailIgnoreCase(email)
                || utilisateurRepository.findByFemmeAssocieeId(invitation.getFemme().getId()).isPresent()) {
            throw new EmailDejaUtiliseException("Un compte ou un conjoint est déjà associé à cette invitation");
        }
        validerAge(RoleUtilisateur.CONJOINT, request.getDateNaissance());

        Utilisateur conjoint = new Utilisateur();
        conjoint.setNom(request.getNom());
        conjoint.setPrenom(request.getPrenom());
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
        String token = jwtUtil.genererToken(enregistre.getId(), enregistre.getEmail(), enregistre.getRole());
        return utilisateurMapper.versLoginResponse(enregistre, token);
    }

    public LoginResponse connecter(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthentificationException("Email ou mot de passe incorrect"));

        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new AuthentificationException("Ce compte a été désactivé. Contactez un administrateur.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = jwtUtil.genererToken(utilisateur.getId(), utilisateur.getEmail(), utilisateur.getRole());
        return utilisateurMapper.versLoginResponse(utilisateur, token);
    }

    public void motDePasseOublie(ForgotPasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthentificationException("Aucun compte associé à cet email"));

        String token = genererTokenAleatoire();
        utilisateur.setResetToken(token);
        utilisateur.setResetTokenExpiration(LocalDateTime.now().plusNanos(resetTokenExpirationMs * 1_000_000));
        utilisateurRepository.save(utilisateur);

        // Simulation d'envoi d'email (comme l'OTP simulé en console dans wallet-service)
        System.out.println("=== [YAY360] Email de réinitialisation ===");
        System.out.println("Destinataire : " + utilisateur.getEmail());
        System.out.println("Token de réinitialisation : " + token);
        System.out.println("Valide " + (resetTokenExpirationMs / 60000) + " minutes");
        System.out.println("===========================================");
    }

    public void reinitialiserMotDePasse(ResetPasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new AuthentificationException("Token de réinitialisation invalide"));

        if (utilisateur.getResetTokenExpiration() == null
                || utilisateur.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new AuthentificationException("Le token de réinitialisation a expiré");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateur.setResetToken(null);
        utilisateur.setResetTokenExpiration(null);
        utilisateurRepository.save(utilisateur);
    }

    public void changerMotDePasse(String email, ChangePasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new AuthentificationException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(request.getAncienMotDePasse(), utilisateur.getMotDePasse())) {
            throw new AuthentificationException("L'ancien mot de passe est incorrect");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);
    }

    private String genererTokenAleatoire() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Valide l'âge selon le rôle : une femme enceinte doit avoir au moins 13 ans,
     * un professionnel de santé (médecin/sage-femme) au moins 21 ans (âge minimum
     * plausible après des études supérieures), un administrateur au moins 18 ans.
     */
    private void validerAge(String role, LocalDate dateNaissance) {
        int age = Period.between(dateNaissance, LocalDate.now()).getYears();

        if (age > AGE_MAXIMUM_PLAUSIBLE) {
            throw new IllegalArgumentException("La date de naissance semble invalide");
        }

        Integer ageMinimum = AGE_MINIMUM_PAR_ROLE.get(role);
        if (ageMinimum != null && age < ageMinimum) {
            throw new IllegalArgumentException(
                    "L'âge minimum requis pour le rôle " + role + " est de " + ageMinimum + " ans");
        }
    }
}
