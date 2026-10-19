package sn.yay360.api.service;

import sn.yay360.api.dto.CalendrierGrossesseDto;

public interface CalendrierService {

    CalendrierGrossesseDto obtenirMonCalendrier(String email);

    CalendrierGrossesseDto obtenirCalendrierParId(Long grossesseId);
}
