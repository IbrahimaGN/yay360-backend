package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RendezVousDto {

    private Long id;
    private Long grossesseId;
    private Long femmeEnceinteId;
    private String femmeEnceinteNomComplet;
    private Long professionnelId;
    private String professionnelNomComplet;
    private Long etablissementId;
    private String etablissementNom;
    private LocalDateTime dateHeure;
    private String type;
    private String statut;
    private String motif;
    private String notesAnnulation;
    private LocalDateTime dateCreation;
}
