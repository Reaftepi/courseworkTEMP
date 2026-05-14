package kpi.pavlenko.shvets.coursework.observer;

import kpi.pavlenko.shvets.coursework.entity.EventType;
import kpi.pavlenko.shvets.coursework.entity.Notifications;
import kpi.pavlenko.shvets.coursework.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppointmentReminderObserver implements NotificationObserver{
    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void update(AppointmentEvent event) {
        if (event.getType() != EventType.APPOINTMENT_CREATED) {
            return;
        }
        var apt = event.getApt();
        String msg = String.format("Reminder: %s %s has an appointment on %s at %s",
                apt.getPatient().getFirstName(),
                apt.getPatient().getLastName(),
                apt.getStartTime().toLocalDate(),
                apt.getStartTime().toLocalTime());

        Notifications forDoctor = Notifications.builder()
                .userId(apt.getStaff().getUser().getId())
                .message(msg)
                .type("APPOINTMENT_REMINDER")
                .build();
        notificationRepository.save(forDoctor);
    }
}
