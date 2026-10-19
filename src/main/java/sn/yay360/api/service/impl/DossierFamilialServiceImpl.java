package sn.yay360.api.service.impl;

import sn.yay360.api.dto.InvitationConjointRequest;
import sn.yay360.api.helper.DossierFamilialHelper;
import sn.yay360.api.service.DossierFamilialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DossierFamilialServiceImpl implements DossierFamilialService {

    private final DossierFamilialHelper dossierFamilialHelper;

    @Override
    public void inviterConjoint(String emailFemme, InvitationConjointRequest request) {
        dossierFamilialHelper.inviterConjoint(emailFemme, request);
    }
}
