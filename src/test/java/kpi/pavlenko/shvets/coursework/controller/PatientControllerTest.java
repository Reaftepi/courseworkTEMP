package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.config.SecurityConfig;
import kpi.pavlenko.shvets.coursework.entity.Patient;
import kpi.pavlenko.shvets.coursework.service.NotificationService;
import kpi.pavlenko.shvets.coursework.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
@Import(SecurityConfig.class)
@WithMockUser
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientService patientService;

    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        given(notificationService.count(any())).willReturn(0L);
    }

    // --- Тести для GET /patients ---
    @Test
    void list_shouldReturnPatientList_whenNoQuery() throws Exception {
        Patient patient1 = Patient.builder().id(1L).firstName("John").lastName("Doe").build();
        Patient patient2 = Patient.builder().id(2L).firstName("Jane").lastName("Smith").build();
        given(patientService.getAllPatients()).willReturn(Arrays.asList(patient1, patient2));

        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/list"))
                .andExpect(model().attributeExists("patients"))
                .andExpect(model().attribute("patients", Arrays.asList(patient1, patient2)))
                .andExpect(model().attribute("query", (String) null))
                .andExpect(model().attribute("pageTitle", "Пацієнти"));
    }

    @Test
    void list_shouldReturnFilteredPatientList_whenQueryProvided() throws Exception {
        String query = "John";
        Patient patient1 = Patient.builder().id(1L).firstName("John").lastName("Doe").build();
        given(patientService.search(query)).willReturn(Collections.singletonList(patient1));

        mockMvc.perform(get("/patients").param("query", query))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/list"))
                .andExpect(model().attributeExists("patients"))
                .andExpect(model().attribute("patients", Collections.singletonList(patient1)))
                .andExpect(model().attribute("query", query))
                .andExpect(model().attribute("pageTitle", "Пацієнти"));
    }

    // --- Тести для GET /patients/{id} ---
    @Test
    void view_shouldReturnPatientCard_whenPatientExists() throws Exception {
        long patientId = 1L;
        Patient patient = Patient.builder()
                .id(patientId)
                .firstName("John")
                .lastName("Doe")
                .build();

        given(patientService.findById(patientId)).willReturn(patient);

        mockMvc.perform(get("/patients/{id}", patientId))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/card"))
                .andExpect(model().attributeExists("patient"))
                .andExpect(model().attribute("patient", patient))
                .andExpect(model().attribute("pageTitle", "Картка: John Doe"));
    }

    @Test
    void view_shouldReturnServerError_whenPatientServiceThrowsException() {
        long patientId = 99L;
        given(patientService.findById(patientId)).willThrow(new RuntimeException("Patient not found."));

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/patients/{id}", patientId));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- Тести для GET /patients/new ---
    @Test
    void newForm_shouldReturnNewPatientForm() throws Exception {
        mockMvc.perform(get("/patients/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/form"))
                .andExpect(model().attributeExists("patient")) // ВИПРАВЛЕНО: Просто перевіряємо, що об'єкт існує в моделі
                .andExpect(model().attribute("pageTitle", "Новий пацієнт"));
    }

    // --- Тести для POST /patients/new ---
    @Test
    void create_shouldCreatePatientAndRedirect_whenValidData() throws Exception {
        Patient newPatient = Patient.builder()
                .firstName("New")
                .lastName("Patient")
                .dateOfArrival(LocalDate.now())
                .build();

        // ВИПРАВЛЕНО: Сервіс повертає пацієнта, а не void
        given(patientService.addPatient(any(Patient.class))).willReturn(newPatient);

        mockMvc.perform(post("/patients/new").with(csrf())
                        .flashAttr("patient", newPatient))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients"))
                .andExpect(flash().attributeExists("success"));

        verify(patientService, times(1)).addPatient(any(Patient.class));
    }

    @Test
    void create_shouldSetDateOfArrival_whenNull() throws Exception {
        Patient newPatient = Patient.builder()
                .firstName("New")
                .lastName("Patient")
                .build();

        // ВИПРАВЛЕНО: Сервіс повертає пацієнта
        given(patientService.addPatient(any(Patient.class))).willAnswer(invocation -> {
            Patient patientArg = invocation.getArgument(0);
            if (patientArg.getDateOfArrival() == null) {
                patientArg.setDateOfArrival(LocalDate.now());
            }
            return patientArg; // Повертаємо об'єкт
        });

        mockMvc.perform(post("/patients/new").with(csrf())
                .flashAttr("patient", newPatient));

        verify(patientService, times(1)).addPatient(argThat(patient -> patient.getDateOfArrival() != null));
    }

    // --- Тести для GET /patients/{id}/edit ---
    @Test
    void editForm_shouldReturnEditForm_whenPatientExists() throws Exception {
        long patientId = 1L;
        Patient patient = Patient.builder().id(patientId).firstName("Edit").lastName("Me").build();
        given(patientService.findById(patientId)).willReturn(patient);

        mockMvc.perform(get("/patients/{id}/edit", patientId))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/form"))
                .andExpect(model().attributeExists("patient"))
                .andExpect(model().attribute("patient", patient))
                .andExpect(model().attribute("pageTitle", "Редагувати: Edit Me"));
    }

    @Test
    void editForm_shouldReturnServerError_whenPatientDoesNotExist() {
        long patientId = 99L;
        given(patientService.findById(patientId)).willThrow(new RuntimeException("Patient not found."));

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/patients/{id}/edit", patientId));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- Тести для POST /patients/{id}/edit ---
    @Test
    void update_shouldUpdatePatientAndRedirect_whenValidData() throws Exception {
        long patientId = 1L;
        Patient existingPatient = Patient.builder().id(patientId).firstName("Old").lastName("Name").build();
        Patient updatedPatient = Patient.builder().id(patientId).firstName("New").lastName("Name").build();

        given(patientService.findById(patientId)).willReturn(existingPatient);

        // ВИПРАВЛЕНО: Сервіс повертає пацієнта
        given(patientService.addPatient(any(Patient.class))).willReturn(updatedPatient);

        mockMvc.perform(post("/patients/{id}/edit", patientId).with(csrf())
                        .flashAttr("patient", updatedPatient))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients/" + patientId))
                .andExpect(flash().attributeExists("success"));

        verify(patientService, times(1)).addPatient(argThat(p ->
                p.getId().equals(patientId) &&
                        p.getFirstName().equals("New") &&
                        p.getLastName().equals("Name")
        ));
    }

    @Test
    void update_shouldReturnServerError_whenPatientServiceThrowsException() {
        long patientId = 1L;
        Patient updatedPatient = Patient.builder().id(patientId).firstName("New").lastName("Name").build();

        given(patientService.findById(patientId)).willThrow(new RuntimeException("Patient not found."));

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/patients/{id}/edit", patientId).with(csrf())
                    .flashAttr("patient", updatedPatient));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- Тести для POST /patients/{id}/delete ---
    @Test
    void delete_shouldDeletePatientAndRedirect() throws Exception {
        long patientId = 1L;
        doNothing().when(patientService).removePatientById(patientId);

        mockMvc.perform(post("/patients/{id}/delete", patientId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients"))
                .andExpect(flash().attributeExists("success"));

        verify(patientService, times(1)).removePatientById(patientId);
    }

    @Test
    void delete_shouldReturnServerError_whenPatientServiceThrowsException() {
        long patientId = 99L;
        doThrow(new RuntimeException("Patient not found.")).when(patientService).removePatientById(patientId);

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/patients/{id}/delete", patientId).with(csrf()));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }
}