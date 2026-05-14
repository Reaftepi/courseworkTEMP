package kpi.pavlenko.shvets.coursework.observer;

import kpi.pavlenko.shvets.coursework.entity.EventType;
import kpi.pavlenko.shvets.coursework.entity.Notifications;
import kpi.pavlenko.shvets.coursework.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScheduleChangeObserver implements NotificationObserver {
    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void update(AppointmentEvent event) {
        if (event.getType() != EventType.SCHEDULE_CHANGED &&
            event.getType() != EventType.APPOINTMENT_CANCELLED &&
            event.getType() != EventType.APPOINTMENT_CREATED) {
            return;
        }
        var apt = event.getApt();
        String msg;
        if (event.getType() == EventType.APPOINTMENT_CREATED) {
            msg = String.format("New Appointment: You have a new appointment with %s %s on %s at %s.",
                    apt.getPatient().getFirstName(),
                    apt.getPatient().getLastName(),
                    apt.getStartTime().toLocalDate(),
                    apt.getStartTime().toLocalTime());
        } else {
            msg = String.format("Schedule Change: %s %s's appointment on %s at %s has been %s",
                    apt.getPatient().getFirstName(),
                    apt.getPatient().getLastName(),
                    apt.getStartTime().toLocalDate(),
                    apt.getStartTime().toLocalTime(),
                    event.getType() == EventType.SCHEDULE_CHANGED ? "rescheduled" : "cancelled");
        }

        Notifications notification = Notifications.builder()
                .userId(apt.getStaff().getUser().getId())
                .message(msg)
                .type("SCHEDULE_CHANGE")
                .build();
        notificationRepository.save(notification);
    }
}
