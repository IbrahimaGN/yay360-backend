package sn.yay360.api.service;

import sn.yay360.api.dto.CreationProfessionnelResponse;
import sn.yay360.api.dto.CreerProfessionnelRequest;
import sn.yay360.api.dto.UpdateProfileRequest;
import sn.yay360.api.dto.UtilisateurDto;

import java.util.List;

public interface UtilisateurService {

    UtilisateurDto obtenirProfil(String email);

    CreationProfessionnelResponse creerProfessionnel(String emailAuteur, CreerProfessionnelRequest request);

    UtilisateurDto mettreAJourProfil(String email, UpdateProfileRequest request);

    List<UtilisateurDto> listerTous();

    List<UtilisateurDto> listerParRole(String role);

    List<UtilisateurDto> listerSoignantsDeMonEtablissement(String emailAuteur);

    UtilisateurDto obtenirParId(Long id);

    UtilisateurDto desactiverCompte(Long id, String emailAuteur);

    UtilisateurDto activerCompte(Long id, String emailAuteur);

    void supprimerCompte(Long id, Long idAuteur);
}
