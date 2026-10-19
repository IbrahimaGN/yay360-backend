package sn.yay360.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarerSymptomeRequest {

    @NotBlank(message = "Le type de symptôme est obligatoire")
    private String typeSymptome;

    private Long grossesseId;

    private String description;
}
