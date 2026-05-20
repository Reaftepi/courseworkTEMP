package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.ClinicalProtocol;
import kpi.pavlenko.shvets.coursework.entity.Patient;
import kpi.pavlenko.shvets.coursework.entity.Staff;
import kpi.pavlenko.shvets.coursework.repository.ClinicalProtocolRepository;
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
class ClinicalProtocolServiceTest {

    @Mock
    private ClinicalProtocolRepository protocolRepository;

    @InjectMocks
    private ClinicalProtocolService clinicalProtocolService;

    private ClinicalProtocol testProtocol;
    private Patient testPatient;
    private Staff testStaff;

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
    }

    // --- findByPatient ---
    @Test
    void findByPatient_shouldReturnProtocolsForPatient() {
        // Given
        List<ClinicalProtocol> protocols = Collections.singletonList(testProtocol);
        when(protocolRepository.findByPatientId(testPatient.getId())).thenReturn(protocols);

        // When
        List<ClinicalProtocol> result = clinicalProtocolService.findByPatient(testPatient.getId());

        // Then
        assertThat(result).isEqualTo(protocols);
        verify(protocolRepository, times(1)).findByPatientId(testPatient.getId());
    }

    // --- findById ---
    @Test
    void findById_shouldReturnProtocol_whenExists() {
        // Given
        when(protocolRepository.findById(testProtocol.getId())).thenReturn(Optional.of(testProtocol));

        // When
        ClinicalProtocol result = clinicalProtocolService.findById(testProtocol.getId());

        // Then
        assertThat(result).isEqualTo(testProtocol);
        verify(protocolRepository, times(1)).findById(testProtocol.getId());
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        // Given
        when(protocolRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> clinicalProtocolService.findById(99L), "Protocol not found with id: 99");
        verify(protocolRepository, times(1)).findById(99L);
    }

    // --- save ---
    @Test
    void save_shouldSaveClinicalProtocol() {
        // Given
        when(protocolRepository.save(any(ClinicalProtocol.class))).thenReturn(testProtocol);

        // When
        ClinicalProtocol result = clinicalProtocolService.save(testProtocol);

        // Then
        assertThat(result).isEqualTo(testProtocol);
        verify(protocolRepository, times(1)).save(testProtocol);
    }
}
