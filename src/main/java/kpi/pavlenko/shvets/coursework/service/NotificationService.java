package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.Notifications;
import kpi.pavlenko.shvets.coursework.entity.User;
import kpi.pavlenko.shvets.coursework.repository.NotificationRepository;
import kpi.pavlenko.shvets.coursework.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Notifications> getAllUserNotifications(String login) {
        User user = userRepository.findByLogin(login).orElseThrow(() -> new RuntimeException("User not found."));
        return notificationRepository.findByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public long count(String login){
        User user = userRepository.findByLogin(login).orElse(null);
        if(user!=null){
            return notificationRepository.countByUserIdAndReadFalse(user.getId());
        }
        return 0;
    }

    public void markRead(Long id){
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    public void markReadAll(String login){
        User user = userRepository.findByLogin(login).orElseThrow(() -> new RuntimeException("User not found."));
        List<Notifications> notifications = notificationRepository.findByUserId(user.getId());
        for(Notifications notification : notifications){
            notification.setRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    public Notifications createNotification(String login, String message) {
        User user = userRepository.findByLogin(login).orElseThrow(() -> new RuntimeException("User not found."));

        // Використовуємо конструктор, згенерований Lombok @AllArgsConstructor,
        // виходячи з логу помилки та структури Notifications.java.
        // Сигнатура: Notifications(Long id, Long userId, String message, String type, boolean read, LocalDateTime createdAt)
        Notifications notification = new Notifications(
            null, // id (буде згенеровано базою даних)
            user.getId(), // userId
            message, // message
            "Неоплачений рахунок", // type (як визначено у вашій сутності)
            false, // read (статус прочитання)
            LocalDateTime.now() // createdAt
        );

        return notificationRepository.save(notification);
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}
