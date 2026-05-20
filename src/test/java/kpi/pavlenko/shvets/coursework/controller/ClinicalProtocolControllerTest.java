package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.config.SecurityConfig;
import kpi.pavlenko.shvets.coursework.entity.ClinicalProtocol;
import kpi.pavlenko.shvets.coursework.entity.Patient;
import kpi.pavlenko.shvets.coursework.entity.Staff;
import kpi.pavlenko.shvets.coursework.entity.User;
import kpi.pavlenko.shvets.coursework.service.ClinicalProtocolService;
import kpi.pavlenko.shvets.coursework.service.NotificationService;
import kpi.pavlenko.shvets.coursework.service.PatientService;
import kpi.pavlenko.shvets.coursework.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClinicalProtocolController.class)
@Import(SecurityConfig.class)
@WithMockUser
class ClinicalProtocolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private PatientService patientService;
    @MockBean private ClinicalProtocolService protocolService;
    @MockBean private StaffService staffService;
    @MockBean private NotificationService notificationService;

    private Patient testPatient;
    private Staff testStaff;
    private ClinicalProtocol testProtocol;
    private User testUser;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder().id(1L).firstName("John").lastName("Doe").build();
        testUser = User.builder().id(10L).login("doctor").build();
        testStaff = Staff.builder().id(2L).firstName("Dr.").lastName("Smith").user(testUser).build();
        testProtocol = ClinicalProtocol.builder()
                .id(1L)
                .patient(testPatient)
                .staff(testStaff)
                .startDate(LocalDate.now())
                .result("Initial result")
                .build();

        given(notificationService.count(any())).willReturn(0L);
    }

    // --- listProtocolsForPatient ---
    @Test
    void listProtocolsForPatient_shouldReturnProtocolsListView() throws Exception {
        List<ClinicalProtocol> protocols = Collections.singletonList(testProtocol);

        given(patientService.findById(anyLong())).willReturn(testPatient);
        given(protocolService.findByPatient(anyLong())).willReturn(protocols);

        mockMvc.perform(get("/old-medical/patient/{patientId}", testPatient.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("protocols/list"))
                .andExpect(model().attributeExists("patient", "protocols", "pageTitle"))
                .andExpect(model().attribute("patient", testPatient))
                .andExpect(model().attribute("protocols", protocols))
                .andExpect(model().attribute("pageTitle", "Протоколи: John Doe"));
    }

    @Test
    void listProtocolsForPatient_shouldHandlePatientNotFound() {
        given(patientService.findById(anyLong())).willThrow(new RuntimeException("Patient not found."));

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/old-medical/patient/{patientId}", 99L));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- newProtocolForm ---
    @Test
    void newProtocolForm_shouldReturnNewProtocolFormView() throws Exception {
        given(patientService.findById(anyLong())).willReturn(testPatient);

        // ВИПРАВЛЕНО: Додаємо "patient" через flashAttr, щоб Thymeleaf не падав
        mockMvc.perform(get("/old-medical/patient/{patientId}/new-protocol", testPatient.getId())
                        .flashAttr("patient", testPatient))
                .andExpect(status().isOk())
                .andExpect(view().name("protocols/form"))
                .andExpect(model().attributeExists("protocol", "pageTitle"))
                .andExpect(model().attribute("protocol", hasProperty("patient", equalTo(testPatient))))
                .andExpect(model().attribute("pageTitle", "Новий протокол для: John Doe"));
    }

    @Test
    void newProtocolForm_shouldHandlePatientNotFound() {
        given(patientService.findById(anyLong())).willThrow(new RuntimeException("Patient not found."));

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/old-medical/patient/{patientId}/new-protocol", 99L));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- createProtocol ---
    @Test
    @WithMockUser(username = "doctor")
    void createProtocol_shouldCreateProtocolAndRedirect() throws Exception {
        ClinicalProtocol newProtocol = new ClinicalProtocol();
        newProtocol.setResult("Some result");

        given(patientService.findById(anyLong())).willReturn(testPatient);
        given(staffService.findByUsername(anyString())).willReturn(testStaff);
        given(protocolService.save(any(ClinicalProtocol.class))).willReturn(testProtocol);

        mockMvc.perform(post("/old-medical/patient/{patientId}/new-protocol", testPatient.getId()).with(csrf())
                        .flashAttr("protocol", newProtocol))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/old-medical/protocol/" + testProtocol.getId()))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "doctor")
    void createProtocol_shouldHandleServiceException() {
        ClinicalProtocol newProtocol = new ClinicalProtocol();
        newProtocol.setResult("Some result");

        given(patientService.findById(anyLong())).willReturn(testPatient);
        given(staffService.findByUsername(anyString())).willReturn(testStaff);
        given(protocolService.save(any(ClinicalProtocol.class))).willThrow(new RuntimeException("Save failed."));

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/old-medical/patient/{patientId}/new-protocol", testPatient.getId()).with(csrf())
                    .flashAttr("protocol", newProtocol));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- viewProtocol ---
    @Test
    void viewProtocol_shouldReturnProtocolView() throws Exception {
        given(protocolService.findById(anyLong())).willReturn(testProtocol);

        mockMvc.perform(get("/old-medical/protocol/{protocolId}", testProtocol.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("protocols/view"))
                .andExpect(model().attributeExists("protocol", "pageTitle"))
                .andExpect(model().attribute("protocol", testProtocol))
                .andExpect(model().attribute("pageTitle", "Протокол №" + testProtocol.getId() + " для John Doe"));
    }

    @Test
    void viewProtocol_shouldHandleProtocolNotFound() {
        given(protocolService.findById(anyLong())).willThrow(new RuntimeException("Protocol not found."));

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/old-medical/protocol/{protocolId}", 99L));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }
}