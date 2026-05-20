package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.entity.Appointment;
import kpi.pavlenko.shvets.coursework.entity.Patient;
import kpi.pavlenko.shvets.coursework.entity.Staff;
import kpi.pavlenko.shvets.coursework.entity.AppStatus;
import kpi.pavlenko.shvets.coursework.service.AppointmentService;
import kpi.pavlenko.shvets.coursework.service.NotificationService;
import kpi.pavlenko.shvets.coursework.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@WithMockUser // Імітуємо аутентифікованого користувача
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientService patientService;
    @MockBean
    private AppointmentService appointmentService;
    @MockBean
    private NotificationService notificationService; // Можливо, використовується в GlobalControllerAdvice

    private Patient testPatient;
    private Appointment testAppointment1;
    private Appointment testAppointment2;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder().id(1L).firstName("John").lastName("Doe").build();
        testAppointment1 = Appointment.builder()
                .id(1L)
                .patient(testPatient)
                .staff(Staff.builder().id(1L).firstName("Dr.").lastName("Smith").build())
                .startTime(LocalDateTime.now().minusDays(1))
                .duration(30)
                .status(AppStatus.SCHEDULED) // FIX: Додано статус
                .build();
        testAppointment2 = Appointment.builder()
                .id(2L)
                .patient(testPatient)
                .staff(Staff.builder().id(1L).firstName("Dr.").lastName("Smith").build())
                .startTime(LocalDateTime.now().plusDays(1))
                .duration(60)
                .status(AppStatus.SCHEDULED) // FIX: Додано статус
                .build();
    }

    @Test
    void dashboard_shouldReturnDashboardViewWithCorrectAttributes() throws Exception {
        // Given
        List<Patient> patients = Collections.singletonList(testPatient);
        List<Appointment> allAppointments = Arrays.asList(testAppointment1, testAppointment2); // Unordered

        // FIX: The test must expect what the controller actually does:
        // It sorts all appointments by date descending and takes the top 5.
        // Since testAppointment2 is in the future, it will be first.
        List<Appointment> expectedRecentAppointments = Arrays.asList(testAppointment2, testAppointment1);

        given(patientService.getAllPatients()).willReturn(patients);
        given(appointmentService.getAllAppointments()).willReturn(allAppointments);
        given(notificationService.count(anyString())).willReturn(5L); // Імітуємо лічильник сповіщень

        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("pageTitle", "totalPatients", "totalAppointments", "recentAppointments"))
                .andExpect(model().attribute("pageTitle", "Дашборд"))
                .andExpect(model().attribute("totalPatients", (long) patients.size()))
                .andExpect(model().attribute("totalAppointments", (long) allAppointments.size()))
                .andExpect(model().attribute("recentAppointments", expectedRecentAppointments));

        verify(patientService, times(1)).getAllPatients();
        verify(appointmentService, times(1)).getAllAppointments(); // FIX: Should be called only once now
        verify(notificationService, times(1)).count(anyString());
    }
}
