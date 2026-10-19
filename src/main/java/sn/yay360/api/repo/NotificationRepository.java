package sn.yay360.api.repo;

import sn.yay360.api.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByDestinataireIdOrderByDateEnvoiDesc(Long destinataireId);

    List<Notification> findByDestinataireIdAndLueFalseOrderByDateEnvoiDesc(Long destinataireId);

    long countByDestinataireIdAndLueFalse(Long destinataireId);
}
