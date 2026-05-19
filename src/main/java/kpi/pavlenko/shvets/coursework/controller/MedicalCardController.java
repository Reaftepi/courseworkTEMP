package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.entity.ClinicalProtocol;
import kpi.pavlenko.shvets.coursework.entity.Patient;
import kpi.pavlenko.shvets.coursework.service.ClinicalProtocolService;
import kpi.pavlenko.shvets.coursework.service.PatientService;
import kpi.pavlenko.shvets.coursework.service.StaffService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/medical-card")
public class MedicalCardController {

    private final PatientService patientService;
    private final ClinicalProtocolService protocolService;
    private final StaffService staffService;

    public MedicalCardController(PatientService patientService, ClinicalProtocolService protocolService, StaffService staffService) {
        this.patientService = patientService;
        this.protocolService = protocolService;
        this.staffService = staffService;
    }

    @GetMapping("/patient/{patientId}")
    public String viewMedicalCard(@PathVariable Long patientId, Model model) {
        Patient patient = patientService.findById(patientId);
        model.addAttribute("patient", patient);
        model.addAttribute("protocols", protocolService.findByPatient(patientId));
        model.addAttribute("pageTitle", "Медкартка: " + patient.getFullName());
        return "medical-card/view";
    }

    @GetMapping("/patient/{patientId}/new-protocol")
    public String newProtocolForm(@PathVariable Long patientId, Model model) {
        Patient patient = patientService.findById(patientId);
        ClinicalProtocol protocol = new ClinicalProtocol();
        protocol.setPatient(patient);
        model.addAttribute("protocol", protocol);
        model.addAttribute("pageTitle", "Новий протокол для: " + patient.getFullName());
        return "medical-card/protocol-form";
    }

    @PostMapping("/patient/{patientId}/new-protocol")
    public String createProtocol(@PathVariable Long patientId, @ModelAttribute ClinicalProtocol protocol, @AuthenticationPrincipal UserDetails currentUser, RedirectAttributes flash) {
        protocol.setPatient(patientService.findById(patientId));
        protocol.setStaff(staffService.findByUsername(currentUser.getUsername()));
        protocol.setStartDate(LocalDate.now());
        ClinicalProtocol savedProtocol = protocolService.save(protocol);
        flash.addFlashAttribute("successMessage", "Протокол успішно створено.");
        return "redirect:/medical-card/protocol/" + savedProtocol.getId();
    }

    @GetMapping("/protocol/{protocolId}")
    public String viewProtocol(@PathVariable Long protocolId, Model model) {
        ClinicalProtocol protocol = protocolService.findById(protocolId);
        model.addAttribute("protocol", protocol);
        model.addAttribute("pageTitle", "Протокол №" + protocol.getId());
        return "medical-card/protocol-view";
    }

    @GetMapping("/protocol/{protocolId}/edit")
    public String editProtocolForm(@PathVariable Long protocolId, Model model) {
        ClinicalProtocol protocol = protocolService.findById(protocolId);
        model.addAttribute("protocol", protocol);
        model.addAttribute("pageTitle", "Редагування протоколу №" + protocol.getId());
        return "medical-card/protocol-form";
    }

    @PostMapping("/protocol/{protocolId}/edit")
    public String updateProtocol(@PathVariable Long protocolId, @ModelAttribute ClinicalProtocol protocolFromForm, RedirectAttributes flash) {
        ClinicalProtocol existingProtocol = protocolService.findById(protocolId);
        existingProtocol.setResult(protocolFromForm.getResult());
        protocolService.save(existingProtocol);
        flash.addFlashAttribute("successMessage", "Протокол успішно оновлено.");
        return "redirect:/medical-card/protocol/" + existingProtocol.getId();
    }
}