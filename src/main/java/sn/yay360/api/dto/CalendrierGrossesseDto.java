package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalendrierGrossesseDto {

    private Long grossesseId;
    private Integer semaineActuelle;
    private LocalDate datePrevueAccouchement;
    private List<CalendrierSemaineDto> semaines;
}
