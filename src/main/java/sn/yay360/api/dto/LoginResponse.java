package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    /** true si le mot de passe a été généré par un administrateur et doit être changé. */
    private boolean motDePasseTemporaire;
}
