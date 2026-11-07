package sn.yay360.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Rattachement d'une grossesse à l'établissement d'un soignant. Le téléphone de la patiente sert
 * de preuve de présence : un soignant ne peut pas s'approprier un dossier en devinant un identifiant.
 */
@Getter
@Setter
public class RattacherGrossesseRequest {

    @NotNull(message = "L'établissement est obligatoire")
    private Long etablissementId;

    @NotBlank(message = "Le téléphone de la patiente est obligatoire")
    private String telephonePatiente;
}
