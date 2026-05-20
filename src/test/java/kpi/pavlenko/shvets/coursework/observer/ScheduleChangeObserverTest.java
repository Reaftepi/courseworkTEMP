package kpi.pavlenko.shvets.coursework.observer;

import kpi.pavlenko.shvets.coursework.entity.*;
import kpi.pavlenko.shvets.coursework.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleChangeObserverTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private ScheduleChangeObserver scheduleChangeObserver;

    private Appointment testAppointment;
    private User testUser;
    private Patient testPatient;
    private Staff testStaff;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(10L).login("doctor").role(Role.DOCTOR).build();
        testPatient = Patient.builder().id(1L).firstName("John").lastName("Doe").build();
        testStaff = Staff.builder().id(2L).firstName("Dr.").lastName("Smith").user(testUser).build();

        testAppointment = Appointment.builder()
                .id(1L)
                .patient(testPatient)
                .staff(testStaff)
                .startTime(LocalDateTime.of(2024, 1, 1, 10, 0))
                .duration(30)
                .status(AppStatus.SCHEDULED)
                .build();
    }

    @Test
    void update_shouldCreateNotification_whenAppointmentCreatedEvent() {
        // Given
        AppointmentEvent event = AppointmentEvent.created(testAppointment);
        when(notificationRepository.save(any(Notifications.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        scheduleChangeObserver.update(event);

        // Then
        ArgumentCaptor<Notifications> notificationCaptor = ArgumentCaptor.forClass(Notifications.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notifications capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getUserId()).isEqualTo(testUser.getId());
        assertThat(capturedNotification.getType()).isEqualTo("SCHEDULE_CHANGE");
        assertThat(capturedNotification.getMessage()).contains("New Appointment: You have a new appointment with John Doe on 2024-01-01 at 10:00.");
    }

    @Test
    void update_shouldCreateNotification_whenScheduleChangedEvent() {
        // Given
        AppointmentEvent event = AppointmentEvent.scheduleChanged(testAppointment);
        when(notificationRepository.save(any(Notifications.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        scheduleChangeObserver.update(event);

        // Then
        ArgumentCaptor<Notifications> notificationCaptor = ArgumentCaptor.forClass(Notifications.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notifications capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getUserId()).isEqualTo(testUser.getId());
        assertThat(capturedNotification.getType()).isEqualTo("SCHEDULE_CHANGE");
        assertThat(capturedNotification.getMessage()).contains("Schedule Change: John Doe's appointment on 2024-01-01 at 10:00 has been rescheduled");
    }

    @Test
    void update_shouldCreateNotification_whenAppointmentCancelledEvent() {
        // Given
        AppointmentEvent event = AppointmentEvent.cancelled(testAppointment);
        when(notificationRepository.save(any(Notifications.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        scheduleChangeObserver.update(event);

        // Then
        ArgumentCaptor<Notifications> notificationCaptor = ArgumentCaptor.forClass(Notifications.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notifications capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getUserId()).isEqualTo(testUser.getId());
        assertThat(capturedNotification.getType()).isEqualTo("SCHEDULE_CHANGE");
        assertThat(capturedNotification.getMessage()).contains("Schedule Change: John Doe's appointment on 2024-01-01 at 10:00 has been cancelled");
    }

    @Test
    void update_shouldDoNothing_whenOtherEventType() {
        // Given
        AppointmentEvent event = AppointmentEvent.invoiceUnpaid(testAppointment, Invoices.builder().build()); // Інший тип події

        // When
        scheduleChangeObserver.update(event);

        // Then
        verifyNoInteractions(notificationRepository); // Перевіряємо, що репозиторій не викликався
    }
}
