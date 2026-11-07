package sn.yay360.api.service.impl;

import sn.yay360.api.dto.CreationProfessionnelResponse;
import sn.yay360.api.dto.CreerProfessionnelRequest;
import sn.yay360.api.dto.UpdateProfileRequest;
import sn.yay360.api.dto.UtilisateurDto;
import sn.yay360.api.helper.UtilisateurHelper;
import sn.yay360.api.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurHelper utilisateurHelper;

    @Override
    public UtilisateurDto obtenirProfil(String email) {
        return utilisateurHelper.obtenirProfil(email);
    }

    @Override
    public CreationProfessionnelResponse creerProfessionnel(CreerProfessionnelRequest request) {
        return utilisateurHelper.creerProfessionnel(request);
    }

    @Override
    public UtilisateurDto mettreAJourProfil(String email, UpdateProfileRequest request) {
        return utilisateurHelper.mettreAJourProfil(email, request);
    }

    @Override
    public List<UtilisateurDto> listerTous() {
        return utilisateurHelper.listerTous();
    }

    @Override
    public List<UtilisateurDto> listerParRole(String role) {
        return utilisateurHelper.listerParRole(role);
    }

    @Override
    public UtilisateurDto obtenirParId(Long id) {
        return utilisateurHelper.obtenirParId(id);
    }

    @Override
    public UtilisateurDto desactiverCompte(Long id, Long idAuteur) {
        return utilisateurHelper.desactiverCompte(id, idAuteur);
    }

    @Override
    public UtilisateurDto activerCompte(Long id) {
        return utilisateurHelper.activerCompte(id);
    }

    @Override
    public void supprimerCompte(Long id, Long idAuteur) {
        utilisateurHelper.supprimerCompte(id, idAuteur);
    }
}
