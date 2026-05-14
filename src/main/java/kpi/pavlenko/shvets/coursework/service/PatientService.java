package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.Patient;
import kpi.pavlenko.shvets.coursework.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientService {
    @Autowired
    private PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Patient findById(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    @Transactional(readOnly = true)
    public List<Patient> search(String query){
        if(query == null || query.isBlank()){
            return getAllPatients();
        }
        return patientRepository.search(query.trim());
    }

    /*@Transactional(readOnly = true)
    public List<Patient> findByName(String name) {
        if (name.isBlank()) {
            return getAllPatients();
        }
        return patientRepository.search(name.trim());

    }
     */
    //func logic must be in PatientRepository

    public Patient addPatient(Patient patient){
        return patientRepository.save(patient);
    }
    public void removePatientById(Long id){
        patientRepository.deleteById(id);
    }
}
