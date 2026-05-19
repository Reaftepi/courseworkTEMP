package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.entity.ClinicalProtocol;
import kpi.pavlenko.shvets.coursework.entity.ProtocolDocument;
import kpi.pavlenko.shvets.coursework.repository.StaffRepository;
import kpi.pavlenko.shvets.coursework.service.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate; // Додано для createProtocol

@Controller
@RequestMapping("/medical")
@PreAuthorize("hasRole('DOCTOR')") // Захист на рівні бекенду
public class MedicalController {

    private final MedicalCardService medicalCardService;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final StaffService staffService;
    private final StaffRepository staffRepository;
    private final DocumentService documentService;

    // Використовуємо Constructor Injection - це краща практика, ніж @Autowired
    public MedicalController(MedicalCardService medicalCardService, PatientService patientService,
                             AppointmentService appointmentService, StaffService staffService,
                             StaffRepository staffRepository, DocumentService documentService) {
        this.medicalCardService = medicalCardService;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.staffService = staffService;
        this.staffRepository = staffRepository;
        this.documentService = documentService;
    }

    @GetMapping("/patient/{patientId}")
    public String patientCard(@PathVariable Long patientId, Model model) {
        model.addAttribute("patient", patientService.findById(patientId));
        model.addAttribute("protocols", medicalCardService.getProtocolsForPatient(patientId));
        model.addAttribute("doctors", staffService.findDoctors()); // Для форми створення нового протоколу
        model.addAttribute("pageTitle", "Медкартка: " + patientService.findById(patientId).getFullName());
        return "medical/card"; // Цей шаблон буде відображати список протоколів для пацієнта
    }

    @PostMapping("/patient/{patientId}/protocol/new")
    public String createProtocol(@PathVariable Long patientId,
                                 @RequestParam Long staffId,
                                 RedirectAttributes flash) {
        medicalCardService.createProtocol(patientId, staffId);
        flash.addFlashAttribute("successMessage", "Протокол успішно створено.");
        return "redirect:/medical/patient/" + patientId; // Повертаємося на сторінку медкарти пацієнта
    }

    @GetMapping("/protocol/{id}")
    public String protocolView(@PathVariable Long id, Model model) {
        ClinicalProtocol protocol = medicalCardService.getProtocol(id);
        // Додаємо пацієнта та всі його протоколи до моделі
        // Це виправить помилку "Property or field 'id' cannot be found on null"
        model.addAttribute("patient", protocol.getPatient());
        model.addAttribute("allPatientProtocols", medicalCardService.getProtocolsForPatient(protocol.getPatient().getId()));

        model.addAttribute("protocol", protocol);
        model.addAttribute("protocolDiagnoses", medicalCardService.getDiagnosesForProtocol(id));
        model.addAttribute("protocolTherapies", medicalCardService.getTherapiesForProtocol(id));
        model.addAttribute("allDiagnoses", medicalCardService.getAllDiagnoses());
        model.addAttribute("allTherapies", medicalCardService.getAllTherapies());
        model.addAttribute("documents", documentService.getForProtocol(id));
        model.addAttribute("pageTitle", "Протокол №" + protocol.getId() + " для " + protocol.getPatient().getFullName());
        return "medical/protocol"; // Цей шаблон буде відображати деталі одного протоколу
    }

    @PostMapping("/protocol/{id}/result")
    public String saveResult(@PathVariable Long id,
                             @RequestParam String result,
                             RedirectAttributes flash) {
        medicalCardService.updateProtocol(id, result);
        flash.addFlashAttribute("successMessage", "Результат протоколу збережено.");
        return "redirect:/medical/protocol/" + id;
    }

    @PostMapping("/protocol/{appointmentId}/note")
    public String addNote(@PathVariable Long appointmentId,
                          @RequestParam String content,
                          @AuthenticationPrincipal UserDetails currentUser,
                          RedirectAttributes flash) {
        // Використовуємо staffService.findByUsername для отримання Staff об'єкта
        var userStaff = staffService.findByUsername(currentUser.getUsername());
        medicalCardService.addNote(appointmentId, userStaff.getId(), content);
        flash.addFlashAttribute("successMessage", "Примітку додано.");
        var apt = appointmentService.findById(appointmentId);
        return "redirect:/medical/patient/" + apt.getPatient().getId();
    }

    @PostMapping("/protocol/{id}/diagnosis")
    public String addDiagnosis(@PathVariable Long id,
                               @RequestParam Long diagnosisId,
                               @RequestParam(defaultValue = "false") boolean isMain,
                               RedirectAttributes flash) {
        medicalCardService.addDiagnosis(id, diagnosisId, isMain);
        flash.addFlashAttribute("successMessage", "Діагноз додано.");
        return "redirect:/medical/protocol/" + id;
    }

    @PostMapping("/protocol/{id}/diagnosis/{dpId}/delete")
    public String removeDiagnosis(@PathVariable Long id,
                                  @PathVariable Long dpId,
                                  RedirectAttributes flash) {
        medicalCardService.removeDiagnosis(dpId);
        flash.addFlashAttribute("successMessage", "Діагноз видалено.");
        return "redirect:/medical/protocol/" + id;
    }

    @PostMapping("/protocol/{id}/therapy")
    public String addTherapy(@PathVariable Long id,
                             @RequestParam Long therapyId,
                             @RequestParam(required = false) String dosage,
                             @RequestParam(required = false) String frequency,
                             @RequestParam(required = false) String instructions,
                             RedirectAttributes flash) {
        medicalCardService.addTherapy(id, therapyId, dosage, frequency, instructions);
        flash.addFlashAttribute("successMessage", "Призначення додано.");
        return "redirect:/medical/protocol/" + id;
    }

    @PostMapping("/protocol/{id}/therapy/{tpId}/delete")
    public String removeTherapy(@PathVariable Long id,
                                @PathVariable Long tpId,
                                RedirectAttributes flash) {
        medicalCardService.removeTherapy(tpId);
        flash.addFlashAttribute("successMessage", "Призначення видалено.");
        return "redirect:/medical/protocol/" + id;
    }

    @PostMapping("/protocol/{id}/documents")
    public String uploadDocument(@PathVariable Long id,
                                 @RequestParam("file") MultipartFile file,
                                 RedirectAttributes flash) {
        try {
            documentService.upload(id, file);
            flash.addFlashAttribute("successMessage", "Файл завантажено: " + file.getOriginalFilename());
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", "Помилка завантаження: " + e.getMessage());
        }
        return "redirect:/medical/protocol/" + id;
    }

    @GetMapping("/protocol/{id}/documents/{docId}")
    @ResponseBody
    public ResponseEntity<Resource> viewDocument(@PathVariable Long id,
                                                 @PathVariable Long docId) {
        try {
            ProtocolDocument doc = documentService.getById(docId);
            Resource resource = documentService.loadAsResource(docId);

            String disposition = "inline; filename=\"" + doc.getOriginalName() + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(MediaType.parseMediaType(doc.getContentType()))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/protocol/{id}/documents/{docId}/delete")
    public String deleteDocument(@PathVariable Long id,
                                 @PathVariable Long docId,
                                 RedirectAttributes flash) {
        try {
            documentService.delete(docId);
            flash.addFlashAttribute("successMessage", "Файл видалено.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", "Помилка видалення: " + e.getMessage());
        }
        return "redirect:/medical/protocol/" + id;
    }
}
