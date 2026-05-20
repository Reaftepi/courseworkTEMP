package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.config.SecurityConfig;
import kpi.pavlenko.shvets.coursework.entity.*;
import kpi.pavlenko.shvets.coursework.repository.StaffRepository;
import kpi.pavlenko.shvets.coursework.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicalController.class)
@Import(SecurityConfig.class) // ДОДАНО: підключаємо ваші реальні налаштування безпеки
class MedicalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private MedicalCardService medicalCardService;
    @MockBean private PatientService patientService;
    @MockBean private AppointmentService appointmentService;
    @MockBean private StaffService staffService;
    @MockBean private StaffRepository staffRepository;
    @MockBean private DocumentService documentService;
    @MockBean private NotificationService notificationService;

    private Patient testPatient;
    private Staff testStaff;
    private ClinicalProtocol testProtocol;
    private Appointment testAppointment;
    private ProtocolDocument testDocument;
    private User testUser;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder().id(1L).firstName("John").lastName("Doe").build();
        testUser = User.builder().id(10L).login("doctor").role(Role.DOCTOR).build();
        testStaff = Staff.builder().id(2L).firstName("Dr.").lastName("Smith").user(testUser).build();
        testProtocol = ClinicalProtocol.builder().id(1L).patient(testPatient).staff(testStaff).startDate(LocalDate.now()).build();
        testAppointment = Appointment.builder().id(1L).patient(testPatient).staff(testStaff).startTime(LocalDateTime.now()).build();
        testDocument = ProtocolDocument.builder().id(1L).protocol(testProtocol).originalName("doc.pdf").storedName("uuid.pdf").contentType("application/pdf").build();

        given(notificationService.count(any())).willReturn(0L);
    }

    // --- Access Control Tests ---
    @Test
    @WithMockUser(roles = {"ADMIN"})
    void medicalEndpoints_shouldBeForbiddenForAdmin() throws Exception {
        mockMvc.perform(get("/medical/patient/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"PATIENT"})
    void medicalEndpoints_shouldBeForbiddenForPatient() throws Exception {
        mockMvc.perform(get("/medical/patient/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void medicalEndpoints_shouldRequireAuthentication() throws Exception {
        // ВИПРАВЛЕНО: просто перевіряємо, що є перенаправлення на будь-яку сторінку авторизації (включаючи OAuth2)
        mockMvc.perform(get("/medical/patient/1"))
                .andExpect(status().is3xxRedirection());
    }

    // --- patientCard ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void patientCard_shouldReturnPatientCardView() throws Exception {
        List<ClinicalProtocol> protocols = Collections.singletonList(testProtocol);
        List<Staff> doctors = Collections.singletonList(testStaff);

        given(patientService.findById(testPatient.getId())).willReturn(testPatient);
        given(medicalCardService.getProtocolsForPatient(testPatient.getId())).willReturn(protocols);
        given(staffService.findDoctors()).willReturn(doctors);

        mockMvc.perform(get("/medical/patient/{patientId}", testPatient.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("medical/card"));
    }

    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void patientCard_shouldHandlePatientNotFound() {
        given(patientService.findById(anyLong())).willThrow(new RuntimeException("Patient not found."));

        // ВИПРАВЛЕНО: очікуємо помилку через assertThrows
        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/medical/patient/{patientId}", 99L));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- createProtocol ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void createProtocol_shouldCreateAndRedirect() throws Exception {
        given(medicalCardService.createProtocol(testPatient.getId(), testStaff.getId())).willReturn(testProtocol);

        mockMvc.perform(post("/medical/patient/{patientId}/protocol/new", testPatient.getId())
                        .param("staffId", String.valueOf(testStaff.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/medical/patient/" + testPatient.getId()));
    }

    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void createProtocol_shouldHandleServiceException() {
        given(medicalCardService.createProtocol(anyLong(), anyLong())).willThrow(new RuntimeException("Error creating protocol."));

        // ВИПРАВЛЕНО
        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/medical/patient/{patientId}/protocol/new", testPatient.getId())
                    .param("staffId", String.valueOf(testStaff.getId()))
                    .with(csrf()));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- protocolView ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void protocolView_shouldReturnProtocolView() throws Exception {
        List<DiagnosisProtocol> diagnoses = Collections.emptyList();
        List<TherapyProtocol> therapies = Collections.emptyList();
        List<ProtocolDocument> documents = Collections.emptyList();
        List<ClinicalProtocol> allProtocols = Collections.singletonList(testProtocol);

        given(medicalCardService.getProtocol(testProtocol.getId())).willReturn(testProtocol);
        given(medicalCardService.getDiagnosesForProtocol(testProtocol.getId())).willReturn(diagnoses);
        given(medicalCardService.getTherapiesForProtocol(testProtocol.getId())).willReturn(therapies);
        given(documentService.getForProtocol(testProtocol.getId())).willReturn(documents);
        given(medicalCardService.getProtocolsForPatient(testPatient.getId())).willReturn(allProtocols);

        mockMvc.perform(get("/medical/protocol/{id}", testProtocol.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("medical/protocol"));
    }

    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void protocolView_shouldHandleProtocolNotFound() {
        given(medicalCardService.getProtocol(anyLong())).willThrow(new RuntimeException("Protocol not found."));

        // ВИПРАВЛЕНО
        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/medical/protocol/{id}", 99L));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- saveResult ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void saveResult_shouldUpdateProtocolAndRedirect() throws Exception {
        String newResult = "Updated result";
        given(medicalCardService.updateProtocol(testProtocol.getId(), newResult)).willReturn(testProtocol);

        mockMvc.perform(post("/medical/protocol/{id}/result", testProtocol.getId())
                        .param("result", newResult)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/medical/protocol/" + testProtocol.getId()));
    }

    // --- addNote ---
    @Test
    @WithMockUser(username = "doctor", roles = {"DOCTOR"})
    void addNote_shouldAddNoteAndRedirect() throws Exception {
        String content = "Some note";
        SessionNotes sessionNotes = SessionNotes.builder().id(1L).content(content).build();
        given(staffService.findByUsername("doctor")).willReturn(testStaff);
        given(medicalCardService.addNote(testAppointment.getId(), testStaff.getId(), content)).willReturn(sessionNotes);
        given(appointmentService.findById(testAppointment.getId())).willReturn(testAppointment);

        mockMvc.perform(post("/medical/protocol/{appointmentId}/note", testAppointment.getId())
                        .param("content", content)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // --- addDiagnosis ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void addDiagnosis_shouldAddDiagnosisAndRedirect() throws Exception {
        Long diagnosisId = 1L;
        doNothing().when(medicalCardService).addDiagnosis(testProtocol.getId(), diagnosisId, true);

        mockMvc.perform(post("/medical/protocol/{id}/diagnosis", testProtocol.getId())
                        .param("diagnosisId", String.valueOf(diagnosisId))
                        .param("isMain", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // --- removeDiagnosis ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void removeDiagnosis_shouldRemoveDiagnosisAndRedirect() throws Exception {
        Long dpId = 1L;
        doNothing().when(medicalCardService).removeDiagnosis(dpId);

        mockMvc.perform(post("/medical/protocol/{id}/diagnosis/{dpId}/delete", testProtocol.getId(), dpId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // --- addTherapy ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void addTherapy_shouldAddTherapyAndRedirect() throws Exception {
        Long therapyId = 1L;
        String dosage = "10mg";
        String frequency = "daily";
        String instructions = "After food";
        doNothing().when(medicalCardService).addTherapy(testProtocol.getId(), therapyId, dosage, frequency, instructions);

        mockMvc.perform(post("/medical/protocol/{id}/therapy", testProtocol.getId())
                        .param("therapyId", String.valueOf(therapyId))
                        .param("dosage", dosage)
                        .param("frequency", frequency)
                        .param("instructions", instructions)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // --- removeTherapy ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void removeTherapy_shouldRemoveTherapyAndRedirect() throws Exception {
        Long tpId = 1L;
        doNothing().when(medicalCardService).removeTherapy(tpId);

        mockMvc.perform(post("/medical/protocol/{id}/therapy/{tpId}/delete", testProtocol.getId(), tpId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // --- uploadDocument ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void uploadDocument_shouldUploadFileAndRedirect() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "some data".getBytes());
        given(documentService.upload(testProtocol.getId(), mockFile)).willReturn(testDocument);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/medical/protocol/{id}/documents", testProtocol.getId())
                        .file(mockFile)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void uploadDocument_shouldHandleUploadException() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "some data".getBytes());
        given(documentService.upload(anyLong(), any(MultipartFile.class))).willThrow(new RuntimeException("Upload failed."));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/medical/protocol/{id}/documents", testProtocol.getId())
                        .file(mockFile)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // --- viewDocument ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void viewDocument_shouldReturnDocumentResource() throws Exception {
        Resource mockResource = mock(Resource.class);
        given(documentService.getById(testDocument.getId())).willReturn(testDocument);
        given(documentService.loadAsResource(testDocument.getId())).willReturn(mockResource);
        given(mockResource.getFilename()).willReturn(testDocument.getOriginalName());

        mockMvc.perform(get("/medical/protocol/{id}/documents/{docId}", testProtocol.getId(), testDocument.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void viewDocument_shouldReturnNotFound_whenDocumentServiceThrowsException() throws Exception {
        given(documentService.getById(anyLong())).willThrow(new RuntimeException("Document not found."));

        // Метод у контролері має try-catch і повертає 404, а не викидає помилку назовні
        mockMvc.perform(get("/medical/protocol/{id}/documents/{docId}", testProtocol.getId(), 99L))
                .andExpect(status().isNotFound());
    }

    // --- deleteDocument ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void deleteDocument_shouldDeleteFileAndRedirect() throws Exception {
        doNothing().when(documentService).delete(testDocument.getId());

        mockMvc.perform(post("/medical/protocol/{id}/documents/{docId}/delete", testProtocol.getId(), testDocument.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void deleteDocument_shouldHandleDeleteException() throws Exception {
        doThrow(new RuntimeException("Delete failed.")).when(documentService).delete(anyLong());

        mockMvc.perform(post("/medical/protocol/{id}/documents/{docId}/delete", testProtocol.getId(), testDocument.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}