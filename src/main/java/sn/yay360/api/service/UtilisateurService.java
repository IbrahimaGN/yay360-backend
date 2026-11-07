package sn.yay360.api.service;

import sn.yay360.api.dto.CreationProfessionnelResponse;
import sn.yay360.api.dto.CreerProfessionnelRequest;
import sn.yay360.api.dto.UpdateProfileRequest;
import sn.yay360.api.dto.UtilisateurDto;

import java.util.List;

public interface UtilisateurService {

    UtilisateurDto obtenirProfil(String email);

    CreationProfessionnelResponse creerProfessionnel(CreerProfessionnelRequest request);

    UtilisateurDto mettreAJourProfil(String email, UpdateProfileRequest request);

    List<UtilisateurDto> listerTous();

    List<UtilisateurDto> listerParRole(String role);

    UtilisateurDto obtenirParId(Long id);

    UtilisateurDto desactiverCompte(Long id, Long idAuteur);

    UtilisateurDto activerCompte(Long id);

    void supprimerCompte(Long id, Long idAuteur);
}
