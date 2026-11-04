package sn.yay360.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Rattachement d'une grossesse à un établissement de suivi. */
@Getter
@Setter
public class ChoisirEtablissementRequest {

    @NotNull(message = "L'établissement est obligatoire")
    private Long etablissementId;
}
