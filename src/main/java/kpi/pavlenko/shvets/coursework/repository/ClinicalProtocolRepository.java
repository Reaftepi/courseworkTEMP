package kpi.pavlenko.shvets.coursework.repository;

import kpi.pavlenko.shvets.coursework.entity.ClinicalProtocol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClinicalProtocolRepository extends JpaRepository<ClinicalProtocol, Long> {
    List<ClinicalProtocol> findByPatientId(Long patientId);
    Optional<ClinicalProtocol> findFirstByPatientIdOrderByStartDateDesc(Long patientId);
}
