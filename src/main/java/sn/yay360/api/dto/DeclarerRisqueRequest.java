package sn.yay360.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarerRisqueRequest {

    @NotBlank(message = "Le motif est obligatoire")
    private String motif;
}
