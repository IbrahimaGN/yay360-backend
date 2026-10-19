package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActiviteEtablissementDto {

    private Long etablissementId;
    private String nom;
    private String ville;
    private Long nombreRendezVous;
}
