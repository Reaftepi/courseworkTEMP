package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.service.PatientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/medical-records")
public class MedicalRecordController {

    private final PatientService patientService;

    public MedicalRecordController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public String listMedicalRecords(Model model) {
        model.addAttribute("patients", patientService.getAllPatients());
        model.addAttribute("pageTitle", "Медичні картки");
        return "medical-records/list";
    }
}
