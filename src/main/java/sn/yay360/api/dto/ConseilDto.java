package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConseilDto {

    private Long id;
    private Integer semaineDebut;
    private Integer semaineFin;
    private String trimestre;
    private String categorie;
    private String titre;
    private String contenu;
    private Boolean actif;
    private String auteurNomComplet;
    private LocalDateTime dateCreation;
}
