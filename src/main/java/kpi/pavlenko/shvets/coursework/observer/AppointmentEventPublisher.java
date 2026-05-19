package kpi.pavlenko.shvets.coursework.observer;

import kpi.pavlenko.shvets.coursework.entity.Appointment;
import kpi.pavlenko.shvets.coursework.entity.Invoices;
import kpi.pavlenko.shvets.coursework.entity.Role;
import kpi.pavlenko.shvets.coursework.entity.User;
import kpi.pavlenko.shvets.coursework.repository.UserRepository;
import kpi.pavlenko.shvets.coursework.service.NotificationService;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AppointmentEventPublisher {

    private final NotificationService notificationService;
    private final UserRepository userRepository;


    public AppointmentEventPublisher(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    public void onAppointmentCreated(Appointment appointment) {
        String patientMessage = String.format("Ви успішно записані на прийом до лікаря %s на %s.",
                appointment.getStaff().getFullName(), appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        // notificationService.createNotification(appointment.getPatient().getPhoneNumber(), patientMessage); // Пацієнти не мають User-акаунтів

        String staffMessage = String.format("Новий запис на прийом: пацієнт %s на %s.",
                appointment.getPatient().getFullName(), appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        notificationService.createNotification(appointment.getStaff().getUser().getLogin(), staffMessage);
    }

    public void onAppointmentCancelled(Appointment appointment) {
        String patientMessage = String.format("Ваш прийом до лікаря %s на %s було скасовано.",
                appointment.getStaff().getFullName(), appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        // notificationService.createNotification(appointment.getPatient().getPhoneNumber(), patientMessage); // Пацієнти не мають User-акаунтів

        String staffMessage = String.format("Прийом пацієнта %s на %s було скасовано.",
                appointment.getPatient().getFullName(), appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        notificationService.createNotification(appointment.getStaff().getUser().getLogin(), staffMessage);
    }

    public void onScheduleChanged(Appointment appointment) {
        // Логіка для сповіщення про зміну розкладу (якщо буде реалізовано)
    }

    public void onAppointmentCompleted(Appointment appointment) {
        // Сповіщення для пацієнта (закоментовано, бо пацієнти не мають акаунтів)
        // String patientMessage = String.format("Ваш прийом до лікаря %s на %s завершено.",
        //         appointment.getStaff().getFullName(), appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        // notificationService.createNotification(appointment.getPatient().getPhoneNumber(), patientMessage);

        // Сповіщення для лікаря
        String staffMessage = String.format("Прийом пацієнта %s, запланований на %s, завершено.",
                appointment.getPatient().getFullName(), appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        notificationService.createNotification(appointment.getStaff().getUser().getLogin(), staffMessage);
    }

    public void onInvoiceUnpaid(Appointment appointment, Invoices invoice) {
        String message = String.format("Новий неоплачений рахунок #%d для пацієнта %s за прийом #%d на суму %.2f грн.",
                invoice.getId(), appointment.getPatient().getFullName(), appointment.getId(), invoice.getTotalAmount());

        // Знаходимо всіх користувачів з роллю ADMIN
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            notificationService.createNotification(admin.getLogin(), message);
        }
    }
}
