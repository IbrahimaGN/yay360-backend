package sn.yay360.api.service;

import sn.yay360.api.dto.InvitationConjointRequest;

public interface DossierFamilialService {
    void inviterConjoint(String emailFemme, InvitationConjointRequest request);
}
