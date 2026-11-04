package sn.yay360.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriseEnChargeRequest {

    /** Observations de résolution : obligatoires pour clôturer une alerte (traçabilité). */
    @jakarta.validation.constraints.NotBlank(message = "Les observations de résolution sont obligatoires")
    @jakarta.validation.constraints.Size(max = 2000, message = "Les observations ne doivent pas dépasser 2000 caractères")
    private String notesPriseEnCharge;
}
