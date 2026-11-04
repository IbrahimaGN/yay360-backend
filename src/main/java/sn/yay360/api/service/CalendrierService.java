package sn.yay360.api.service;

import sn.yay360.api.dto.*;

import java.util.List;

public interface CalendrierService {

    CalendrierGrossesseDto obtenirMonCalendrier(String email);

    CalendrierGrossesseDto obtenirCalendrierParId(String email, Long grossesseId);
}
