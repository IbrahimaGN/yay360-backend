package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.yay360.api.helper.MoteurReglesCliniques;

import java.util.List;

/** Plan des 8 contacts prénatals recommandés par l'OMS, rapproché des consultations réalisées. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanCpnDto {
    private Long grossesseId;
    private int semaineActuelle;
    private List<MoteurReglesCliniques.ContactCpn> contacts;
    private long nombreRealises;
    private long nombreEnRetard;
    private String reference = "OMS 2016 : 8 contacts prénatals (12, 20, 26, 30, 34, 36, 38 et 40 SA)";
}
