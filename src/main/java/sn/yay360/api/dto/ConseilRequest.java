package sn.yay360.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConseilRequest {

    @NotNull(message = "La semaine de début est obligatoire")
    @Min(1) @Max(40)
    private Integer semaineDebut;

    @NotNull(message = "La semaine de fin est obligatoire")
    @Min(1) @Max(40)
    private Integer semaineFin;

    @NotBlank(message = "La catégorie est obligatoire")
    private String categorie;

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    @NotBlank(message = "Le contenu est obligatoire")
    private String contenu;
}
