package sn.yay360.api.mapper;

import sn.yay360.api.dto.LoginResponse;
import sn.yay360.api.dto.UtilisateurDto;
import sn.yay360.api.model.Utilisateur;
import org.springframework.stereotype.Component;

@Component
public class UtilisateurMapper {

    public UtilisateurDto versDto(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        return new UtilisateurDto(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getTelephone(),
                utilisateur.getDateNaissance(),
                utilisateur.getRole(),
                utilisateur.getActif(),
                utilisateur.getDateCreation()
        );
    }

    public LoginResponse versLoginResponse(Utilisateur utilisateur, String token) {
        return new LoginResponse(
                token,
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getRole(),
                Boolean.TRUE.equals(utilisateur.getMotDePasseTemporaire()),
                null
        );
    }
}
