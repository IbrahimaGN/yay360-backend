package sn.yay360.api.service;

import sn.yay360.api.dto.NotificationDto;

import java.util.List;

public interface NotificationService {

    List<NotificationDto> mesNotifications(String email);

    List<NotificationDto> mesNotificationsNonLues(String email);

    long compterNonLues(String email);

    NotificationDto marquerLue(String email, Long id);

    void marquerToutesLues(String email);
}
