package kpi.pavlenko.shvets.coursework.repository;

import kpi.pavlenko.shvets.coursework.entity.ProtocolDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProtocolDocumentRepository extends JpaRepository<ProtocolDocument, Long> {
    List<ProtocolDocument> findByProtocolId(Long protocolId);
}
