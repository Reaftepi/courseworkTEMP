package kpi.pavlenko.shvets.coursework.observer;

import kpi.pavlenko.shvets.coursework.entity.*;
import kpi.pavlenko.shvets.coursework.repository.UserRepository;
import kpi.pavlenko.shvets.coursework.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentEventPublisherTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppointmentEventPublisher appointmentEventPublisher;

    private Appointment testAppointment;
    private Patient testPatient;
    private Staff testStaff;
    private User testDoctorUser;
    private User testAdminUser1;
    private User testAdminUser2;
    private Invoices testInvoice;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder().id(1L).firstName("John").lastName("Doe").build();
        testDoctorUser = User.builder().id(10L).login("doctor1").role(Role.DOCTOR).build();
        testStaff = Staff.builder().id(2L).firstName("Dr.").lastName("Smith").user(testDoctorUser).build();

        testAppointment = Appointment.builder()
                .id(1L)
                .patient(testPatient)
                .staff(testStaff)
                .startTime(LocalDateTime.of(2024, 1, 1, 10, 0))
                .duration(30)
                .status(AppStatus.SCHEDULED)
                .build();

        testInvoice = Invoices.builder()
                .id(1L)
                .appointment(testAppointment)
                .totalAmount(new BigDecimal("150.00"))
                .isPaid(false)
                .build();

        testAdminUser1 = User.builder().id(20L).login("admin1").role(Role.ADMIN).build();
        testAdminUser2 = User.builder().id(21L).login("admin2").role(Role.ADMIN).build();
    }

    @Test
    void onAppointmentCreated_shouldCreateNotificationForStaff() {
        // Given
        String expectedStaffMessage = String.format("Новий запис на прийом: пацієнт John Doe на %s.",
                testAppointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        // When
        appointmentEventPublisher.onAppointmentCreated(testAppointment);

        // Then
        verify(notificationService, times(1)).createNotification(testDoctorUser.getLogin(), expectedStaffMessage);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onAppointmentCancelled_shouldCreateNotificationForStaff() {
        // Given
        String expectedStaffMessage = String.format("Прийом пацієнта John Doe на %s було скасовано.",
                testAppointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        // When
        appointmentEventPublisher.onAppointmentCancelled(testAppointment);

        // Then
        verify(notificationService, times(1)).createNotification(testDoctorUser.getLogin(), expectedStaffMessage);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onScheduleChanged_shouldDoNothing() {
        // When
        appointmentEventPublisher.onScheduleChanged(testAppointment);

        // Then
        verifyNoInteractions(notificationService); // Метод порожній, не повинен викликати сервіс
    }

    @Test
    void onAppointmentCompleted_shouldCreateNotificationForStaff() {
        // Given
        String expectedStaffMessage = String.format("Прийом пацієнта John Doe, запланований на %s, завершено.",
                testAppointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        // When
        appointmentEventPublisher.onAppointmentCompleted(testAppointment);

        // Then
        verify(notificationService, times(1)).createNotification(testDoctorUser.getLogin(), expectedStaffMessage);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onInvoiceUnpaid_shouldCreateNotificationForAllAdmins() {
        // Given
        List<User> adminUsers = Arrays.asList(testAdminUser1, testAdminUser2);
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(adminUsers);

        String expectedAdminMessage = String.format("Новий неоплачений рахунок #%d для пацієнта John Doe за прийом #%d на суму %.2f грн.",
                testInvoice.getId(), testAppointment.getId(), testInvoice.getTotalAmount());

        // When
        appointmentEventPublisher.onInvoiceUnpaid(testAppointment, testInvoice);

        // Then
        verify(userRepository, times(1)).findByRole(Role.ADMIN);
        verify(notificationService, times(1)).createNotification(testAdminUser1.getLogin(), expectedAdminMessage);
        verify(notificationService, times(1)).createNotification(testAdminUser2.getLogin(), expectedAdminMessage);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onInvoiceUnpaid_shouldDoNothing_whenNoAdminsFound() {
        // Given
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(Collections.emptyList());

        // When
        appointmentEventPublisher.onInvoiceUnpaid(testAppointment, testInvoice);

        // Then
        verify(userRepository, times(1)).findByRole(Role.ADMIN);
        verifyNoInteractions(notificationService); // Немає адмінів, не повинно бути сповіщень
    }
}
