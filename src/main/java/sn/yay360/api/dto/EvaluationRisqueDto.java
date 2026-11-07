package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Facteurs de risque détectés par le moteur de règles. Aide à la décision uniquement :
 * seul le médecin classe une grossesse à risque.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRisqueDto {
    private Long grossesseId;
    private boolean grossesseARisque;
    private List<String> facteurs;
    private boolean suggestionDeclarerARisque;
    private String avertissement = "Suggestion automatique : la décision appartient au médecin.";
}
