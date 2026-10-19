package sn.yay360.api.service;

import sn.yay360.api.dto.*;

import java.util.List;

public interface RendezVousService {

    RendezVousDto prendre(String emailFemme, PrendreRendezVousRequest request);

    List<RendezVousDto> mesRendezVous(String emailFemme);

    List<RendezVousDto> mesRendezVousAVenir(String emailFemme);

    List<RendezVousDto> rendezVousDuProfessionnel(String emailProfessionnel);

    List<RendezVousDto> listerParStatut(String statut);

    List<RendezVousDto> listerParGrossesse(Long grossesseId);

    RendezVousDto obtenirParId(String email, Long id);

    RendezVousDto modifier(String email, Long id, ModifierRendezVousRequest request);

    RendezVousDto annuler(String email, Long id, AnnulerRendezVousRequest request);

    RendezVousDto changerStatut(Long id, ChangerStatutRendezVousRequest request);
}
