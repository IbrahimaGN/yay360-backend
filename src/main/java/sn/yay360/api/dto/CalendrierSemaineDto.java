package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalendrierSemaineDto {

    private Integer semaine;
    private String trimestre;
    private String statutSemaine; // PASSEE, ACTUELLE, A_VENIR
    private String developpementBebe;
    private String changementsMere;
    private String examensRecommandes;
    private String objectifsSante;
}
