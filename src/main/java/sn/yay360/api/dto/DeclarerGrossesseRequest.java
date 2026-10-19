package sn.yay360.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DeclarerGrossesseRequest {

    @NotNull(message = "La date des dernières règles est obligatoire")
    @PastOrPresent(message = "La date des dernières règles ne peut pas être dans le futur")
    private LocalDate dateDernieresRegles;

    @Min(value = 0, message = "Le nombre de grossesses précédentes ne peut pas être négatif")
    @Max(value = 20, message = "Le nombre de grossesses précédentes semble invalide")
    private Integer nombreGrossessesPrecedentes;

    private String antecedents;
}
