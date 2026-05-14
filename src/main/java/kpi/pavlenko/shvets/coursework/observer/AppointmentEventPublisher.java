package kpi.pavlenko.shvets.coursework.observer;

import kpi.pavlenko.shvets.coursework.entity.Appointment;
import kpi.pavlenko.shvets.coursework.entity.Invoices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Component
public class AppointmentEventPublisher implements NotificationSubject {
    private final List<NotificationObserver> observers = new ArrayList<>();

    @Autowired
    public AppointmentEventPublisher(List<NotificationObserver> observers) {
        this.observers.addAll(observers);
        System.out.println("Initialized AppointmentEventPublisher with " + observers.size() + " observers.");
    }

    @Override
    public void subscribe(NotificationObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unsubscribe(NotificationObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(AppointmentEvent event) {
        for (NotificationObserver observer : observers) {
            observer.update(event);
        }
    }

    public void onAppointmentCreated(Appointment apt) {
        notifyObservers(AppointmentEvent.created(apt));
    }

    public void onScheduleChanged(Appointment apt){
        notifyObservers(AppointmentEvent.scheduleChanged(apt));
    }

    public void onAppointmentCancelled(Appointment apt){
        notifyObservers(AppointmentEvent.cancelled(apt));
    }

    public void onInvoiceUnpaid(Appointment apt, Invoices inv){
        notifyObservers(AppointmentEvent.invoiceUnpaid(apt, inv));
    }
}
