package sn.yay360.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminerGrossesseRequest {

    /** Issue de la grossesse ; le statut final (TERMINEE ou INTERROMPUE) en est déduit. */
    @NotBlank(message = "L'issue de la grossesse est obligatoire")
    @Pattern(regexp = sn.yay360.api.model.IssueGrossesse.MOTIF, message = "Issue de grossesse inconnue")
    private String issue;

    /** Date de l'accouchement ou de l'interruption (par défaut : aujourd'hui). */
    @jakarta.validation.constraints.PastOrPresent(message = "La date de fin ne peut pas être dans le futur")
    private java.time.LocalDate dateFin;
}
