package kpi.pavlenko.shvets.coursework.observer;

import kpi.pavlenko.shvets.coursework.entity.Appointment;
import kpi.pavlenko.shvets.coursework.entity.EventType;
import kpi.pavlenko.shvets.coursework.entity.Invoices;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AppointmentEvent {
    private final Appointment apt;
    private final Invoices inv;
    private final EventType type;
    private final LocalDateTime timestamp;

    public static AppointmentEvent created(Appointment apt) {
        return new AppointmentEvent(apt, null, EventType.APPOINTMENT_CREATED, LocalDateTime.now());
    }

    public static AppointmentEvent scheduleChanged(Appointment apt) {
        return new AppointmentEvent(apt, null, EventType.SCHEDULE_CHANGED, LocalDateTime.now());
    }

    public static AppointmentEvent cancelled(Appointment apt) {
        return new AppointmentEvent(apt, null, EventType.APPOINTMENT_CANCELLED, LocalDateTime.now());
    }

    public static AppointmentEvent invoiceUnpaid(Appointment apt, Invoices inv) {
        return new AppointmentEvent(apt, inv, EventType.INVOICE_CREATED, LocalDateTime.now());
    }
}
