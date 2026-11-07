package sn.yay360.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarerSymptomeRequest {

    @NotBlank(message = "Le type de symptôme est obligatoire")
    @jakarta.validation.constraints.Pattern(
            regexp = "SAIGNEMENTS|DOULEURS_ABDOMINALES|PERTE_LIQUIDE|DIMINUTION_MOUVEMENTS_BEBE|FIEVRE_ELEVEE"
                    + "|MAUX_DE_TETE_INTENSES|OEDEMES_SOUDAINS|VOMISSEMENTS_SEVERES|TROUBLES_VISUELS|CONVULSIONS|AUTRE",
            message = "Type de symptôme inconnu")
    private String typeSymptome;

    private Long grossesseId;

    @jakarta.validation.constraints.Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;
}
