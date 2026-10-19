package sn.yay360.api.mapper;

import sn.yay360.api.dto.NotificationDto;
import sn.yay360.api.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDto versDto(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getTitre(),
                notification.getMessage(),
                notification.getLue(),
                notification.getDateEnvoi(),
                notification.getDateLecture()
        );
    }
}
