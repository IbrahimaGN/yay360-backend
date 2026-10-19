package sn.yay360.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ModifierRendezVousRequest {

    @NotNull(message = "La nouvelle date et heure sont obligatoires")
    @Future(message = "La date du rendez-vous doit être dans le futur")
    private LocalDateTime dateHeure;

    private String motif;
}
