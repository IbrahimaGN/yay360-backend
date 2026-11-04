package sn.yay360.api.service;

import sn.yay360.api.dto.InvitationConjointRequest;
import sn.yay360.api.dto.InvitationConjointResponse;

public interface DossierFamilialService {
    InvitationConjointResponse inviterConjoint(String emailFemme, InvitationConjointRequest request);
}
