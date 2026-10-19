package sn.yay360.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssistantTourRequest {

    @NotBlank
    @Pattern(regexp = "user|assistant", message = "Le rôle doit être user ou assistant")
    private String role;

    @NotBlank
    @Size(max = 2000, message = "Chaque message doit contenir au maximum 2000 caractères")
    private String content;
}
