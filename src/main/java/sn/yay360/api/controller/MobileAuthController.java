package sn.yay360.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.yay360.api.dto.*;
import sn.yay360.api.service.MobileAuthService;

@RestController
@RequestMapping("/auth/mobile")
@RequiredArgsConstructor
@Tag(name = "Connexion mobile", description = "Téléphone, code reçu par e-mail, PIN à 4 chiffres et session longue")
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    @PostMapping("/demarrer")
    @Operation(summary = "Étape 1 : numéro de téléphone (code envoyé si le compte existe, sinon inscription)")
    public MobileDemarrerResponse demarrer(@Valid @RequestBody MobileDemarrerRequest request) {
        return mobileAuthService.demarrer(request);
    }

    @PostMapping("/pin-oublie")
    @Operation(summary = "PIN oublié : envoie un code de vérification à l'e-mail du compte")
    public MobileDemarrerResponse pinOublie(@Valid @RequestBody MobileDemarrerRequest request) {
        return mobileAuthService.pinOublie(request);
    }

    @PostMapping("/inscription")
    @Operation(summary = "Inscription d'une femme enceinte : envoie le code de vérification par e-mail")
    public MobileDemarrerResponse inscrire(@Valid @RequestBody MobileInscriptionRequest request) {
        return mobileAuthService.inscrire(request);
    }

    @PostMapping("/verifier-code")
    @Operation(summary = "Vérifie le code à 6 chiffres et renvoie un jeton pour définir le PIN")
    public MobileVerifierCodeResponse verifierCode(@Valid @RequestBody MobileVerifierCodeRequest request) {
        return mobileAuthService.verifierCode(request);
    }

    @PostMapping("/pin")
    @Operation(summary = "Définit (ou redéfinit) le PIN à 4 chiffres et ouvre la session")
    public LoginResponse definirPin(@Valid @RequestBody MobileDefinirPinRequest request) {
        return mobileAuthService.definirPin(request);
    }

    @PostMapping("/connexion")
    @Operation(summary = "Connexion par téléphone et PIN")
    public LoginResponse connecter(@Valid @RequestBody MobileConnexionPinRequest request) {
        return mobileAuthService.connecter(request);
    }

    @PostMapping("/rafraichir")
    @Operation(summary = "Rester connecté : échange le jeton de session contre un nouveau (rotation)")
    public LoginResponse rafraichir(@Valid @RequestBody MobileJetonRequest request) {
        return mobileAuthService.rafraichir(request);
    }

    @PostMapping("/deconnexion")
    @Operation(summary = "Ferme la session longue (révoque le jeton et sa famille)")
    public ResponseEntity<MessageResponse> deconnecter(@Valid @RequestBody MobileJetonRequest request) {
        mobileAuthService.deconnecter(request);
        return ResponseEntity.ok(new MessageResponse("Déconnexion réussie"));
    }
}
