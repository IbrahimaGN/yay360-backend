package sn.yay360.api.service;

import sn.yay360.api.dto.*;

/** Connexion mobile : téléphone, code reçu par e-mail, PIN à 4 chiffres et session longue. */
public interface MobileAuthService {

    MobileDemarrerResponse demarrer(MobileDemarrerRequest request);

    MobileDemarrerResponse pinOublie(MobileDemarrerRequest request);

    MobileDemarrerResponse inscrire(MobileInscriptionRequest request);

    MobileVerifierCodeResponse verifierCode(MobileVerifierCodeRequest request);

    LoginResponse definirPin(MobileDefinirPinRequest request);

    LoginResponse connecter(MobileConnexionPinRequest request);

    LoginResponse rafraichir(MobileJetonRequest request);

    void deconnecter(MobileJetonRequest request);
}
