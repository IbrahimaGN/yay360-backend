package sn.yay360.api.controller;

import sn.yay360.api.dto.MessageResponse;
import sn.yay360.api.dto.NotificationDto;
import sn.yay360.api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Rappels et informations intelligentes (§5.10)")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lister mes notifications")
    public List<NotificationDto> mesNotifications(Authentication authentication) {
        return notificationService.mesNotifications(authentication.getName());
    }

    @GetMapping("/non-lues")
    @Operation(summary = "Lister mes notifications non lues")
    public List<NotificationDto> mesNotificationsNonLues(Authentication authentication) {
        return notificationService.mesNotificationsNonLues(authentication.getName());
    }

    @GetMapping("/non-lues/nombre")
    @Operation(summary = "Compter mes notifications non lues")
    public Map<String, Long> compterNonLues(Authentication authentication) {
        return Map.of("nombre", notificationService.compterNonLues(authentication.getName()));
    }

    @PutMapping("/{id}/lue")
    @Operation(summary = "Marquer une notification comme lue")
    public NotificationDto marquerLue(Authentication authentication, @PathVariable Long id) {
        return notificationService.marquerLue(authentication.getName(), id);
    }

    @PutMapping("/tout-marquer-lu")
    @Operation(summary = "Marquer toutes mes notifications comme lues")
    public MessageResponse marquerToutesLues(Authentication authentication) {
        notificationService.marquerToutesLues(authentication.getName());
        return new MessageResponse("Toutes les notifications ont été marquées comme lues");
    }
}
