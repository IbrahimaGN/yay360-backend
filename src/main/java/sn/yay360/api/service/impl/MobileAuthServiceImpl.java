package sn.yay360.api.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.yay360.api.dto.*;
import sn.yay360.api.helper.MobileAuthHelper;
import sn.yay360.api.service.MobileAuthService;

@Service
@RequiredArgsConstructor
public class MobileAuthServiceImpl implements MobileAuthService {

    private final MobileAuthHelper mobileAuthHelper;

    @Override
    public MobileDemarrerResponse demarrer(MobileDemarrerRequest request) {
        return mobileAuthHelper.demarrer(request);
    }

    @Override
    public MobileDemarrerResponse pinOublie(MobileDemarrerRequest request) {
        return mobileAuthHelper.pinOublie(request);
    }

    @Override
    public MobileDemarrerResponse inscrire(MobileInscriptionRequest request) {
        return mobileAuthHelper.inscrire(request);
    }

    @Override
    public MobileVerifierCodeResponse verifierCode(MobileVerifierCodeRequest request) {
        return mobileAuthHelper.verifierCode(request);
    }

    @Override
    public LoginResponse definirPin(MobileDefinirPinRequest request) {
        return mobileAuthHelper.definirPin(request);
    }

    @Override
    public LoginResponse connecter(MobileConnexionPinRequest request) {
        return mobileAuthHelper.connecter(request);
    }

    @Override
    public LoginResponse rafraichir(MobileJetonRequest request) {
        return mobileAuthHelper.rafraichir(request);
    }

    @Override
    public void deconnecter(MobileJetonRequest request) {
        mobileAuthHelper.deconnecter(request);
    }
}
