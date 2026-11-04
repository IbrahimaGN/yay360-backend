package sn.yay360.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminerGrossesseRequest {

    @NotBlank(message = "Le statut final est obligatoire")
    @Pattern(regexp = "TERMINEE|INTERROMPUE", message = "Statut invalide")
    private String statut;

    /** Date de l'accouchement ou de l'interruption (par défaut : aujourd'hui). */
    @jakarta.validation.constraints.PastOrPresent(message = "La date de fin ne peut pas être dans le futur")
    private java.time.LocalDate dateFin;
}
