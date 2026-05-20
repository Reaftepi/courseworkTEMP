package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.*;
import kpi.pavlenko.shvets.coursework.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class MedicalCardServiceTest {

    @Mock private ClinicalProtocolRepository protocolRepository;
    @Mock private SessionNotesRepository notesRepository;
    @Mock private DiagnosisRepository diagnosisRepository;
    @Mock private TherapyRepository therapyRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DiagnosisProtocolRepository diagnosisProtocolRepository;
    @Mock private TherapyProtocolRepository therapyProtocolRepository;

    @InjectMocks
    private MedicalCardService medicalCardService;

    private Patient testPatient;
    private Staff testStaff;
    private ClinicalProtocol testProtocol;
    private Diagnosis testDiagnosis;
    private Therapy testTherapy;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder().id(1L).firstName("John").lastName("Doe").build();
        testStaff = Staff.builder().id(2L).firstName("Dr.").lastName("Smith").build();
        testProtocol = ClinicalProtocol.builder()
                .id(1L)
                .patient(testPatient)
                .staff(testStaff)
                .startDate(LocalDate.now())
                .result("Initial result")
                .build();
        testDiagnosis = Diagnosis.builder().id(1L).name("Flu").build();
        testTherapy = Therapy.builder().id(1L).name("Antivirals").build();
    }

    // --- getProtocolsForPatient ---
    @Test
    void getProtocolsForPatient_shouldReturnProtocols() {
        // Given
        List<ClinicalProtocol> protocols = Collections.singletonList(testProtocol);
        when(protocolRepository.findByPatientId(testPatient.getId())).thenReturn(protocols);

        // When
        List<ClinicalProtocol> result = medicalCardService.getProtocolsForPatient(testPatient.getId());

        // Then
        assertThat(result).isEqualTo(protocols);
        verify(protocolRepository, times(1)).findByPatientId(testPatient.getId());
    }

    // --- getOneProtocolForPatient ---
    @Test
    void getOneProtocolForPatient_shouldReturnProtocol_whenExists() {
        // Given
        when(protocolRepository.findFirstByPatientIdOrderByStartDateDesc(testPatient.getId())).thenReturn(Optional.of(testProtocol));

        // When
        ClinicalProtocol result = medicalCardService.getOneProtocolForPatient(testPatient.getId());

        // Then
        assertThat(result).isEqualTo(testProtocol);
        verify(protocolRepository, times(1)).findFirstByPatientIdOrderByStartDateDesc(testPatient.getId());
    }

    @Test
    void getOneProtocolForPatient_shouldThrowException_whenNotFound() {
        // Given
        when(protocolRepository.findFirstByPatientIdOrderByStartDateDesc(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> medicalCardService.getOneProtocolForPatient(99L), "No protocol found for patient.");
    }

    // --- getProtocol ---
    @Test
    void getProtocol_shouldReturnProtocol_whenExists() {
        // Given
        when(protocolRepository.findById(testProtocol.getId())).thenReturn(Optional.of(testProtocol));

        // When
        ClinicalProtocol result = medicalCardService.getProtocol(testProtocol.getId());

        // Then
        assertThat(result).isEqualTo(testProtocol);
        verify(protocolRepository, times(1)).findById(testProtocol.getId());
    }

    @Test
    void getProtocol_shouldThrowException_whenNotFound() {
        // Given
        when(protocolRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> medicalCardService.getProtocol(99L), "Protocol not found.");
    }

    // --- getPatientById ---
    @Test
    void getPatientById_shouldReturnPatient_whenExists() {
        // Given
        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));

        // When
        Patient result = medicalCardService.getPatientById(testPatient.getId());

        // Then
        assertThat(result).isEqualTo(testPatient);
        verify(patientRepository, times(1)).findById(testPatient.getId());
    }

    @Test
    void getPatientById_shouldThrowException_whenNotFound() {
        // Given
        when(patientRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> medicalCardService.getPatientById(99L), "Patient not found.");
    }

    // --- createProtocol ---
    @Test
    void createProtocol_shouldCreateAndSaveProtocol() {
        // Given
        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));
        when(staffRepository.findById(testStaff.getId())).thenReturn(Optional.of(testStaff));
        when(protocolRepository.save(any(ClinicalProtocol.class))).thenAnswer(inv -> {
            ClinicalProtocol cp = inv.getArgument(0);
            cp.setId(1L);
            return cp;
        });

        // When
        ClinicalProtocol result = medicalCardService.createProtocol(testPatient.getId(), testStaff.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPatient()).isEqualTo(testPatient);
        assertThat(result.getStaff()).isEqualTo(testStaff);
        assertThat(result.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(result.getResult()).isEqualTo("");
        verify(patientRepository, times(1)).findById(testPatient.getId());
        verify(staffRepository, times(1)).findById(testStaff.getId());
        verify(protocolRepository, times(1)).save(any(ClinicalProtocol.class));
    }

    @Test
    void createProtocol_shouldThrowException_whenPatientNotFound() {
        // Given
        when(patientRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> medicalCardService.createProtocol(99L, testStaff.getId()), "Patient not found.");
    }

    @Test
    void createProtocol_shouldThrowException_whenStaffNotFound() {
        // Given
        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));
        when(staffRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> medicalCardService.createProtocol(testPatient.getId(), 99L), "Staff not found.");
    }

    // --- saveProtocol ---
    @Test
    void saveProtocol_shouldSaveProtocol() {
        // Given
        when(protocolRepository.save(any(ClinicalProtocol.class))).thenReturn(testProtocol);

        // When
        ClinicalProtocol result = medicalCardService.saveProtocol(testProtocol);

        // Then
        assertThat(result).isEqualTo(testProtocol);
        verify(protocolRepository, times(1)).save(testProtocol);
    }

    // --- updateProtocol ---
    @Test
    void updateProtocol_shouldUpdateResultAndSave() {
        // Given
        String newResult = "Updated result";
        when(protocolRepository.findById(testProtocol.getId())).thenReturn(Optional.of(testProtocol));
        when(protocolRepository.save(any(ClinicalProtocol.class))).thenReturn(testProtocol);

        // When
        ClinicalProtocol result = medicalCardService.updateProtocol(testProtocol.getId(), newResult);

        // Then
        assertThat(result.getResult()).isEqualTo(newResult);
        verify(protocolRepository, times(1)).findById(testProtocol.getId());
        verify(protocolRepository, times(1)).save(testProtocol);
    }

    @Test
    void updateProtocol_shouldThrowException_whenProtocolNotFound() {
        // Given
        when(protocolRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> medicalCardService.updateProtocol(99L, "Result"), "Protocol not found.");
    }

    // --- addNote ---
    @Test
    void addNote_shouldCreateAndSaveSessionNote() {
        // Given
        Long appointmentId = 10L;
        Long staffId = testStaff.getId();
        String content = "Test note content";
        SessionNotes expectedNote = SessionNotes.builder()
                .appointment(Appointment.builder().id(appointmentId).build())
                .staffId(staffId)
                .content(content)
                .build();
        when(notesRepository.save(any(SessionNotes.class))).thenAnswer(inv -> {
            SessionNotes sn = inv.getArgument(0);
            sn.setId(1L);
            return sn;
        });

        // When
        SessionNotes result = medicalCardService.addNote(appointmentId, staffId, content);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAppointment().getId()).isEqualTo(appointmentId);
        assertThat(result.getStaffId()).isEqualTo(staffId);
        assertThat(result.getContent()).isEqualTo(content);
        verify(notesRepository, times(1)).save(any(SessionNotes.class));
    }

    // --- getNotesForAppointment ---
    @Test
    void getNotesForAppointment_shouldReturnNotes() {
        // Given
        Long appointmentId = 10L;
        SessionNotes note = SessionNotes.builder().id(1L).appointment(Appointment.builder().id(appointmentId).build()).build();
        List<SessionNotes> notes = Collections.singletonList(note);
        when(notesRepository.findByAppointmentId(appointmentId)).thenReturn(notes);

        // When
        List<SessionNotes> result = medicalCardService.getNotesForAppointment(appointmentId);

        // Then
        assertThat(result).isEqualTo(notes);
        verify(notesRepository, times(1)).findByAppointmentId(appointmentId);
    }

    // --- getAllDiagnoses ---
    @Test
    void getAllDiagnoses_shouldReturnAllDiagnoses() {
        // Given
        List<Diagnosis> diagnoses = Arrays.asList(testDiagnosis, Diagnosis.builder().id(2L).name("Cold").build());
        when(diagnosisRepository.findAll()).thenReturn(diagnoses);

        // When
        List<Diagnosis> result = medicalCardService.getAllDiagnoses();

        // Then
        assertThat(result).isEqualTo(diagnoses);
        verify(diagnosisRepository, times(1)).findAll();
    }

    // --- getAllTherapies ---
    @Test
    void getAllTherapies_shouldReturnAllTherapies() {
        // Given
        List<Therapy> therapies = Arrays.asList(testTherapy, Therapy.builder().id(2L).name("Painkillers").build());
        when(therapyRepository.findAll()).thenReturn(therapies);

        // When
        List<Therapy> result = medicalCardService.getAllTherapies();

        // Then
        assertThat(result).isEqualTo(therapies);
        verify(therapyRepository, times(1)).findAll();
    }

    // --- addDiagnosis ---
    @Test
    void addDiagnosis_shouldAddDiagnosisToProtocol() {
        // Given
        when(protocolRepository.findById(testProtocol.getId())).thenReturn(Optional.of(testProtocol));
        when(diagnosisRepository.findById(testDiagnosis.getId())).thenReturn(Optional.of(testDiagnosis));
        when(diagnosisProtocolRepository.save(any(DiagnosisProtocol.class))).thenAnswer(inv -> {
            DiagnosisProtocol dp = inv.getArgument(0);
            dp.setId(1L);
            return dp;
        });

        // When
        medicalCardService.addDiagnosis(testProtocol.getId(), testDiagnosis.getId(), true);

        // Then
        verify(protocolRepository, times(1)).findById(testProtocol.getId());
        verify(diagnosisRepository, times(1)).findById(testDiagnosis.getId());
        verify(diagnosisProtocolRepository, times(1)).save(any(DiagnosisProtocol.class));
    }

    @Test
    void addDiagnosis_shouldThrowException_whenProtocolNotFound() {
        // Given
        when(protocolRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> medicalCardService.addDiagnosis(99L, testDiagnosis.getId(), true), "Протокол не знайдено");
    }

    @Test
    void addDiagnosis_shouldThrowException_whenDiagnosisNotFound() {
        // Given
        when(protocolRepository.findById(testProtocol.getId())).thenReturn(Optional.of(testProtocol));
        when(diagnosisRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> medicalCardService.addDiagnosis(testProtocol.getId(), 99L, true), "Діагноз не знайдено");
    }

    // --- removeDiagnosis ---
    @Test
    void removeDiagnosis_shouldDeleteDiagnosisProtocol() {
        // Given
        Long dpId = 1L;
        doNothing().when(diagnosisProtocolRepository).deleteById(dpId);

        // When
        medicalCardService.removeDiagnosis(dpId);

        // Then
        verify(diagnosisProtocolRepository, times(1)).deleteById(dpId);
    }

    // --- addTherapy ---
    @Test
    void addTherapy_shouldAddTherapyToProtocol() {
        // Given
        String dosage = "1 tablet";
        String frequency = "daily";
        String instructions = "After food";
        when(protocolRepository.findById(testProtocol.getId())).thenReturn(Optional.of(testProtocol));
        when(therapyRepository.findById(testTherapy.getId())).thenReturn(Optional.of(testTherapy));
        when(therapyProtocolRepository.save(any(TherapyProtocol.class))).thenAnswer(inv -> {
            TherapyProtocol tp = inv.getArgument(0);
            tp.setId(1L);
            return tp;
        });

        // When
        medicalCardService.addTherapy(testProtocol.getId(), testTherapy.getId(), dosage, frequency, instructions);

        // Then
        verify(protocolRepository, times(1)).findById(testProtocol.getId());
        verify(therapyRepository, times(1)).findById(testTherapy.getId());
        verify(therapyProtocolRepository, times(1)).save(any(TherapyProtocol.class));
    }

    @Test
    void addTherapy_shouldThrowException_whenProtocolNotFound() {
        // Given
        when(protocolRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> medicalCardService.addTherapy(99L, testTherapy.getId(), "d", "f", "i"), "Протокол не знайдено");
    }

    @Test
    void addTherapy_shouldThrowException_whenTherapyNotFound() {
        // Given
        when(protocolRepository.findById(testProtocol.getId())).thenReturn(Optional.of(testProtocol));
        when(therapyRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> medicalCardService.addTherapy(testProtocol.getId(), 99L, "d", "f", "i"), "Терапію не знайдено");
    }

    // --- removeTherapy ---
    @Test
    void removeTherapy_shouldDeleteTherapyProtocol() {
        // Given
        Long tpId = 1L;
        doNothing().when(therapyProtocolRepository).deleteById(tpId);

        // When
        medicalCardService.removeTherapy(tpId);

        // Then
        verify(therapyProtocolRepository, times(1)).deleteById(tpId);
    }

    // --- getDiagnosesForProtocol ---
    @Test
    void getDiagnosesForProtocol_shouldReturnDiagnoses() {
        // Given
        DiagnosisProtocol dp = DiagnosisProtocol.builder().id(1L).clinicalProtocol(testProtocol).diagnosis(testDiagnosis).build();
        List<DiagnosisProtocol> diagnoses = Collections.singletonList(dp);
        when(diagnosisProtocolRepository.findByClinicalProtocol_Id(testProtocol.getId())).thenReturn(diagnoses);

        // When
        List<DiagnosisProtocol> result = medicalCardService.getDiagnosesForProtocol(testProtocol.getId());

        // Then
        assertThat(result).isEqualTo(diagnoses);
        verify(diagnosisProtocolRepository, times(1)).findByClinicalProtocol_Id(testProtocol.getId());
    }

    // --- getTherapiesForProtocol ---
    @Test
    void getTherapiesForProtocol_shouldReturnTherapies() {
        // Given
        TherapyProtocol tp = TherapyProtocol.builder().id(1L).clinicalProtocol(testProtocol).therapy(testTherapy).build();
        List<TherapyProtocol> therapies = Collections.singletonList(tp);
        when(therapyProtocolRepository.findByClinicalProtocol_Id(testProtocol.getId())).thenReturn(therapies);

        // When
        List<TherapyProtocol> result = medicalCardService.getTherapiesForProtocol(testProtocol.getId());

        // Then
        assertThat(result).isEqualTo(therapies);
        verify(therapyProtocolRepository, times(1)).findByClinicalProtocol_Id(testProtocol.getId());
    }
}
