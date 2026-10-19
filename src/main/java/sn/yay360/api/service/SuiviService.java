package sn.yay360.api.service;

import sn.yay360.api.dto.SuiviHebdomadaireDto;

public interface SuiviService {

    SuiviHebdomadaireDto obtenirMonSuivi(String email);

    SuiviHebdomadaireDto obtenirSuiviParId(Long grossesseId);
}
