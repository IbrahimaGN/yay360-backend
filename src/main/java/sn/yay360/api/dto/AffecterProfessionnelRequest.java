package sn.yay360.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AffecterProfessionnelRequest {

    @NotNull(message = "L'id du professionnel est obligatoire")
    private Long professionnelId;
}
