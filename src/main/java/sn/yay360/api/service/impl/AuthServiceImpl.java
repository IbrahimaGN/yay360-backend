package sn.yay360.api.service.impl;

import sn.yay360.api.dto.*;
import sn.yay360.api.helper.AuthHelper;
import sn.yay360.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthHelper authHelper;

    @Override
    public LoginResponse inscrire(RegisterRequest request) {
        return authHelper.inscrire(request);
    }

    @Override
    public LoginResponse accepterInvitationConjoint(AccepterInvitationConjointRequest request) {
        return authHelper.accepterInvitationConjoint(request);
    }

    @Override
    public LoginResponse connecter(LoginRequest request) {
        return authHelper.connecter(request);
    }

    @Override
    public void motDePasseOublie(ForgotPasswordRequest request) {
        authHelper.motDePasseOublie(request);
    }

    @Override
    public void reinitialiserMotDePasse(ResetPasswordRequest request) {
        authHelper.reinitialiserMotDePasse(request);
    }

    @Override
    public void changerMotDePasse(String email, ChangePasswordRequest request) {
        authHelper.changerMotDePasse(email, request);
    }
}
