package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.Patient;
import kpi.pavlenko.shvets.coursework.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private Patient testPatient;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    // --- getAllPatients ---
    @Test
    void getAllPatients_shouldReturnAllPatients() {
        // Given
        List<Patient> patients = Arrays.asList(testPatient, Patient.builder().id(2L).build());
        when(patientRepository.findAll()).thenReturn(patients);

        // When
        List<Patient> result = patientService.getAllPatients();

        // Then
        assertThat(result).isEqualTo(patients);
        verify(patientRepository, times(1)).findAll();
    }

    // --- findById ---
    @Test
    void findById_shouldReturnPatient_whenExists() {
        // Given
        when(patientRepository.findById(testPatient.getId())).thenReturn(Optional.of(testPatient));

        // When
        Patient result = patientService.findById(testPatient.getId());

        // Then
        assertThat(result).isEqualTo(testPatient);
        verify(patientRepository, times(1)).findById(testPatient.getId());
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        // Given
        when(patientRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> patientService.findById(99L), "Patient not found");
        verify(patientRepository, times(1)).findById(99L);
    }

    // --- search ---
    @Test
    void search_shouldReturnAllPatients_whenQueryIsNull() {
        // Given
        List<Patient> patients = Collections.singletonList(testPatient);
        when(patientRepository.findAll()).thenReturn(patients);

        // When
        List<Patient> result = patientService.search(null);

        // Then
        assertThat(result).isEqualTo(patients);
        verify(patientRepository, times(1)).findAll();
        verify(patientRepository, never()).search(anyString());
    }

    @Test
    void search_shouldReturnAllPatients_whenQueryIsBlank() {
        // Given
        List<Patient> patients = Collections.singletonList(testPatient);
        when(patientRepository.findAll()).thenReturn(patients);

        // When
        List<Patient> result = patientService.search("   ");

        // Then
        assertThat(result).isEqualTo(patients);
        verify(patientRepository, times(1)).findAll();
        verify(patientRepository, never()).search(anyString());
    }

    @Test
    void search_shouldReturnFilteredPatients_whenQueryProvided() {
        // Given
        String query = "John";
        List<Patient> patients = Collections.singletonList(testPatient);
        when(patientRepository.search(query)).thenReturn(patients);

        // When
        List<Patient> result = patientService.search(query);

        // Then
        assertThat(result).isEqualTo(patients);
        verify(patientRepository, times(1)).search(query);
        verify(patientRepository, never()).findAll();
    }

    // --- addPatient ---
    @Test
    void addPatient_shouldSavePatient() {
        // Given
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);

        // When
        Patient result = patientService.addPatient(testPatient);

        // Then
        assertThat(result).isEqualTo(testPatient);
        verify(patientRepository, times(1)).save(testPatient);
    }

    // --- removePatientById ---
    @Test
    void removePatientById_shouldCallRepositoryDelete() {
        // Given
        doNothing().when(patientRepository).deleteById(testPatient.getId());

        // When
        patientService.removePatientById(testPatient.getId());

        // Then
        verify(patientRepository, times(1)).deleteById(testPatient.getId());
    }
}
