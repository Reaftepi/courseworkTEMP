package kpi.pavlenko.shvets.coursework.repository;

import kpi.pavlenko.shvets.coursework.entity.DiagnosisProtocol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosisProtocolRepository extends JpaRepository<DiagnosisProtocol, Long> {
    List<DiagnosisProtocol> findByClinicalProtocol_Id(Long protocolId);
}
