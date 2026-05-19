package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.ClinicalProtocol;
import kpi.pavlenko.shvets.coursework.repository.ClinicalProtocolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClinicalProtocolService {

    private final ClinicalProtocolRepository protocolRepository;

    public ClinicalProtocolService(ClinicalProtocolRepository protocolRepository) {
        this.protocolRepository = protocolRepository;
    }

    @Transactional(readOnly = true)
    public List<ClinicalProtocol> findByPatient(Long patientId) {
        return protocolRepository.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public ClinicalProtocol findById(Long protocolId) {
        return protocolRepository.findById(protocolId)
                .orElseThrow(() -> new RuntimeException("Protocol not found with id: " + protocolId));
    }

    public ClinicalProtocol save(ClinicalProtocol protocol) {
        return protocolRepository.save(protocol);
    }
}