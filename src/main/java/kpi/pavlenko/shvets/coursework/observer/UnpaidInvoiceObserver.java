package kpi.pavlenko.shvets.coursework.observer;

import kpi.pavlenko.shvets.coursework.entity.EventType;
import kpi.pavlenko.shvets.coursework.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UnpaidInvoiceObserver implements NotificationObserver {
    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void update(AppointmentEvent event) {
        if (event.getType() != EventType.INVOICE_CREATED) {
            return;
        }
        var apt = event.getApt();
        String msg = String.format("Unpaid Invoice: %s %s's invoice for appointment on %s at %s (%s) is unpaid",
                apt.getPatient().getFirstName(),
                apt.getPatient().getLastName(),
                apt.getStartTime().toLocalDate(),
                apt.getStartTime().toLocalTime(),
                event.getInv().getTotalAmount());

        System.out.println(msg);
    }
}
