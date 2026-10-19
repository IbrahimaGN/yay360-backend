package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapeGrossesseDto {

    private Long id;
    private Integer semaine;
    private String trimestre;
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
