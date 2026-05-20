package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.*;
import kpi.pavlenko.shvets.coursework.observer.AppointmentEventPublisher;
import kpi.pavlenko.shvets.coursework.repository.AppointmentRepository;
import kpi.pavlenko.shvets.coursework.repository.InvoiceRepository;
import kpi.pavlenko.shvets.coursework.repository.PatientRepository;
import kpi.pavlenko.shvets.coursework.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private AppointmentEventPublisher appointmentEventPublisher;

    @InjectMocks
    private AppointmentService appointmentService;

    private Patient testPatient;
    private Staff testStaff;
    private Appointment testAppointment;
    private LocalDateTime testStartTime;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder().id(1L).firstName("John").lastName("Doe").build();
        testStaff = Staff.builder().id(2L).firstName("Dr.").lastName("Smith").isMedical(true).user(User.builder().id(10L).login("doctor").role(Role.DOCTOR).build()).build();
        testStartTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        testAppointment = Appointment.builder()
                .id(1L)
                .patient(testPatient)
                .staff(testStaff)
                .startTime(testStartTime)
                .duration(30)
                .status(AppStatus.SCHEDULED)
                .build();
    }

    // --- Тести для create ---
    @Test
    void create_shouldSaveAppointmentAndInvoice_whenNoConflict() {
        // Given
        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));
        when(staffRepository.findById(testStaff.getId())).thenReturn(Optional.of(testStaff));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment apt = inv.getArgument(0);
            if (apt.getId() == null) apt.setId(1L); // Імітуємо збереження
            return apt;
        });
        when(invoiceRepository.save(any(Invoices.class))).thenAnswer(inv -> inv.getArgument(0));
        // Мокуємо hasConflict, щоб він повертав false
        when(appointmentRepository.findByStaffAndDay(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        Appointment createdAppointment = appointmentService.create(testPatient.getId(), testStaff.getId(), testStartTime, 30, new BigDecimal("500.00"));

        // Then
        assertThat(createdAppointment).isNotNull();
        assertThat(createdAppointment.getStatus()).isEqualTo(AppStatus.SCHEDULED);
        assertThat(createdAppointment.getPatient().getId()).isEqualTo(testPatient.getId());
        assertThat(createdAppointment.getStaff().getId()).isEqualTo(testStaff.getId());

        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(invoiceRepository, times(1)).save(any(Invoices.class));
        verify(appointmentEventPublisher, times(1)).onAppointmentCreated(any(Appointment.class));
        verify(appointmentEventPublisher, times(1)).onInvoiceUnpaid(any(Appointment.class), any(Invoices.class));
    }

    @Test
    void create_shouldThrowException_whenConflictExists() {
        // Given
        // Налаштовуємо hasConflict, щоб він повертав true
        when(appointmentRepository.findByStaffAndDay(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(testAppointment)); // Імітуємо конфлікт

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> appointmentService.create(testPatient.getId(), testStaff.getId(), testStartTime, 30, BigDecimal.TEN));

        assertThat(exception.getMessage()).isEqualTo("Конфлікт розкладу: обраний час вже зайнято.");
        verify(appointmentRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());
        verify(appointmentEventPublisher, never()).onAppointmentCreated(any());
        verify(appointmentEventPublisher, never()).onInvoiceUnpaid(any(), any());
    }

    @Test
    void create_shouldThrowException_whenPatientNotFound() {
        // Given
        when(patientRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> appointmentService.create(testPatient.getId(), testStaff.getId(), testStartTime, 30, BigDecimal.TEN),
                "Patient not found.");
    }

    @Test
    void create_shouldThrowException_whenStaffNotFound() {
        // Given
        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));
        when(staffRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> appointmentService.create(testPatient.getId(), testStaff.getId(), testStartTime, 30, BigDecimal.TEN),
                "Staff not found.");
    }

    // --- Тести для getAllAppointments ---
    @Test
    void getAllAppointments_shouldReturnAllAppointments() {
        // Given
        List<Appointment> appointments = Arrays.asList(testAppointment, Appointment.builder().id(2L).build());
        when(appointmentRepository.findAll()).thenReturn(appointments);

        // When
        List<Appointment> result = appointmentService.getAllAppointments();

        // Then
        assertThat(result).isEqualTo(appointments);
        verify(appointmentRepository, times(1)).findAll();
    }

    // --- Тести для findById ---
    @Test
    void findById_shouldReturnAppointment_whenExists() {
        // Given
        when(appointmentRepository.findById(testAppointment.getId())).thenReturn(Optional.of(testAppointment));

        // When
        Appointment result = appointmentService.findById(testAppointment.getId());

        // Then
        assertThat(result).isEqualTo(testAppointment);
        verify(appointmentRepository, times(1)).findById(testAppointment.getId());
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        // Given
        when(appointmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> appointmentService.findById(99L), "Appointment not found.");
        verify(appointmentRepository, times(1)).findById(99L);
    }

    // --- Тести для findByPatientId ---
    @Test
    void findByPatientId_shouldReturnAppointments() {
        // Given
        List<Appointment> appointments = Collections.singletonList(testAppointment);
        when(appointmentRepository.findByPatientId(testPatient.getId())).thenReturn(appointments);

        // When
        List<Appointment> result = appointmentService.findByPatientId(testPatient.getId());

        // Then
        assertThat(result).isEqualTo(appointments);
        verify(appointmentRepository, times(1)).findByPatientId(testPatient.getId());
    }

    // --- Тести для getScheduleForDay ---
    @Test
    void getScheduleForDay_shouldReturnAppointmentsForStaffAndDay() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        List<Appointment> appointments = Collections.singletonList(testAppointment);
        when(appointmentRepository.findByStaffAndDay(testStaff.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .thenReturn(appointments);

        // When
        List<Appointment> result = appointmentService.getScheduleForDay(testStaff.getId(), date);

        // Then
        assertThat(result).isEqualTo(appointments);
        verify(appointmentRepository, times(1)).findByStaffAndDay(testStaff.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    // --- Тести для hasConflict ---
    @Test
    void hasConflict_shouldReturnFalse_whenNoConflict() {
        // Given
        when(appointmentRepository.findByStaffAndDay(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        boolean result = appointmentService.hasConflict(testStaff.getId(), testStartTime, 30, null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasConflict_shouldReturnTrue_whenConflictExists() {
        // Given
        Appointment conflictingAppointment = Appointment.builder()
                .id(2L)
                .staff(testStaff)
                .startTime(testStartTime.plusMinutes(15)) // Перетинається
                .duration(30)
                .status(AppStatus.SCHEDULED)
                .build();
        when(appointmentRepository.findByStaffAndDay(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(conflictingAppointment));

        // When
        boolean result = appointmentService.hasConflict(testStaff.getId(), testStartTime, 30, null);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasConflict_shouldReturnFalse_whenConflictWithSelf() {
        // Given
        // Імітуємо, що findByStaffAndDay повертає сам запис, який ми редагуємо
        when(appointmentRepository.findByStaffAndDay(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(testAppointment));

        // When
        boolean result = appointmentService.hasConflict(testStaff.getId(), testStartTime, 30, testAppointment.getId());

        // Then
        assertThat(result).isFalse(); // Не конфліктує сам із собою
    }

    @Test
    void hasConflict_shouldReturnFalse_whenConflictingAppointmentIsCancelled() {
        // Given
        Appointment cancelledAppointment = Appointment.builder()
                .id(2L)
                .staff(testStaff)
                .startTime(testStartTime.plusMinutes(15))
                .duration(30)
                .status(AppStatus.CANCELLED) // Скасований
                .build();
        when(appointmentRepository.findByStaffAndDay(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(cancelledAppointment));

        // When
        boolean result = appointmentService.hasConflict(testStaff.getId(), testStartTime, 30, null);

        // Then
        assertThat(result).isFalse(); // Скасовані не враховуються як конфлікт
    }

    // --- Тести для save ---
    @Test
    void save_shouldUpdateAppointment_whenNoConflict() {
        // Given
        Appointment updatedAppointment = Appointment.builder()
                .id(testAppointment.getId())
                .patient(testPatient)
                .staff(testStaff)
                .startTime(testStartTime.plusHours(1)) // Змінений час
                .duration(45)
                .status(AppStatus.SCHEDULED)
                .build();
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(updatedAppointment);
        // Мокуємо hasConflict, щоб він повертав false
        when(appointmentRepository.findByStaffAndDay(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        Appointment result = appointmentService.save(updatedAppointment);

        // Then
        assertThat(result).isEqualTo(updatedAppointment);
        verify(appointmentRepository, times(1)).save(updatedAppointment);
    }

    @Test
    void save_shouldThrowException_whenConflictExists() {
        // Given
        Appointment updatedAppointment = Appointment.builder()
                .id(testAppointment.getId()) // Ми оновлюємо запис з ID 1
                .patient(testPatient)
                .staff(testStaff)
                .startTime(testStartTime.plusHours(1))
                .duration(45)
                .status(AppStatus.SCHEDULED)
                .build();

        // FIX: Створюємо ІНШИЙ запис, який буде конфліктувати
        Appointment conflictingAppointment = Appointment.builder()
                .id(99L) // Інший ID
                .staff(testStaff)
                .startTime(updatedAppointment.getStartTime()) // Той самий час, що й у оновленого
                .duration(30)
                .status(AppStatus.SCHEDULED)
                .build();

        // Мокуємо hasConflict, щоб він повертав true, надаючи конфліктуючий запис
        when(appointmentRepository.findByStaffAndDay(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(conflictingAppointment));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> appointmentService.save(updatedAppointment));

        assertThat(exception.getMessage()).isEqualTo("Конфлікт розкладу: обраний час вже зайнято.");
        verify(appointmentRepository, never()).save(any());
    }

    // --- Тести для cancel ---
    @Test
    void cancel_shouldSetStatusToCancelledAndPublishEvent() {
        // Given
        when(appointmentRepository.findById(testAppointment.getId())).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        // When
        Appointment result = appointmentService.cancel(testAppointment.getId());

        // Then
        assertThat(result.getStatus()).isEqualTo(AppStatus.CANCELLED);
        verify(appointmentRepository, times(1)).save(testAppointment);
        verify(appointmentEventPublisher, times(1)).onAppointmentCancelled(testAppointment);
    }

    @Test
    void cancel_shouldThrowException_whenAppointmentNotFound() {
        // Given
        when(appointmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> appointmentService.cancel(99L), "Appointment not found.");
    }

    // --- Тести для completed ---
    @Test
    void completed_shouldSetStatusToCompletedAndPublishEvent() {
        // Given
        when(appointmentRepository.findById(testAppointment.getId())).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        // When
        Appointment result = appointmentService.completed(testAppointment.getId());

        // Then
        assertThat(result.getStatus()).isEqualTo(AppStatus.COMPLETED);
        verify(appointmentRepository, times(1)).save(testAppointment);
        verify(appointmentEventPublisher, times(1)).onAppointmentCompleted(testAppointment);
    }

    @Test
    void completed_shouldThrowException_whenAppointmentNotFound() {
        // Given
        when(appointmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> appointmentService.completed(99L), "Appointment not found.");
    }

    // --- Тести для reschedule ---
    @Test
    void reschedule_shouldUpdateStartTimeAndPublishEvent_whenNoConflict() {
        // Given
        LocalDateTime newStartTime = testStartTime.plusDays(2);
        when(appointmentRepository.findById(testAppointment.getId())).thenReturn(Optional.of(testAppointment));
        // Мокуємо hasConflict, щоб він повертав false
        when(appointmentRepository.findByStaffAndDay(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(testAppointment)); // Повертаємо сам запис, щоб hasConflict повернув false

        // When
        appointmentService.reschedule(testAppointment.getId(), newStartTime);

        // Then
        assertThat(testAppointment.getStartTime()).isEqualTo(newStartTime);
        verify(appointmentRepository, times(1)).save(testAppointment);
        verify(appointmentEventPublisher, times(1)).onScheduleChanged(testAppointment);
    }

    @Test
    void reschedule_shouldThrowException_whenAppointmentNotFound() {
        // Given
        when(appointmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> appointmentService.reschedule(99L, LocalDateTime.now()), "Appointment not found.");
    }

    @Test
    void reschedule_shouldThrowException_whenAppointmentIsCancelled() {
        // Given
        testAppointment.setStatus(AppStatus.CANCELLED);
        when(appointmentRepository.findById(testAppointment.getId())).thenReturn(Optional.of(testAppointment));

        // When & Then
        assertThrows(RuntimeException.class, () -> appointmentService.reschedule(testAppointment.getId(), LocalDateTime.now()), "Cannot reschedule a cancelled appointment.");
    }

    @Test
    void reschedule_shouldThrowException_whenConflictExists() {
        // Given
        LocalDateTime newStartTime = testStartTime.plusDays(2);
        Appointment conflictingAppointment = Appointment.builder()
                .id(2L)
                .staff(testStaff)
                .startTime(newStartTime.plusMinutes(15))
                .duration(30)
                .status(AppStatus.SCHEDULED)
                .build();
        when(appointmentRepository.findById(testAppointment.getId())).thenReturn(Optional.of(testAppointment));
        // Мокуємо hasConflict, щоб він повертав true
        when(appointmentRepository.findByStaffAndDay(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testAppointment, conflictingAppointment)); // Повертаємо конфлікт

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> appointmentService.reschedule(testAppointment.getId(), newStartTime));

        assertThat(exception.getMessage()).isEqualTo("Schedule conflict detected.");
        verify(appointmentRepository, never()).save(any());
        verify(appointmentEventPublisher, never()).onScheduleChanged(any());
    }

    // --- Тести для getAllForDay ---
    @Test
    void getAllForDay_shouldReturnAppointmentsForGivenDay() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        List<Appointment> appointments = Collections.singletonList(testAppointment);
        when(appointmentRepository.findAllByDay(date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .thenReturn(appointments);

        // When
        List<Appointment> result = appointmentService.getAllForDay(date);

        // Then
        assertThat(result).isEqualTo(appointments);
        verify(appointmentRepository, times(1)).findAllByDay(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    // --- Тести для findAppointmentsByFilter ---
    @Test
    void findAppointmentsByFilter_shouldReturnScheduleForDay_whenStaffIdProvided() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        List<Appointment> appointments = Collections.singletonList(testAppointment);
        when(appointmentRepository.findByStaffAndDay(testStaff.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .thenReturn(appointments);

        // When
        List<Appointment> result = appointmentService.findAppointmentsByFilter(testStaff.getId(), date);

        // Then
        assertThat(result).isEqualTo(appointments);
        verify(appointmentRepository, times(1)).findByStaffAndDay(testStaff.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        verify(appointmentRepository, never()).findAllByDay(any(), any());
    }

    @Test
    void findAppointmentsByFilter_shouldReturnAllForDay_whenNoStaffIdProvided() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        List<Appointment> appointments = Collections.singletonList(testAppointment);
        when(appointmentRepository.findAllByDay(date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .thenReturn(appointments);

        // When
        List<Appointment> result = appointmentService.findAppointmentsByFilter(null, date);

        // Then
        assertThat(result).isEqualTo(appointments);
        verify(appointmentRepository, times(1)).findAllByDay(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        verify(appointmentRepository, never()).findByStaffAndDay(anyLong(), any(), any());
    }
}
