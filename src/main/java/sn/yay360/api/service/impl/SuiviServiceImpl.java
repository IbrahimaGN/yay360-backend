package sn.yay360.api.service.impl;

import sn.yay360.api.dto.*;
import sn.yay360.api.helper.SuiviHelper;
import sn.yay360.api.service.SuiviService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuiviServiceImpl implements SuiviService {

    private final SuiviHelper suiviHelper;

    @Override
    public SuiviHebdomadaireDto obtenirMonSuivi(String email) {
        return suiviHelper.obtenirMonSuivi(email);
    }

    @Override
    public SuiviHebdomadaireDto obtenirSuiviParId(String email, Long grossesseId) {
        return suiviHelper.obtenirSuiviParId(email, grossesseId);
    }
}
