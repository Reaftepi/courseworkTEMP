package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.config.SecurityConfig;
import kpi.pavlenko.shvets.coursework.entity.*;
import kpi.pavlenko.shvets.coursework.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
// ПРАВИЛЬНІ ІМПОРТИ ДЛЯ TEST WEB SERVLET:
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentController.class)
@Import(SecurityConfig.class)
@WithMockUser
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AppointmentService appointmentService;
    @MockBean private PatientService patientService;
    @MockBean private StaffService staffService;
    @MockBean private NotificationService notificationService;
    @MockBean private InvoiceService invoiceService;

    private Patient testPatient;
    private Staff testStaff;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder().id(1L).firstName("John").lastName("Doe").build();
        testStaff = Staff.builder().id(2L).firstName("Dr.").lastName("Smith").build();
        testAppointment = Appointment.builder()
                .id(1L)
                .patient(testPatient)
                .staff(testStaff)
                .startTime(LocalDateTime.of(2024, 1, 1, 10, 0))
                .duration(30)
                .status(AppStatus.SCHEDULED) // FIX 1: Add status to avoid NPE in template
                .build();

        given(notificationService.count(any())).willReturn(0L);
    }

    @Test
    void listAppointments_shouldReturnCalendarViewForToday_whenNoParams() throws Exception {
        LocalDate today = LocalDate.now();
        List<Appointment> appointments = Collections.singletonList(testAppointment);
        List<Staff> doctors = Collections.singletonList(testStaff);

        given(appointmentService.findAppointmentsByFilter(isNull(), eq(today))).willReturn(appointments);
        given(staffService.findDoctors()).willReturn(doctors);

        mockMvc.perform(get("/appointments"))
                .andExpect(status().isOk())
                .andExpect(view().name("appointments/calendar"));
    }

    @Test
    void listAppointments_shouldReturnCalendarViewForSelectedDateAndStaff() throws Exception {
        LocalDate selectedDate = LocalDate.of(2024, 2, 15);
        List<Appointment> appointments = Collections.singletonList(testAppointment);
        given(appointmentService.findAppointmentsByFilter(testStaff.getId(), selectedDate)).willReturn(appointments);
        given(staffService.findDoctors()).willReturn(Collections.singletonList(testStaff));

        mockMvc.perform(get("/appointments")
                        .param("staffId", String.valueOf(testStaff.getId()))
                        .param("date", selectedDate.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void listAppointments_shouldReturnAllAppointmentsView_whenShowAllIsTrue() throws Exception {
        // FIX 1: Ensure all test objects have the necessary fields to avoid NPE in templates
        Appointment app2 = Appointment.builder()
                .id(2L)
                .patient(testPatient)
                .staff(testStaff)
                .status(AppStatus.SCHEDULED)
                .startTime(LocalDateTime.now())
                .build();
        List<Appointment> allAppointments = Arrays.asList(testAppointment, app2);

        given(appointmentService.getAllAppointments()).willReturn(allAppointments);
        given(staffService.findDoctors()).willReturn(Collections.singletonList(testStaff));

        mockMvc.perform(get("/appointments").param("showAll", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void update_shouldUpdateAndRedirect_whenValidData() throws Exception {
        LocalDateTime newStartTime = LocalDateTime.of(2024, 1, 2, 11, 0);

        given(appointmentService.findById(testAppointment.getId())).willReturn(testAppointment);
        // When save is called, we can just return the same object or a new one
        when(appointmentService.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // FIX 2: Simulate form submission using params, not flashAttr.
        // This correctly populates the @ModelAttribute in the controller using dot notation for nested objects.
        mockMvc.perform(post("/appointments/{id}/edit", testAppointment.getId()).with(csrf())
                        .param("patient.id", String.valueOf(testPatient.getId()))
                        .param("staff.id", String.valueOf(testStaff.getId()))
                        .param("startTime", newStartTime.toString()) // Format "YYYY-MM-DDTHH:mm"
                        .param("duration", "45"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments?date=" + newStartTime.toLocalDate()));
    }
}