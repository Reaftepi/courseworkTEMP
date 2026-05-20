package kpi.pavlenko.shvets.coursework.observer;

import kpi.pavlenko.shvets.coursework.entity.*;
import kpi.pavlenko.shvets.coursework.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnpaidInvoiceObserverTest {

    @Mock
    private NotificationRepository notificationRepository; // Хоча зараз не використовується, але може бути в майбутньому

    @InjectMocks
    private UnpaidInvoiceObserver unpaidInvoiceObserver;

    private Appointment testAppointment;
    private Invoices testInvoice;
    private ByteArrayOutputStream outputStreamCaptor;

    @BeforeEach
    void setUp() {
        testAppointment = Appointment.builder()
                .id(1L)
                .patient(Patient.builder().firstName("John").lastName("Doe").build())
                .staff(Staff.builder().firstName("Dr.").lastName("Smith").build())
                .startTime(LocalDateTime.of(2024, 1, 1, 10, 0))
                .duration(30)
                .status(AppStatus.SCHEDULED)
                .build();

        testInvoice = Invoices.builder()
                .id(1L)
                .appointment(testAppointment)
                .totalAmount(new BigDecimal("100.00"))
                .isPaid(false)
                .build();

        // Перехоплюємо System.out.println
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    void update_shouldPrintMessage_whenInvoiceCreatedEvent() {
        // Given
        AppointmentEvent event = AppointmentEvent.invoiceUnpaid(testAppointment, testInvoice);

        // When
        unpaidInvoiceObserver.update(event);

        // Then
        String expectedMessage = String.format("Unpaid Invoice: John Doe's invoice for appointment on %s at %s (100.00) is unpaid",
                testAppointment.getStartTime().toLocalDate(),
                testAppointment.getStartTime().toLocalTime());
        assertThat(outputStreamCaptor.toString().trim()).isEqualTo(expectedMessage);
        verifyNoInteractions(notificationRepository); // Перевіряємо, що репозиторій не викликався
    }

    @Test
    void update_shouldDoNothing_whenOtherEventType() {
        // Given
        AppointmentEvent event = AppointmentEvent.created(testAppointment); // Інший тип події

        // When
        unpaidInvoiceObserver.update(event);

        // Then
        assertThat(outputStreamCaptor.toString().trim()).isEmpty(); // Нічого не надруковано
        verifyNoInteractions(notificationRepository);
    }
}
