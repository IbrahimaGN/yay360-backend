package sn.yay360.api.service.impl;

import sn.yay360.api.dto.NotificationDto;
import sn.yay360.api.helper.NotificationHelper;
import sn.yay360.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationHelper notificationHelper;

    @Override
    public List<NotificationDto> mesNotifications(String email) {
        return notificationHelper.mesNotifications(email);
    }

    @Override
    public List<NotificationDto> mesNotificationsNonLues(String email) {
        return notificationHelper.mesNotificationsNonLues(email);
    }

    @Override
    public long compterNonLues(String email) {
        return notificationHelper.compterNonLues(email);
    }

    @Override
    public NotificationDto marquerLue(String email, Long id) {
        return notificationHelper.marquerLue(email, id);
    }

    @Override
    public void marquerToutesLues(String email) {
        notificationHelper.marquerToutesLues(email);
    }
}
