package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtablissementDto {

    private Long id;
    private String nom;
    private String type;
    private String adresse;
    private String ville;
    private String telephone;
    private String email;
    private Double latitude;
    private Double longitude;
    private Boolean actif;
    private List<String> professionnels;
}
