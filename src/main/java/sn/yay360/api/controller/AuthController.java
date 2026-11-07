package sn.yay360.api.controller;

import sn.yay360.api.dto.*;
import sn.yay360.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, connexion, invitations du conjoint et mot de passe")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un nouvel utilisateur")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.inscrire(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/accept-invitation-conjoint")
    @Operation(summary = "Créer le compte conjoint depuis une invitation")
    public ResponseEntity<LoginResponse> accepterInvitationConjoint(
            @Valid @RequestBody AccepterInvitationConjointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.accepterInvitationConjoint(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion d'un utilisateur")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.connecter(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion (côté client : suppression du token)")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(new MessageResponse("Déconnexion réussie"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Demande de réinitialisation du mot de passe")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.motDePasseOublie(request);
        return ResponseEntity.ok(new MessageResponse(
                "Si un compte actif est associé à cet email, un lien de réinitialisation vient d'être envoyé."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialisation du mot de passe avec un token")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.reinitialiserMotDePasse(request);
        return ResponseEntity.ok(new MessageResponse("Mot de passe réinitialisé avec succès"));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Changement du mot de passe (utilisateur connecté)",
            description = "Renvoie un nouveau jeton : les jetons émis avant le changement sont invalidés.")
    public ResponseEntity<ChangementMotDePasseResponse> changePassword(Authentication authentication,
                                                                       @Valid @RequestBody ChangePasswordRequest request) {
        LoginResponse connexion = authService.changerMotDePasse(authentication.getName(), request);
        return ResponseEntity.ok(new ChangementMotDePasseResponse("Mot de passe modifié avec succès",
                connexion.getToken()));
    }
}
