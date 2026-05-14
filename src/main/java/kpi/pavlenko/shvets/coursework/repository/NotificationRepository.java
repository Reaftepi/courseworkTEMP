package kpi.pavlenko.shvets.coursework.repository;

import kpi.pavlenko.shvets.coursework.entity.Notifications;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notifications, Long> {
    List<Notifications> findByUserId(Long userId);

    Long countByUserIdAndReadFalse(Long id);
    //List<Notifications> findByLogin(String login);
}
