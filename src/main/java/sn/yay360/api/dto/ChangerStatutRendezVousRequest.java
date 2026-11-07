package sn.yay360.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangerStatutRendezVousRequest {

    @NotBlank(message = "Le statut est obligatoire")
    @Pattern(regexp = "CONFIRME|ANNULE|TERMINE|ABSENT", message = "Statut invalide : CONFIRME, ANNULE, TERMINE ou ABSENT")
    private String statut;
}
