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
public class NotificationDto {

    private Long id;
    private String type;
    private String titre;
    private String message;
    private Boolean lue;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateLecture;
}
