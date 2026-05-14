package kpi.pavlenko.shvets.coursework.repository;

import kpi.pavlenko.shvets.coursework.entity.TherapyProtocol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TherapyProtocolRepository extends JpaRepository<TherapyProtocol, Long> {
    List<TherapyProtocol> findByClinicalProtocol_Id(Long protocolId);
}
