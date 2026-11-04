package sn.yay360.api.service;

import sn.yay360.api.dto.*;

import java.util.List;

public interface SuiviService {

    SuiviHebdomadaireDto obtenirMonSuivi(String email);

    SuiviHebdomadaireDto obtenirSuiviParId(String email, Long grossesseId);
}
