package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.*;
import kpi.pavlenko.shvets.coursework.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class MedicalCardService {
    @Autowired private ClinicalProtocolRepository protocolRepository;
    @Autowired private SessionNotesRepository notesRepository;
    @Autowired private DiagnosisRepository diagnosisRepository;
    @Autowired private TherapyRepository therapyRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DiagnosisProtocolRepository diagnosisProtocolRepository;
    @Autowired private TherapyProtocolRepository therapyProtocolRepository;

    @Transactional(readOnly = true)
    public List<ClinicalProtocol> getProtocolsForPatient(Long patientId) {
        return protocolRepository.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public ClinicalProtocol getOneProtocolForPatient(Long patientId){
        return protocolRepository.findFirstByPatientIdOrderByStartDateDesc(patientId).orElseThrow(() -> new RuntimeException("No protocol found for patient."));
    }

    @Transactional(readOnly = true)
    public ClinicalProtocol getProtocol(Long id){
        return protocolRepository.findById(id).orElseThrow(() -> new RuntimeException("Protocol not found."));
    }

    @Transactional(readOnly = true)
    public Patient getPatientById(Long patientId) {
        return patientRepository.findById(patientId).orElseThrow(() -> new RuntimeException("Patient not found."));
    }

    public ClinicalProtocol createProtocol(Long patientId, Long staffId) {
        var patient = patientRepository.findById(patientId).orElseThrow(() -> new RuntimeException("Patient not found."));
        var staff = staffRepository.findById(staffId).orElseThrow(() -> new RuntimeException("Staff not found."));
        ClinicalProtocol protocol = ClinicalProtocol.builder()
                .patient(patient)
                .staff(staff)
                .startDate(LocalDate.now())
                .result("")
                .build();
        return protocolRepository.save(protocol);
    }

    public ClinicalProtocol saveProtocol(ClinicalProtocol protocol) {
        return protocolRepository.save(protocol);
    }

    public ClinicalProtocol updateProtocol(Long protocolId, String result){
        ClinicalProtocol protocol = getProtocol(protocolId);
        protocol.setResult(result);
        return protocolRepository.save(protocol);
    }

    public SessionNotes addNote(Long appointmentId, Long staffId, String content){
        SessionNotes note = SessionNotes.builder()
                .appointment(Appointment.builder().id(appointmentId).build())
                .staffId(staffId)
                .content(content)
                .build();
        return notesRepository.save(note);
    }

    @Transactional(readOnly = true)
    public List<SessionNotes> getNotesForAppointment(Long appointmentId){
        return notesRepository.findByAppointmentId(appointmentId);
    }

    @Transactional(readOnly = true)
    public List<Diagnosis> getAllDiagnoses(){
        return diagnosisRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Therapy> getAllTherapies(){
        return therapyRepository.findAll();
    }

    public void addDiagnosis(Long protocolId, Long diagnosisId, boolean isMain) {
        ClinicalProtocol protocol = protocolRepository.findById(protocolId)
                .orElseThrow(() -> new RuntimeException("Протокол не знайдено"));
        Diagnosis diagnosis = diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new RuntimeException("Діагноз не знайдено"));

        DiagnosisProtocol dp = new DiagnosisProtocol();
        dp.setClinicalProtocol(protocol);
        dp.setDiagnosis(diagnosis);
        dp.setMain(isMain);
        dp.setSetDate(LocalDate.now());
        diagnosisProtocolRepository.save(dp);
    }

    public void removeDiagnosis(Long id) {
        diagnosisProtocolRepository.deleteById(id);
    }

    public void addTherapy(Long protocolId, Long therapyId,
                           String dosage, String frequency, String instructions) {
        ClinicalProtocol protocol = protocolRepository.findById(protocolId)
                .orElseThrow(() -> new RuntimeException("Протокол не знайдено"));
        Therapy therapy = therapyRepository.findById(therapyId)
                .orElseThrow(() -> new RuntimeException("Терапію не знайдено"));

        TherapyProtocol tp = new TherapyProtocol();
        tp.setClinicalProtocol(protocol);
        tp.setTherapy(therapy);
        tp.setDosage(dosage);
        tp.setFrequency(frequency);
        tp.setInstructions(instructions);
        therapyProtocolRepository.save(tp);
    }

    public void removeTherapy(Long id) {
        therapyProtocolRepository.deleteById(id);
    }

    public List<DiagnosisProtocol> getDiagnosesForProtocol(Long protocolId) {
        return diagnosisProtocolRepository.findByClinicalProtocol_Id(protocolId);
    }

    public List<TherapyProtocol> getTherapiesForProtocol(Long protocolId) {
        return therapyProtocolRepository.findByClinicalProtocol_Id(protocolId);
    }

}
