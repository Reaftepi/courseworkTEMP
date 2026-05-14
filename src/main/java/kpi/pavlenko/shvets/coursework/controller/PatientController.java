package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.entity.Patient;
import kpi.pavlenko.shvets.coursework.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/patients")
public class PatientController {
    @Autowired private PatientService patientService;

    @GetMapping
    public String list(@RequestParam(required = false) String query, Model model){
        var patients = (query != null && !query.isBlank())
                ? patientService.search(query)
                : patientService.getAllPatients();
        model.addAttribute("patients", patients);
        model.addAttribute("query", query);
        return "patients/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model){
        model.addAttribute("patient", patientService.findById(id));
        return "patients/card";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
    model.addAttribute("patient", new Patient());
    return "patients/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute Patient patient, RedirectAttributes flash) {
        if(patient.getDateOfArrival() == null){
            patient.setDateOfArrival(LocalDate.now());
        }
        patientService.addPatient(patient);
        flash.addFlashAttribute("success", "Patient created successfully");
        return "redirect:/patients";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("patient", patientService.findById(id));
        return "patients/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute Patient patient, RedirectAttributes flash) {
        patient.setId(id);
        patientService.addPatient(patient);
        flash.addFlashAttribute("success", "Patient updated successfully");
        return "redirect:/patients/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        patientService.removePatientById(id);
        flash.addFlashAttribute("success", "Patient deleted successfully");
        return "redirect:/patients";
    }
}
