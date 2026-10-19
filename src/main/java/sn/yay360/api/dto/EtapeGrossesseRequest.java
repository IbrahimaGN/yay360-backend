package sn.yay360.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EtapeGrossesseRequest {

    @NotNull(message = "Le numéro de semaine est obligatoire")
    @Min(value = 1, message = "La semaine doit être comprise entre 1 et 40")
    @Max(value = 40, message = "La semaine doit être comprise entre 1 et 40")
    private Integer semaine;

    private String developpementBebe;
    private String changementsMere;
    private String objectifsSante;
    private String conseilsAlimentation;
    private String conseilsHydratation;
    private String exercicesPhysiques;
    private String conseilsSommeil;
    private String precautions;
    private String examensRecommandes;
    private String signesAlerte;
}
