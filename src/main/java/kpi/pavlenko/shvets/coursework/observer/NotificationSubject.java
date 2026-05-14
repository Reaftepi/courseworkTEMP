package kpi.pavlenko.shvets.coursework.observer;

public interface NotificationSubject {
    void subscribe(NotificationObserver observer);
    void unsubscribe(NotificationObserver observer);
    void notifyObservers(AppointmentEvent event);
}
