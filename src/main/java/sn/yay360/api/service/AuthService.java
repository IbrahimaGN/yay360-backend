package sn.yay360.api.service;

import sn.yay360.api.dto.*;

public interface AuthService {

    LoginResponse inscrire(RegisterRequest request);

    LoginResponse accepterInvitationConjoint(AccepterInvitationConjointRequest request);

    LoginResponse connecter(LoginRequest request);

    void motDePasseOublie(ForgotPasswordRequest request);

    void reinitialiserMotDePasse(ResetPasswordRequest request);

    void changerMotDePasse(String email, ChangePasswordRequest request);
}
